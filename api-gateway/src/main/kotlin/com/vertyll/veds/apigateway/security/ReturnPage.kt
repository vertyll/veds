package com.vertyll.veds.apigateway.security

/**
 * The page of FastDo a sign-in returns to, named in `return_to`. It travels inside the encrypted authorization request,
 * beside `state` and the PKCE verifier, and only a path on FastDo's own site is kept, so the sign-in cannot send the
 * browser elsewhere.
 */
internal object ReturnPage {
    const val PARAMETER = "return_to"

    /** Where the page rides in the authorization request, and then in the callback's exchange. */
    val ATTRIBUTE: String = ReturnPage::class.java.name

    private const val MOST_LENGTH = 2048

    /** A path that starts with a single `/`, with no backslash and no control character. */
    fun onThisSite(path: String?): String? =
        path?.takeIf {
            it.startsWith("/") &&
                !it.startsWith("//") &&
                '\\' !in it &&
                it.none(Char::isISOControl) &&
                it.length <= MOST_LENGTH
        }
}
