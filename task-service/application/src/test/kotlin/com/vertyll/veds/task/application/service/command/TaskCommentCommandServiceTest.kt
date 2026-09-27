@file:OptIn(ExperimentalUuidApi::class)

package com.vertyll.veds.task.application.service.command

import com.vertyll.veds.sharederror.ApiException
import com.vertyll.veds.task.application.InMemoryCommentRepository
import com.vertyll.veds.task.application.InMemoryProjectDirectory
import com.vertyll.veds.task.application.InMemoryRolePermissions
import com.vertyll.veds.task.application.InMemoryTaskRepository
import com.vertyll.veds.task.application.InMemoryUserDirectory
import com.vertyll.veds.task.application.RecordingTaskEventPublisher
import com.vertyll.veds.task.application.command.CreateCommentCommand
import com.vertyll.veds.task.application.command.UpdateCommentCommand
import com.vertyll.veds.task.application.dto.Actor
import com.vertyll.veds.task.application.membership
import com.vertyll.veds.task.application.projectRef
import com.vertyll.veds.task.application.service.TaskAuthorizationService
import com.vertyll.veds.task.application.task
import com.vertyll.veds.task.domain.error.TaskError
import com.vertyll.veds.task.domain.model.TaskComment
import org.junit.jupiter.api.Test
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlin.uuid.toJavaUuid

internal class TaskCommentCommandServiceTest {
    private val comments = InMemoryCommentRepository()
    private val tasks = InMemoryTaskRepository()
    private val directory = InMemoryProjectDirectory()
    private val users = InMemoryUserDirectory()
    private val events = RecordingTaskEventPublisher()

    private val roles = InMemoryRolePermissions()

    private val service =
        TaskCommentCommandService(
            commentRepository = comments,
            userDirectory = users,
            authorization = TaskAuthorizationService(directory, tasks, roles),
            eventPublisher = events,
        )

    private val project = projectRef().also { directory.saveProject(it) }
    private val projectId = project.projectId

    private fun actorWithRole(roleCode: String): Actor {
        val id = Uuid.generateV7().toJavaUuid()
        directory.saveMembership(membership(projectId, id, roleCode))
        return Actor(id = id, email = "$roleCode@example.com", firstName = "Test", lastName = "User")
    }

    private val author = actorWithRole("MEMBER")
    private val existingTask = task(projectId, createdBy = author.id).also { tasks.given(it) }

    private fun givenComment(authorId: UUID = author.id) =
        TaskComment(taskId = existingTask.id, authorId = authorId, content = "Working on it", version = 0L)
            .also { comments.given(it) }

    @Test
    fun `a comment is stored against its task and author`() {
        val response = service.addComment(existingTask.id, CreateCommentCommand(LOOKS_GOOD, emptySet()), author)

        val stored = comments.findById(response.id)!!
        assertEquals(existingTask.id, stored.taskId)
        assertEquals(author.id, stored.authorId)
        assertEquals(LOOKS_GOOD, stored.content)
    }

    @Test
    fun `adding a comment announces it`() {
        val response = service.addComment(existingTask.id, CreateCommentCommand(LOOKS_GOOD, emptySet()), author)

        assertEquals(listOf("CommentAdded(${existingTask.id},${response.id})"), events.published)
    }

    @Test
    fun `the author is recorded in the user directory`() {
        service.addComment(existingTask.id, CreateCommentCommand(LOOKS_GOOD, emptySet()), author)

        assertNotNull(users.findById(author.id))
    }

    @Test
    fun `a client may comment`() {
        val client = actorWithRole("CLIENT")

        service.addComment(existingTask.id, CreateCommentCommand("Any update?", emptySet()), client)

        assertEquals(1, comments.stored.size)
    }

    @Test
    fun `someone with no membership cannot comment`() {
        val outsider = Actor(Uuid.generateV7().toJavaUuid(), "out@example.com", null, null)

        assertFailsWith<ApiException> { service.addComment(existingTask.id, CreateCommentCommand("Hello", emptySet()), outsider) }

        assertTrue(comments.stored.isEmpty())
        assertTrue(events.published.isEmpty())
    }

    @Test
    fun `the author may edit their own comment`() {
        val comment = givenComment()

        service.editComment(comment.id, UpdateCommentCommand(REWORDED), author, 0L)

        assertEquals(REWORDED, comments.findById(comment.id)!!.content)
    }

    @Test
    fun `nobody else may edit a comment, not even a manager`() {
        val comment = givenComment()
        val manager = actorWithRole("MANAGER")

        val error = assertFailsWith<ApiException> { service.editComment(comment.id, UpdateCommentCommand(REWORDED), manager, 0L) }

        assertEquals(TaskError.COMMENT_NOT_AUTHORED_BY_CALLER, error.error)
        assertEquals("Working on it", comments.findById(comment.id)!!.content)
    }

    @Test
    fun `an edit against a stale version is refused`() {
        val comment = givenComment()

        val error = assertFailsWith<ApiException> { service.editComment(comment.id, UpdateCommentCommand(REWORDED), author, 9L) }

        assertEquals(TaskError.VERSION_MISMATCH, error.error)
    }

    @Test
    fun `an unknown comment is reported as missing`() {
        val error =
            assertFailsWith<ApiException> { service.editComment(Uuid.generateV7().toJavaUuid(), UpdateCommentCommand("x"), author, 0L) }

        assertEquals(TaskError.COMMENT_NOT_FOUND, error.error)
    }

    @Test
    fun `the author may delete their own comment`() {
        val comment = givenComment()

        service.deleteComment(comment.id, author)

        assertNull(comments.findById(comment.id))
    }

    @Test
    fun `anyone who can manage the board may delete somebody else's comment`() {
        listOf("MANAGER", "MEMBER").forEach { roleCode ->
            comments.stored.clear()
            val comment = givenComment()

            service.deleteComment(comment.id, actorWithRole(roleCode))

            assertNull(comments.findById(comment.id), "$roleCode could not moderate")
        }
    }

    @Test
    fun `a client may not delete somebody else's comment`() {
        val comment = givenComment()
        val client = actorWithRole("CLIENT")

        val error = assertFailsWith<ApiException> { service.deleteComment(comment.id, client) }

        assertEquals(TaskError.COMMENT_NOT_AUTHORED_BY_CALLER, error.error)
        assertNotNull(comments.findById(comment.id))
    }

    @Test
    fun `a client may still delete their own comment`() {
        val client = actorWithRole("CLIENT")
        val comment = givenComment(authorId = client.id)

        service.deleteComment(comment.id, client)

        assertNull(comments.findById(comment.id))
    }

    @Test
    fun `deleting an unknown comment is reported as missing`() {
        val error = assertFailsWith<ApiException> { service.deleteComment(Uuid.generateV7().toJavaUuid(), author) }

        assertEquals(TaskError.COMMENT_NOT_FOUND, error.error)
    }
}

private const val LOOKS_GOOD = "Looks good"
private const val REWORDED = "Reworded"
