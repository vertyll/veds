package com.vertyll.veds.sharedinfrastructure.saga

/**
 * Naming convention for saga-compensation Kafka topics shared by all
 * services participating in the choreography.
 *
 * The infrastructure module owns only the **shape** of the topic name
 * (`saga-compensation-<participant>`); the actual participant identifier
 * is supplied by each microservice, preserving its autonomy.
 *
 * Usage:
 * ```
 * companion object {
 *     // const val — usable in @KafkaListener and other annotations
 *     const val SAGA_COMPENSATION_TOPIC: String = SagaCompensationTopic.PREFIX + "iam"
 * }
 * ```
 *
 * For runtime composition (e.g. tests, configuration), use
 * [forParticipant] which validates the participant name.
 *
 * The same convention must be mirrored by the topic provisioner
 * (see `infra/kafka/topics.tf`).
 */
object SagaCompensationTopic {
    const val PREFIX: String = "saga-compensation-"

    fun forParticipant(participant: String): String {
        require(participant.isNotBlank()) { "Saga participant name must not be blank" }
        return "$PREFIX$participant"
    }
}

