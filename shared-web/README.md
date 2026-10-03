# shared-web

Keycloak authentication, HTTP concurrency helpers and the shared configuration defaults.

This is the only shared library `api-gateway` takes. The gateway is reactive and has no
database, so this module deliberately carries no JPA, no Kafka and no Avro — pulling those in
is what forced the gateway to disable Hibernate autoconfiguration before the shared libraries
were split.

See [Shared Modules](../docs/shared-modules.md) for how this module relates to the others.

## What lives here

| Type                                         | Role                                                                   |
|----------------------------------------------|------------------------------------------------------------------------|
| `KeycloakJwtAuthenticationConverter`         | Realm roles to Spring authorities, servlet flavour                     |
| `ReactiveKeycloakJwtAuthenticationConverter` | The same mapping for the reactive gateway                              |
| `ETagUtils`                                  | Builds and parses the weak `ETag` exposed from a JPA `@Version` column |
| `OptimisticLockingValidatorUtils`            | Verifies a client-supplied `If-Match` version before a write           |
| `SharedConfigProperties`                     | The `application.shared.keycloak.*` settings                           |

`shared-web-config.yml` ships the Keycloak and OAuth2 defaults; an `EnvironmentPostProcessor`
loads it so no service has to import it by hand.

## API documentation

```bash
./gradlew docs
```
