package com.vertyll.veds.mail.domain.model

@JvmInline
value class SenderAddress(
    val value: String,
) {
    init {
        require(value.isNotBlank()) { "sender address must not be blank" }
    }
}
