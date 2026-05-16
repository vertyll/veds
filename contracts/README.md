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
contracts/iam-service/mail-requested/v1/mail-requested.avsc
```

## Subject naming

Schemas are registered to Schema Registry using the topic name as the subject:

```
{topic}-value
```

## Code generation (SpecificRecord)

Dedicated services generates Java `SpecificRecord` classes from `.avsc` files.

Use the generated classes in publishers and consumers via the typed `Builder` API. 
Do **not** hand-roll `GenericRecord` instances – they defeat the type-safety we get from codegen.

## Compatibility mode

The registration script (`scripts/schema_registry/register_schemas.py`) sets
per-subject compatibility to **BACKWARD** by default, before pushing the
schema. This guarantees a new schema version can be read by consumers still
using the previous version.

Override via:

```bash
python scripts/schema_registry/register_schemas.py \
  --registry-url $SCHEMA_REGISTRY_URL \
  --compatibility FULL
```

## Ownership

- The service that **publishes** an event owns its schema files.
- Consumers treat schemas as read-only.
