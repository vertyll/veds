# CQRS

Command Query Responsibility Segregation, as applied here — and where it is deliberately not.

## Scope

CQRS is a pattern for a bounded context, not an application-wide architecture. Greg Young, who named it, is explicit
that applying it everywhere is the most common way people hurt themselves with it. So the decision is per service:

| Service                | Applied     | Why                                                              |
|------------------------|-------------|------------------------------------------------------------------|
| `project-service`      | both levels | `getProjectDetails` read five aggregates to build one flat DTO   |
| `iam-service`          | ports only  | queries are single-aggregate; a read model would add nothing     |
| `template-service`     | ports only  | it is the pattern every new service is cloned from               |
| `mail-service`         | **none**    | one read endpoint returning an empty page — nothing to segregate |
| `task-service`         | planned     | reads vastly outnumber writes; read models already required      |
| `notification-service` | planned     | same                                                             |

Splitting `mail-service` would add a port, a service and a bean per read that does not exist. That is the "hurt yourself
with it" case, so it keeps one port and this note.

## Level 1 — separate ports

Applied in project-service, iam-service and template-service.

| Before                     | After                                               |
|----------------------------|-----------------------------------------------------|
| `ProjectUseCase`           | `ProjectCommandUseCase` + `ProjectQueryUseCase`     |
| `ProjectCategoryUseCase`   | `ProjectCategoryCommandUseCase` + `…QueryUseCase`   |
| `ProjectStatusUseCase`     | `ProjectStatusCommandUseCase` + `…QueryUseCase`     |
| `ProjectMembershipUseCase` | `ProjectMembershipCommandUseCase` + `…QueryUseCase` |
| `ProjectInvitationUseCase` | `ProjectInvitationCommandUseCase` + `…QueryUseCase` |
| `UserUseCase`              | `UserCommandUseCase` + `UserQueryUseCase`           |
| `RoleUseCase`              | `RoleCommandUseCase` + `RoleQueryUseCase`           |
| `TemplateSagaUseCase`      | `TemplateCommandUseCase` + `TemplateQueryUseCase`   |

`ProjectTypeUseCase` and `ProjectRoleUseCase` became query-only: adding a type or a role means adding an enum constant,
which is a code change rather than a runtime command.

Mapping is shared rather than duplicated: `UserResponseMapper` and `RoleResponseMapper` are named types both sides
call. A private helper on each side would let the read and write halves drift into returning different shapes for the
same entity.

The split also decides transaction mode. `TransactionalUseCaseFactory` reads it from which side a port belongs to — a
query port is read-only in its entirety — rather than from a list of method names that a rename could silently
invalidate.

## Level 2 — the read side does not load aggregates

Applied in project-service only. IAM's queries each return a single aggregate with nothing to assemble, so a dedicated
read model there would be a second place to maintain for no gain — level 2 is worth it when a query spans aggregates.

The write side loads `Project`, `ProjectMember` and `ProjectRole` because it must enforce invariants that span them. A
read has no invariants to enforce.

`ProjectQueryPort` returns view models; `ProjectQueryAdapter` builds them with JPQL tuple queries and native reads for
the element-collection tables. Nothing on that path is reconstituted into a domain object.

What it costs compared with loading aggregates:

| Operation           | Through aggregates                          | Through the read model                        |
|---------------------|---------------------------------------------|-----------------------------------------------|
| `getProjectDetails` | 5 repository calls, every aggregate rebuilt | 1 authorization load + 4 flat projections     |
| `searchProjects`    | page query + one member-count query per row | one statement, count as a correlated subquery |

The deeper point is not the query count: the read model is free to have a shape the write model does not, so queries
stop being constrained by aggregate boundaries. That coupling is what CQRS exists to remove.

**Authorization still runs on the read path**, through the same `ProjectAccessPolicy` as writes. CQRS separates models,
not access rules; a read model that skipped the check would be a data leak.

**Not every read bypasses the domain.** `getMyInvitations` still goes through the repositories:
the list is short, per-user and rarely fetched, so a dedicated projection would be a second place to maintain for no
measurable gain.

## What was deliberately not done

**No command bus / mediator.** A dispatcher gives a hook for cross-cutting concerns, but the transactional decorator and
the outbox already cover those here. It would cost reflection and the ability to jump from a call site to its
implementation, and buy nothing.

**No separate read database, no projections fed by events.** That adds eventual consistency to operations that are
currently immediately consistent — create a project and not see it in the list. Worth it at a scale this application
does not have.

**No event sourcing.** A different pattern, frequently confused with this one. The aggregates here are stored as current
state, and the outbox publishes integration events; neither implies rebuilding state from an event log.
