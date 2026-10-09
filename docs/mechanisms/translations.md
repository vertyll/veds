# Translations

Three different things get called "translation" in this system, and they work differently.

| Kind                                       | Owner                      | Storage                                 | Missing value renders as           |
|--------------------------------------------|----------------------------|-----------------------------------------|------------------------------------|
| UI copy (errors, labels, validation)       | code declares, admin edits | `translation-service`                   | **the key itself**                 |
| Data labels (project categories, statuses) | the user who typed them    | the owning service                      | the language the author did write  |
| Mail bodies and subjects                   | code                       | `mail-service` templates, producer code | nothing — English is the only text |

## Two tracks: who renders the text

The split above decides **where** a string becomes readable, and the two halves never mix.

| Track           | Response carries         | Rendered by | Language comes from           |
|-----------------|--------------------------|-------------|-------------------------------|
| System messages | a key and its parameters | the client  | whatever the client is set to |
| Data labels     | finished text            | the server  | `X-Lang` on the request       |

A system message is a contract: `{"code": "project.invitation.expired", "params": {}}` names a condition, and the
condition is the same in every language. Rendering it at the edge means one catalog serves the web client, a future
mobile client and a support tool alike, and a corrected wording reaches all of them without a redeployment.

A data label has no key — a category is a UUID somebody named — so there is nothing for a client to look up. The server
resolves it against `X-Lang` and returns `statusName`, already readable.

> [!WARNING]
>
> A service that starts returning finished sentences for system messages breaks this. The client then has no key to
> branch on, and the response language is fixed at whatever the server guessed.

## Why a missing key renders as the key

Not a fallback. A fallback substitutes another language and the reader assumes the text is finished. A bare
`project.member_removed` on screen is the opposite: visible, greppable, impossible to miss in review, and it names
exactly what has to be fixed.

Throwing instead would take a page down over one absent string. The pattern itself is checked twice before it can
reach a reader, so what survives to render is an *absent* key, not a malformed one.

Data labels are the one exception, and for a concrete reason: a category is identified by a UUID, so there is no key to
print. `a3f2e1b4-…` on screen names nothing. The read returns what the author did write and the response reports which
language that is, so the client can mark it.

## Ownership

**Code owns the keys. Administrators own the text.**

Each service declares its keys with the DSL in `shared-translation`:

```kotlin
val catalogue = translations("project-service") {
    key("project.member_count") {
        pl("{count, plural, one{# członek} few{# członkowie} many{# członków} other{# członka}}")
        en("{count, plural, one{# member} other{# members}}")
    }
}
```

At start-up the service registers this with `translation-service`. Registration runs off the
start-up thread and retries every `application.translation.client.registration-retry-interval` until it is
accepted, so boot order does not matter: a key that never registered renders as the key itself to
every reader, which is too visible a failure to leave until the next restart.

Registration is additive: it inserts new keys and refreshes shipped defaults, and **never touches
an override**.

That is why `default_value` and `override_value` are two columns. One column written by both sides would mean every
redeploy silently reverts somebody's correction — the single most likely way an editable-translations feature turns into
a liability.

There is no API to create or delete a key. Deleting one would leave the emitting code with nothing to render; creating
one would produce a key nothing ever asks for.

## E-mail is written in English

Every message this platform sends reads in English, and the wording lives with the template that
renders it rather than with the service that asked for the message: a producer names *which*
message to send, and `mail-service` owns how it reads. That is why `mail-requested` carries a
template name and its variables, and no subject line.

Localization stops at the screen. The interface — in-app notifications included — is rendered from
this catalog in whichever language the reader has selected, carried on every request as `X-Lang`.
An e-mail has no request and no reader present to ask, so it does not try to guess one.

### Where `common.*` lives

Declared once, in `translation-service`, not in every service. Registration refuses a key another service already owns —
deliberately, since two contexts claiming the same message means whichever deploys last silently wins. Every service
declaring its own `common.version_mismatch`
would hit exactly that rule, so the platform catalog owns the platform's keys.

