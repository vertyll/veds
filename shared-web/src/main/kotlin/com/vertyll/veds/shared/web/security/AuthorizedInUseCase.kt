package com.vertyll.veds.shared.web.security

/**
 * The decision needs the resource's own state, so the use case takes it rather than the method.
 *
 * A static permission cannot express "a member of this project may edit this task": the answer
 * depends on membership, ownership and the aggregate's lifecycle, none of which the filter chain
 * can see. Naming the collaborator here is what separates a deliberate decision taken deeper from
 * an endpoint nobody guarded.
 *
 * @property by the type that refuses the call — a policy, an authorization service or the use case
 *              itself.
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class AuthorizedInUseCase(
    val by: String,
)
