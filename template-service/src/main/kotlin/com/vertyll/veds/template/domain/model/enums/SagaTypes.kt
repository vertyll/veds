package com.vertyll.veds.template.domain.model.enums

enum class SagaTypes(
    val value: String,
) {
    EXAMPLE_SAGA("exampleSaga"),
    ;

    companion object {
        fun fromString(value: String): SagaTypes? = SagaTypes.entries.find { it.value == value }
    }
}
