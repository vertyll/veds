# project-service

Projects, membership and per-project authorization.

| Property  | Value                                                                                                                                                                                       |
|-----------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Port      | 8084                                                                                                                                                                                        |
| Database  | 5435                                                                                                                                                                                        |
| Publishes | `project-created`, `project-updated`, `project-archived`, `project-member-invited`, `project-member-joined`, `project-member-removed`, `project-category-changed`, `project-status-changed` |
| Consumes  | `mail-sent`, `mail-failed`, `user-registered`, `user-profile-updated`                                                                                                                       |

## Authorization

`ProjectAccessPolicy` is a pure domain service: no Spring, no repositories, no security context.
It evaluates rules in order and the first decision wins, so **deny overrides**:

1. `RESOURCE_STATE` — an archived project is frozen for everyone, including its owner
2. `OWNER_GRANT`
3. `PUBLIC_VISIBILITY` — read only
4. `ROLE_GRANT`

The order is the rule. Putting `OWNER_GRANT` first would make an owner able to edit an archived
project, which is exactly what archiving is supposed to prevent.

`permissionsOf` must agree with `permits` for every subject, and a test enforces that: the front
end renders its controls from the first, so a drift would offer actions the policy then refuses.

## Translations of user-entered labels

Category and status names are written by users, in whichever languages they need.

- **Completeness is required on the way in**, in `TranslationCompletenessValidator`, not in the
  aggregate constructor. Languages are seeded data — enforcing "all of them" during
  reconstitution would mean the day a language is added, every stored row becomes invalid and
  the next write to it throws.
- **On the way out** `resolveFor` returns the requested language or whatever the author wrote,
  and the response reports which in `nameLanguage`. There is no key to render here: a category
  is identified by a UUID, and an identifier on screen names nothing.

## Reads bypass the domain model

`ProjectQueryPort` returns view models straight from the database: `getProjectDetails` loads no
aggregate to flatten into a DTO, and the list query counts members in the same statement. See
[CQRS](../docs/cqrs.md).

## Identity

UUIDs assigned by the domain, not sequences. An aggregate needs its id before the transaction
commits so outbox events can reference it, and the id crosses service boundaries where a
per-database sequence would collide.
