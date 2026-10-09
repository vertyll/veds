# Testing

Two tiers, split by what they can actually prove.

| Tier                 | Where                                     | Needs   | Runs on          |
|----------------------|-------------------------------------------|---------|------------------|
| Domain / application | `domain/src/test`, `application/src/test` | nothing | every push       |
| Integration          | `infrastructure/src/test`                 | Docker  | service workflow |

## Domain and application tests

No Spring, no database, no containers — a direct consequence of the framework-free application layer
(see [Hexagonal Layering](hexagonal-layering.md)). They use JUnit 5 plus
`kotlin-test`, deliberately **not** `spring-boot-starter-test`: pulling that in would put Spring on the module's
classpath and the architecture check would fail the build.

What is covered in project-service:

| Test                      | Pins down                                                                     |
|---------------------------|-------------------------------------------------------------------------------|
| `ProjectAccessPolicyTest` | every permission × owner / member / stranger, public and archived projects    |
| `ProjectInvitationTest`   | state transitions — an invitation cannot be accepted twice or after rejection |
| `TranslationTest`         | the completeness invariant that removes the lookup fallback                   |
| `VersionGuardTest`        | optimistic locking, including "no `If-Match` means no check"                  |
| `ProjectTest`             | identity assigned by the domain, name validation, archive/restore             |

Two of these are worth calling out because they protect properties that are easy to regress silently:

- `ProjectAccessPolicyTest` asserts that `permissionsOf` agrees with `permits` for every subject kind. The front end
  renders controls from `permissionsOf`; if the two drift, the UI offers actions the policy then refuses.
- `TranslationTest` asserts that an aggregate *cannot be constructed* with a language missing. That invariant is what
  makes `translationFor` total, so nothing needs a fallback.

## Integration tests

`IntegrationTestBase` starts one PostgreSQL and one Kafka container for the whole module.
It injects both addresses into the `test` profile, so no test resource points at a fixed host. That profile also
takes a fresh consumer group per run, so one run never replays another's offsets, and shortens the saga timeouts
enough that a watchdog test does not wait on the production cooldown.

Collaborators arrive through the test class's constructor, never an `@Autowired` field, which is
what keeps a test honest about how many things it needs. Spring resolves those parameters only
when the module carries `spring.test.constructor.autowire.mode = all` in
`junit-platform.properties` — its default is `ANNOTATED`, and without the file it declines to
resolve them at all, so every test in the module fails before the context is even asked for.

The `test` profile is a full profile, not a patch: a property the service requires has to be set
there as well. It gets an unroutable value — `http://localhost:1` for the authz and translation
clients — so a test that unintentionally calls out fails loudly instead of reaching a real
service. A property left unset surfaces as a bean-creation failure at context start, and only
under `-PintegrationTests`, which is why `./gradlew build` stays green while the service cannot
boot.

The outbox and the saga engine are the two things most worth testing here, and neither can be meaningfully exercised
against mocks: the outbox exists *because* Kafka does not join the database transaction, so a test with both faked
proves nothing about the property it protects.

`ProjectInvitationSagaIntegrationTest` asserts three things that only hold together:

1. **Outbox atomicity** — `project-member-invited` is written in the same transaction as the invitation row, not sent to
   Kafka directly.
2. **Compensation** — a `mail-failed` reply settles the saga, so nobody is left holding a pending invitation they were
   never told about.
3. **Idempotency** — replaying the same feedback event changes nothing, which is required because the outbox delivers at
   least once.

`UserJpaRepositoryIntegrationTest` and `TaskQueryAdapterIntegrationTest` cover the two persistence mistakes a
unit test cannot see, because both are rejected by the database rather than by the compiler:

| Test                               | What only a real database rejects                                                                                                                           |
|------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `UserJpaRepositoryIntegrationTest` | An `@EntityGraph` naming an attribute the entity does not own. Hibernate refuses the query outright, so every lookup fails rather than fetching lazily      |
| `TaskQueryAdapterIntegrationTest`  | A column missing from `user_ref` in the native assignee query. It only runs once a task has an assignee, so a project with an empty task list looks healthy |

The assignee fixture in the second is therefore load-bearing: a task with no assignees exercises none of it.

### Why the problem document is pinned twice

`shared-web` unit-tests how the document is built; `ProblemDetailContractTest` pins that Spring
still serializes it that way once the whole chain — advice, message converter, Jackson mixin — is
in play. That chain is where a problem document quietly turns back into `{ "properties": { … } }`
on an upgrade, and only a request over the real HTTP stack with the container's own `ObjectMapper`
would notice.

Two answers are pinned there because Spring's defaults are wrong for this contract: an
unauthenticated request would carry `WWW-Authenticate` and an empty body, so the entry point fills
the body in rather than making a caller special-case one status; and an unknown path stays a `404`
rather than becoming a `500`, which would report a defect in the service for a request the caller
got wrong.

