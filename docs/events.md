# Event Catalog

Every topic in the system, who owns it and who reads it. Schemas live in `contracts/`; this page is the map.

## Ownership rule

A topic belongs to **one** service, which is the only one allowed to change its schema. For domain events that is the
producer. For *commands*, it is the **consumer** — it defines what it accepts — which is why `mail-requested` belongs to
mail-service even though mail-service never produces it.

## Domain events

| Topic                      | Owner           | Consumed by                      |
|----------------------------|-----------------|----------------------------------|
| `user-registered`          | iam-service     | project, task, notification      |
| `user-profile-updated`     | iam-service     | project, task, notification      |
| `role-permissions-changed` | iam-service     | project, task, translation, mail |
| `project-created`          | project-service | task                             |
| `project-updated`          | project-service | task                             |
| `project-archived`         | project-service | task                             |
| `project-member-invited`   | project-service | notification                     |
| `project-member-joined`    | project-service | task, notification               |
| `project-member-removed`   | project-service | task                             |
| `project-category-changed` | project-service | task                             |
| `project-status-changed`   | project-service | task                             |
| `task-created`             | task-service    | notification                     |
| `task-assigned`            | task-service    | notification                     |
| `task-status-changed`      | task-service    | notification                     |
| `task-archived`            | task-service    | notification                     |
| `task-comment-added`       | task-service    | notification                     |
| `file-confirmed`           | file-service    | —                                |
| `file-deleted`             | file-service    | task                             |
| `mail-sent`                | mail-service    | project                          |
| `mail-failed`              | mail-service    | project                          |

`file-confirmed` has no consumer yet. It is published because the alternative — adding it later, once something needs
it — means a producer change at the moment a consumer is already waiting.

## Commands

| Topic            | Owner        | Produced by                       |
|------------------|--------------|-----------------------------------|
| `mail-requested` | mail-service | iam-service, notification-service |

`mail-requested` names a template and its variables; it carries no subject and no prose, because
the wording belongs to `mail-service`. See [Translations](./translations.md).

## Compensation

| Topic                       | Service         |
|-----------------------------|-----------------|
| `saga-compensation-mail`    | mail-service    |
| `saga-compensation-project` | project-service |

Internal to one service: a compensation event is how a saga undoes its own steps, never a message another context reacts
to. See [Eventual Consistency](./eventual-consistency.md).

`iam-service`, `task-service`, `notification-service`, `translation-service` and `file-service` have no compensation
topic — none of them starts a saga or owns a step of one. They react to events other contexts publish, and a consumer
has nothing to undo: re-reading the event is the recovery.

## Conventions that hold for all of them

- **Written to the outbox, never sent directly.** Kafka does not join the database transaction, so publishing directly
  would let an event escape from a transaction that later rolled back.
- **Keyed by the aggregate id**, so all events about one entity land on the same partition and are consumed in order — a
  status change cannot overtake the creation it followed.
- **Carrying full state, not deltas.** A consumer that missed an earlier event still converges on the right state
  instead of applying a change to a value it never had. This is also what makes at-least-once delivery safe.
- **Claimed through `ProcessedEventGuard`** before handling, so a redelivery costs nothing.
- **Translated at the boundary.** Generated Avro types never travel past the consumer adapter; a schema change upstream
  is a compile error in one file rather than a ripple through a domain.

## Adding one

1. Write the schema under `contracts/{owner-service}/{topic}/v1/`
2. Add the topic to `infra/kafka/topics.tf`
3. Add the constant to the owner's `*KafkaTopics` object
4. Publish through the outbox, never directly
5. On the consuming side, translate to the local model in the adapter and claim the event first
