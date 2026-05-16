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

Every service generates Java `SpecificRecord` classes from **all** `.avsc` files
under `contracts/` at build time, via a Gradle task (`generateAvroJava`) that
invokes `avro-tools`. Generated sources land in
`build/generated/sources/avro/main/java` and are added to the service's main
Java source set.

Use the generated classes (e.g. `com.vertyll.veds.iam.mail.MailRequestedEvent`)
in publishers and consumers via the typed `Builder` API. Do **not** hand-roll
`GenericRecord` instances – they defeat the type-safety we get from codegen.

To regenerate locally:

```
./gradlew :iam-service:generateAvroJava :mail-service:generateAvroJava
```

## Compatibility mode

The registration script (`scripts/schema_registry/register_schemas.py`) sets
per-subject compatibility to **BACKWARD** by default, before pushing the
schema. This guarantees a new schema version can be read by consumers still
using the previous version.

Override via:

```
python scripts/schema_registry/register_schemas.py \
  --registry-url $SCHEMA_REGISTRY_URL \
  --compatibility FULL
```

In the GitHub Actions workflow (`.github/workflows/schema-registry.yml`) the
mode is configurable via the `SCHEMA_COMPATIBILITY` repository variable.

## Ownership

- The service that **publishes** an event owns its schema files.
- Consumers treat schemas as read-only.
