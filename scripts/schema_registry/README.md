# Schema Registry Scripts

This directory contains automation scripts for managing Avro schemas in Confluent Schema Registry.

## Register All Schemas

Run the following script to discover and register all `.avsc` files located in the `contracts/` directory.

```bash
python scripts/schema_registry/register_schemas.py --registry-url http://localhost:8081
```

> **Note on Compatibility:**  
> By default, the script enforces **BACKWARD** compatibility for each subject before registering a new schema version. You can override this using the `--compatibility` flag.

---

## Subject Naming Convention

The script automatically derives the Schema Registry **Subject** name from the directory structure of your contracts.

**Expected Path Structure:** `contracts/{service}/{topic}/v{version}/{schema}.avsc`.

**Resulting Subject Name:** `{topic}-value`.

### Example

If a new schema is placed at: `contracts/mail-service/mail-requested/v1/mail-requested.avsc`.

The script will register it under the subject: `mail-requested-value`.
