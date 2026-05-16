# Schema Registry scripts

## Register all schemas

```
python scripts/schema_registry/register_schemas.py --registry-url http://localhost:8081
```

The subject name is derived from the schema path:

```
contracts/{service}/{topic}/v{version}/{schema}.avsc
```

Subject: `{topic}-value`