## The architecture check

`./gradlew checkHexagonalDependencies` fails if a framework appears on the application layer's resolved
`compileClasspath`. It reads the *resolved* classpath rather than declared dependencies, so a framework arriving
transitively is caught too. That is the path worth guarding: importing a single enum from a Spring-bound module puts the
whole framework on the application classpath.

It is wired into `check`, so `./gradlew build` runs it. In CI, it is a separate job of the reusable
`quality-checks.yml` workflow, enabled with `hexagonal: true` — only the `-service` builds register the task.

The rule is enforced three times over, each mechanism catching what the others cannot:

| Mechanism                    | Catches                                                                                                                            |
|------------------------------|------------------------------------------------------------------------------------------------------------------------------------|
| The Gradle module graph      | a framework never reaches the application layer's classpath, so it cannot compile                                                  |
| `checkHexagonalDependencies` | a framework arriving transitively through another dependency                                                                       |
| `shared-archunit`            | what a classpath cannot express — a JPA annotation on a domain model, a controller outside the web adapter, a port that is a class |

Each service applies the shared rules with a single class:

```kotlin
class ProjectArchitectureTest : VedsArchitectureTest("com.vertyll.veds.project")
```

See [shared-archunit](../shared-archunit/README.md) for the full list and the two deliberate exemptions.

Since `everyEndpointDeclaresItsAuthorization`, that check also covers authorization: every method
Spring maps to an HTTP request must carry `@PreAuthorize`, `@AuthorizedInUseCase`, `@ScopedToCaller`
or `@PublicEndpoint`. The rule cannot tell whether a decision is *right* — it makes the absence of
one impossible to miss, which is the failure that hides best.

## Coverage by service

| Service                | Domain tests                                                       |
|------------------------|--------------------------------------------------------------------|
| `project-service`      | access policy, invitations, translations, version guard, aggregate |
| `task-service`         | access policy, task aggregate, comment authorship                  |
| `iam-service`          | role permissions, permissions derived from roles                   |
| `notification-service` | delivery settings, notification lifecycle                          |
| `translation-service`  | default/override columns, language tags, key ownership             |
| `file-service`         | upload lifecycle, scope limits                                     |
| `mail-service`         | none — its logic is delivery, which mocks cannot prove             |

Several of these exist to pin down a property that is easy to regress silently:

- **`Task.moveTo` returns the same instance for a no-op move.** A board drag that lands a card back in its own column
  must not emit an event that reaches every watcher as a notification.
- **Re-seeding a translation leaves an administrator's override alone.** Break this and every redeploy quietly reverts
  somebody's correction.
- **A user's permissions are the union of their roles, never stored.** Break this and RBAC dissolves into per-user
  exceptions nobody can audit.
- **`permissionsOf` agrees with `evaluate`** in both access policies. The client renders controls from the first; if
  they drift, the UI offers actions the policy then refuses.

## What is not covered yet

- **`mail-service`** — its behavior is talking to an SMTP server, and a test with that mocked proves only that the mock
  was called.
- **Web-layer tests** (`@WebMvcTest`) for controllers, serialization and ETag handling.
- **`ProjectQueryAdapter` and `TaskQueryAdapter`** — together the largest piece of handwritten JPQL in the repository,
  and therefore the most likely to be wrong. They need a Testcontainers test against a real PostgreSQL; nothing else
  would prove anything about them.
- **Application services** — everything so far is domain-level. The use cases are constructible without Spring by
  design, so this is a gap in effort rather than in possibility.
- **The shared libraries** — none of them has a test of its own, and between them, they hold the outbox dispatcher,
  the saga engine and compensation, and the Keycloak token mapping. They are exercised only indirectly, through the
  services that consume them, so a regression there surfaces as a puzzling failure somewhere else.
  `shared-saga-api` and `shared-translation` are the cheapest to start with: no Spring, no containers.
- **`api-gateway`** — the Token Handler and session encryption are security-critical and untested.

## Running the integration tests

They need a container runtime, so `./gradlew build` does **not** run them: every test extending
`IntegrationTestBase` carries `@Tag("integration")` and the tag is excluded unless asked for.

```bash
./gradlew test -PintegrationTests
```

Excluded rather than silently skipped on a missing runtime, deliberately. A suite that skips itself looks exactly like a
suite that passes, and the first time that matters is the time it would have caught something.

### With Podman

Testcontainers looks for a Docker socket. Podman provides a compatible one, but it has to be pointed at:

```bash
podman machine start
export DOCKER_HOST="unix://$(podman machine inspect --format '{{.ConnectionInfo.PodmanSocket.Path}}')"
export TESTCONTAINERS_RYUK_DISABLED=true
```

Ryuk is disabled because its container needs privileges rootless Podman does not grant; without that variable it fails
at startup and takes the suite with it. The cost is that stopped containers are not reaped automatically —
`podman container prune` after a run.
