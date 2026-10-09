# shared-translation-client

Registers a service's translation keys with `translation-service` at start-up.

Depends on [`shared-translation`](../shared-translation/README.md) for the key-declaration DSL
whose catalogues it ships.

See [Shared Modules](../docs/shared-modules.md) for how this module relates to the others.

## Why it is a separate module

This is the one synchronous call in the system, and it is not on a request path; what happens when it fails is in
[Service Dependencies](../docs/service-dependencies.md). `translation-service` itself does not take this module — it is
the registry.

## API documentation

```bash
./gradlew docs
```
