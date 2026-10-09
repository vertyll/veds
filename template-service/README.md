# template-service

The reference service. Clone it when adding a bounded context; do not deploy it.

## What a clone gets

| Concern      | What you get                                                                                                             |
|--------------|--------------------------------------------------------------------------------------------------------------------------|
| Layering     | `domain` / `application` / `infrastructure`, with the dependency rule enforced by `checkHexagonalDependencies`           |
| CQRS         | Command and query ports already split, and the transactional decorator wired to them                                     |
| Errors       | `TemplateError` + `ErrorKind`, mapped to HTTP only in the web adapter                                                    |
| Translations | A catalog config and the registration runner                                                                             |
| Messaging    | Outbox tables, saga scaffolding, Avro wiring, an idempotent-receiver ledger                                              |
| Wiring       | `ApplicationBeansConfig`, `TransactionalUseCaseFactory`, `Slf4jUseCaseLogger`, security, Flyway, Dockerfile, CI workflow |

## After cloning

1. Rename the package, the module names in `settings.gradle.kts` and the Gradle project names
2. Replace `TemplateError` with the real failures — CI fails if a key it can emit is not
   declared in the catalog
3. Replace the placeholder aggregate and its migration
4. Assign a port and a database port, and add both to `docker-compose.local.yml`
5. Add the service to the root `settings.gradle.kts`
6. Delete what the context does not use — the saga and outbox tables are scaffolding, not a
   requirement. `translation-service` and `file-service` both removed the saga machinery
   because neither takes part in a distributed flow
7. Write a `README.md` describing what the context owns and what it deliberately does not

Step 6 matters more than it looks: a service that carries saga tables it never writes to invites
somebody to use them later without asking whether a distributed transaction is warranted.
