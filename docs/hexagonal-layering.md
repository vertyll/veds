# Hexagonal Layering

How the dependency rule is enforced in practice, and what it cost.

> [!NOTE]
>
> Applied in **project-service**, **iam-service**, **mail-service** and
> **template-service** — see [Status](#status).

## The rule

Dependencies point inwards only:

```text
infrastructure ──► application ──► domain
```

| Layer            | May depend on                               | Contains                                                   |
|------------------|---------------------------------------------|------------------------------------------------------------|
| `domain`         | Kotlin stdlib, `shared-error`               | Aggregates, value objects, repository *ports*, policies    |
| `application`    | `domain` plus the framework-free `shared-*` | Use cases, commands, response DTOs, inbound/outbound ports |
| `infrastructure` | everything                                  | JPA, Kafka, Avro, web, Spring wiring, adapters             |

`shared-saga-api` is a module holding only the saga vocabulary, with the Kotlin standard library as its single
dependency. Keeping them out of the Spring-bound modules is what stops an import of a saga status from putting the
whole framework on the application classpath.

In project-service the application layer's entire external import surface is:

```text
com.vertyll.veds.shared.saga.SagaStatus
com.vertyll.veds.shared.saga.SagaStepStatus
com.vertyll.veds.shared.saga.SagaTypeValue
java.time.Instant
java.util.UUID
```

No Spring, no `jakarta.validation`, no Jackson, no SLF4J.

## What that required

### `@Service` → explicit beans

There is no component scanning of the application layer. `infrastructure/config/ApplicationBeansConfig` constructs
each use case by hand. Verbose, but it means every use case is constructible in a plain unit test with no Spring
context.

### `@Transactional` → a decorator at the port

The transaction *boundary* is a use case; the transaction *mechanism* is infrastructure.
`TransactionalUseCaseFactory` wraps each inbound port in a dynamic proxy that runs every call inside a
`TransactionTemplate`, with the read-only methods named per port.

A dynamic proxy rather than seven handwritten decorators: those would be several hundred lines of pure delegation, and
each new use-case method would need a matching edit or would silently run without a transaction. The trade-off is that
read-only method names are checked at startup rather than by the compiler — a typo fails the context, it does not
degrade quietly.

> An earlier attempt inlined `transactionRunner.inTransaction { … }` into each method body.
> It does not work: a `return` inside a non-inline lambda is a non-local return and will not
> compile. The decorator avoids touching method bodies at all.

### Transaction mode follows the port

`TransactionalUseCaseFactory` takes no list of read-only method *names*. A query port is read-only in its entirety and
a command port is not, so the caller passes a constant. Nothing hand-maintained can drift out of step with a rename —
see [CQRS](./cqrs.md).

### Bean validation → the web adapter

Request DTOs live in `infrastructure/web/dto` with their `jakarta.validation` annotations, because those constraints are
enforced by Spring MVC and therefore describe an **HTTP**
contract. The use-case ports accept **commands** (`application/command`) built by the controller — already parsed and
typed, so a use case can never receive a half-validated request.

Validation is split, not duplicated:

| Kind                                               | Enforced by         |
|----------------------------------------------------|---------------------|
| Syntactic (required, length, e-mail)               | web adapter         |
| Invariants (name not blank, complete translations) | domain constructors |

### SLF4J → a port

`UseCaseLogger` in the application layer, `Slf4jUseCaseLogger` in infrastructure. Arguable — SLF4J is a facade, not a
framework — but it makes the dependency rule a single sentence with no exceptions, which a build check can enforce.

### The version check is the domain's

`VersionGuard.requireMatch` lives in the domain and the caller supplies the exception to throw. A shared helper
throwing Spring's `OptimisticLockingFailureException` would pull the framework back in through the back door.

### Paging is the domain's

A use case takes the domain's own `PageRequest` and returns a `PageResult`; the conversion to and from Spring Data's
`Pageable` / `Page` happens in the controller.

### Spring configuration out of the use cases

`EmailService` takes a `SenderAddress` value object rather than mail-service's `@ConfigurationProperties` object, and
`MailProperties` lives in infrastructure — SMTP host, port and password stay out of reach of code that has no business
knowing them.

## Status

Every service's application layer is framework-free, and `checkHexagonalDependencies` fails the build if a framework
reaches its resolved compile classpath. `template-service` carries the shape a new service starts from: the error
catalogue, the logging port, the transactional decorator, the bean configuration and the CQRS port split.

What an application layer may import from outside its own service is the framework-free shared modules —
`shared-saga-api`, `shared-error`, `shared-translation` — plus `java.time`, `java.util` and `kotlin.uuid`.

## Error catalogues

Each context owns one, in `domain/error/`:

| Service            | Catalogue       |
|--------------------|-----------------|
| `project-service`  | `ProjectError`  |
| `iam-service`      | `IamError`      |
| `mail-service`     | `MailError`     |
| `template-service` | `TemplateError` |

One choice worth naming: IAM maps both "no such user" and "wrong password" to
`iam.auth.invalid_credentials`. Telling them apart would be a user-enumeration oracle.
