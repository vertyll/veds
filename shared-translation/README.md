# shared-translation

The key-declaration DSL and the ICU renderer, shared by every service.

Depends on ICU4J and the Kotlin standard library, and nothing else. It is referenced by
application layers, which must stay framework-free.

Why ICU4J and not `java.text.MessageFormat` is in [Translations](../docs/translations.md).

## What lives here

| Type                  | Role                                                                  |
|-----------------------|-----------------------------------------------------------------------|
| `translations { }`    | DSL a service uses to declare its keys and shipped defaults           |
| `IcuPatternValidator` | Refuses a pattern that will not compile, at declaration and at save   |
| `MessageResolver`     | Renders a key; a missing one renders as the key itself                |
| `TranslationSnapshot` | An immutable set for one language, with the version served as an ETag |

A duplicate key fails at start-up: two places believing they own the same message would make the
winner depend on declaration order.
