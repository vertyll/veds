# Inbox

How a consumer handles each event once, even though Kafka delivers at least once.

`ProcessedEventGuard.claim(eventId, consumerGroup)` writes to a ledger with a UNIQUE constraint on
`(eventId, consumerGroup)`. A delivery whose id is already there is skipped.

The claim is written **in the handler's own transaction**, so the two share a fate. That is what makes the
retry and dead-letter machinery mean anything:

| Handler outcome | Claim                              | Redelivery                                                  |
|-----------------|------------------------------------|-------------------------------------------------------------|
| commits         | committed with the business writes | skipped as a duplicate                                      |
| throws          | rolled back with them              | a real retry, and after `MAX_RETRIES` the dead letter topic |

A claim committed independently would invert the second row: every retry would find its own claim, report a
duplicate and return successfully, so a transient failure would drop the message on its first attempt and
nothing would ever reach the dead letter topic.

Delivery is therefore **at-least-once**, and every `@KafkaListener` that claims is `@Transactional` so the
claim and the handler share one unit of work. A handler must tolerate being re-entered after a failure.

Two consumers racing on the same event both pass the existence check; one loses on the UNIQUE constraint and
rolls back, and its redelivery then sees the committed row and skips.
