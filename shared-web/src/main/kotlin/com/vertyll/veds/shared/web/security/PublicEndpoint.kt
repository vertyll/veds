package com.vertyll.veds.shared.web.security

/**
 * The endpoint answers a caller nobody authenticated.
 *
 * The gateway lets these through without a token, so the reason belongs beside the method rather
 * than in a route matcher three services away. Anything that reads or writes a person's data needs
 * a permission or [AuthorizedInUseCase] instead.
 *
 * @property why what makes an unauthenticated caller acceptable here.
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class PublicEndpoint(
    val why: String,
)