### Registration failure does not stop a service

A catalog that could not be published is a degraded state: clients use whatever the catalog already holds, and a
genuinely missing key renders as the key. Refusing to start would mean a brief `translation-service` outage takes every
service down with it. Registration is retried on the next restart.

## Languages

Seeded from code, same as keys. A language is data, never an enum: with a fixed set of constants every translatable
aggregate would owe a value for each one, so adding a language would invalidate every stored row and the next write to
it would throw.

A tag is a `LanguageTag` value class: BCP 47, normalized to lower case. Normalization is not cosmetic — without it
`pl`, `PL` and `pl-PL` become three languages nobody ever reconciles.

Completeness of a *submitted* label is enforced in the application layer against the seeded catalog, so a caller still
cannot save a category naming itself in only one of the configured languages.

## ICU, and why not `java.text.MessageFormat`

`MessageFormat` has no CLDR plural rules, so Polish `one/few/many/other` — "1 zadanie",
"3 zadania", "5 zadań" — is not expressible. `shared-translation` uses **ICU4J**, and the front end uses
`ngx-translate-messageformat-compiler` for the same reason.

Patterns are validated twice: when a service declares them (start-up, in the owning service)
and when an administrator saves one (rejected with a reason). An unbalanced brace in
`{count, plural, one{…}` does not fail quietly — it throws at render time, in the middle of a page nobody was editing.

## The public endpoint

`GET /translations/{language}` is unauthenticated and cacheable. Unauthenticated because the login screen needs its
labels before anybody has a token; cacheable because every application start in every tab fetches it, once per
language the client offers.

The ETag is `MAX(updated_at)` for the language — cheaper than hashing the set, and monotonic:
any write moves it, so a client holding the current marker is holding the current translations.

`GET /translations/languages` exists so that no other component has to compile its own list; it answers with the
languages actually seeded.

## The language header

The client states its language in **`X-Lang`**, not `Accept-Language`.

`Accept-Language` is a forbidden header name in the Fetch standard, so a browser refuses to let JavaScript set it — an
application can only ever send the browser's locale that way, never the language the user actually picked in the
interface. A custom header is the only way to carry a deliberate choice.

## Key naming

Every key is prefixed with the context that owns it:

| Shape                    | Owner                          | Example                              |
|--------------------------|--------------------------------|--------------------------------------|
| `common.*`               | platform (translation-service) | `common.version_mismatch`            |
| `<context>.*`            | that bounded context           | `project.invitation.expired`         |
| `validation.<context>.*` | that bounded context           | `validation.iam.password_complexity` |

The prefix is what makes ownership explicit rather than accidental. An unprefixed `validation.email_required` is a key
two contexts will both reach for, and registration refuses the second one — so whichever deploys first claims it and the
other fails to register. The prefix removes the collision instead of resolving it by deployment order.

## Mail is outside all of this

Outgoing mail is English, and nothing in the translation machinery touches it.

| Part            | Where it comes from                                               |
|-----------------|-------------------------------------------------------------------|
| Body            | a Thymeleaf template in `mail-service/…/resources/templates`      |
| Subject         | a literal at the producer, chosen per message type                |
| Template choice | `templateName` on `MailRequestedCommand`, an `EmailTemplate` name |

`mail-service` registers its own catalog, but only for the error keys its API returns. It resolves nothing for the
messages it sends.

The reason is that no language is available at the point of sending. `MailRequestedCommand` carries no language, a
recipient record carries no preference, and the one case that needs mail most — inviting somebody who has no account
yet — has no user to ask. A translated subject needs all three: a field on the event, a preference on the recipient with
a configured default behind it, and resolution in `mail-service`, which already has `shared-translation-client` on its
classpath.

Because the subject is presentation copy rather than a domain fact, it lives at the producing adapter, not on
`NotificationType`. The enum carries only the key its in-app notification renders with.

