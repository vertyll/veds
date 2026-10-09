# Avro Contracts

This directory is the single source of truth for Apache Kafka event schemas.

## Layout

```
contracts/{service}/{topic}/v{version}/{schema}.avsc
```

For topics containing dots, replace `.` with `-` in the path and filename:

```
contracts/{service}/{topic-with-dashes}/v{version}/{topic-with-dashes}.avsc
```

Example:

```
contracts/mail-service/mail-requested/v1/mail-requested.avsc
```

## Which directory a schema belongs in

The **owning** service, which is not always the producer. For a domain event the owner is the producer; for a command it
is the consumer, because the consumer defines what it accepts.
`mail-requested` therefore lives under `mail-service/` even though iam-service and notification-service are the ones
that publish it.

Who owns and consumes each topic: [Event Catalog](../docs/events.md).

## Subject naming

Schemas are registered to Schema Registry using the topic name as the subject:

```
{topic}-value
```

## Code generation (SpecificRecord)

Dedicated services generates Java `SpecificRecord` classes from `.avsc` files.

> [!IMPORTANT]
>
> Use the generated classes in publishers and consumers via the typed `Builder` API.
> Do **not** hand-roll `GenericRecord` instances – they defeat the type-safety we get from codegen.

## Compatibility mode

The registration script (`scripts/schema_registry/register_schemas.py`) sets per-subject compatibility to **BACKWARD**
by default, before pushing the schema. This guarantees a new schema version can be read by consumers still using the
previous version.

Override via:

```bash
python scripts/schema_registry/register_schemas.py \
  --registry-url $SCHEMA_REGISTRY_URL \
  --compatibility FULL
```

## Ownership

- The service that **publishes** an event owns its schema files.
- Consumers treat schemas as read-only.
