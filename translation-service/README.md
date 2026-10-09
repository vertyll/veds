# translation-service

The translation catalogue: keys, languages and their text.

| Property  | Value   |
|-----------|---------|
| Port      | 8087    |
| Database  | 5438    |
| Publishes | nothing |
| Consumes  | nothing |

No outbox and no saga tables: it is a catalogue other services read and an administrator edits.

How keys are declared and registered, why defaults and overrides are separate columns, why rendering uses ICU4J,
what a missing key renders as, and the cacheable public endpoint are in [Translations](../docs/translations.md), which
covers every service's part.
