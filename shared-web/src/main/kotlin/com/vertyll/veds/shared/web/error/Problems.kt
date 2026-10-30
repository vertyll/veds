package com.vertyll.veds.shared.web.error

import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import java.net.URI

/**
 * Builds the RFC 9457 problem document this platform answers refusals with.
 *
 * One place decides the shape so a servlet service and the reactive gateway
 * cannot drift apart: `type` identifies the problem, `code` repeats the bare
 * catalogue key so a client looks up its translation without parsing a URI, and
 * `detail` is left out because the prose belongs to `translation-service`.
 */
object Problems {
    private const val TYPE_PREFIX = "urn:veds:error:"

    /** A problem detail with [status], the catalogue key [code], and the optional [instance] and [properties]. */
    fun of(
        status: HttpStatus,
        code: String,
        instance: String? = null,
        properties: Map<String, Any> = emptyMap(),
    ): ProblemDetail =
        ProblemDetail.forStatus(status).apply {
            type = URI.create(TYPE_PREFIX + code)
            title = status.reasonPhrase
            if (instance != null) this.instance = URI.create(instance)
            setProperty("code", code)
            properties.forEach { (name, value) -> setProperty(name, value) }
        }
}