> [!WARNING]
>
> A template name is `EmailTemplate.<NAME>`, upper case, matching the file. A producer that sends any other spelling
> gets no error at the call site: `mail-service` logs `Invalid template name` and drops the message.

## The front end

Angular offers two unrelated ways to translate, and only one of them can read a catalog that an administrator edits.

| Property              | `@angular/localize`   | `ngx-translate`             |
|-----------------------|-----------------------|-----------------------------|
| Resolved              | at build time         | at run time                 |
| ICU plurals           | built in              | needs a `TranslateCompiler` |
| Changing language     | one bundle per locale | no rebuild                  |
| Catalog from a server | not possible          | the normal case             |

`@angular/localize` compiles messages into the bundle, so the catalog is frozen at build time and an administrator's
correction would never reach anybody. The front end uses `ngx-translate` for that reason alone, and pays for it by
having to add ICU back. Its default parser substitutes `{{param}}` and does nothing else — no plural rules, no
select — so `ngx-translate-messageformat-compiler` is registered as the `TranslateCompiler`. Without it Polish
`few`/`many` do not work on the client and every plural collapses to one form.

`BackendCatalogueLoader` merges two sources into the catalog `ngx-translate` holds:

```typescript
forkJoin({
  ui: this.http.get<Catalogue>(`./i18n/${language}.json`),
  backend: this.http.get<BackendCatalogue>(`${environment.apiUrl}/translations/${language}`),
})
```

The static file carries interface copy that no back-end service knows about — button labels, page titles, form
captions. `GET /translations/{language}` carries every key a service can put in a response. Both are needed: drop the
static file and the interface loses its labels, drop the request and **every** back-end key renders as itself, which is
what a user sees as `project.invitation.expired` in a toast.

The back end wins on a collision, because it owns the keys it declares. A failed request falls back to an empty
catalog rather than an error, so `translation-service` being briefly unreachable costs the back-end messages, not the
whole interface.

Both catalogs are ICU, single brace. `{{param}}` is `ngx-translate`'s own interpolation syntax and the compiler does
not read it — under ICU `{{count}}` renders as `{5}`, braces included. One dialect across both sources is what keeps
that from being a per-key accident.

> [!NOTE]
>
> `environment.availableLanguages` is a constant list in the front end, while `GET /translations/languages` exists and
> answers the same question. A language seeded in the back end therefore needs a front-end release to become
> selectable.

## Administration

`/admin/translations/**`, behind the **global** `ADMIN` realm role — not the project-scoped policy used elsewhere.
Editing translations is not something one can be permitted inside one project and not another, so the per-project model
would be the wrong shape.

- keys are listed with both columns and the languages they still lack
- an override can be dropped, reverting to what the owning service ships, without the editor needing to know the
  original text
- `updated_by` is recorded on every override

### Spreadsheet import and export

`GET /admin/translations/export` produces an `.xlsx` with a fixed, positional layout: key, source service, description,
then one column per language.

**Read by position, not by header text.** The headers are themselves translated, so a file exported in Polish and
re-imported would not match headers looked up in English. Positional reading also means the file survives a translator
renaming a heading.

Headers are resolved **on the server**, in the language from `X-Lang`, because the file is produced there — sending raw
keys would put `translation.export.column.key` in a column heading. The language columns are headed by each language's
own display name, which needs no translation and cannot go missing.

A blank cell is skipped, not imported as an empty string: leaving a cell alone means
"unchanged", not "clear this translation".

`SXSSFWorkbook` rather than `XSSFWorkbook`, since a few thousand keys times a handful of languages is a large sheet and
the streaming writer keeps only a window of rows in memory.

Import reports rather than fails: a spreadsheet of a few thousand rows with three bad ones applies the rest and names
what was skipped. Rejecting the file wholesale would mean a translator fixing one cell and re-uploading everything. Rows
for unknown keys and unknown languages are reported, never created — both sets are owned by code.
