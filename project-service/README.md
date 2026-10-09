# project-service

Projects, membership and per-project authorization.

| Property  | Value                                                                                                                                                                                       |
|-----------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Port      | 8084                                                                                                                                                                                        |
| Database  | 5435                                                                                                                                                                                        |
| Publishes | `project-created`, `project-updated`, `project-archived`, `project-member-invited`, `project-member-joined`, `project-member-removed`, `project-category-changed`, `project-status-changed` |
| Consumes  | `mail-sent`, `mail-failed`, `user-registered`, `user-profile-updated`                                                                                                                       |

## Reads bypass the domain model

`ProjectQueryPort` returns view models straight from the database: `getProjectDetails` loads no
aggregate to flatten into a DTO, and the list query counts members in the same statement. See
[CQRS](../docs/mechanisms/cqrs.md).

## Identity

UUIDs assigned by the domain, not sequences. An aggregate needs its id before the transaction
commits so outbox events can reference it, and the id crosses service boundaries where a
per-database sequence would collide.

## Mechanisms

- [Access policy](docs/mechanisms/access-policy.md) – How project-service decides what a person may do with a project.
- [Localized labels](docs/mechanisms/localized-labels.md) – How category and status names written by users are stored
  and served in several languages.
