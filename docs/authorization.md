# Authorization

Answers: who decides what a person may do, and where that decision is made.

## The split

| Concern                         | Owner                     | Where it lives                                  |
|---------------------------------|---------------------------|-------------------------------------------------|
| Who the person is               | Keycloak                  | the access token, `realm_access.roles`          |
| Which permissions exist         | the service enforcing one | its `PermissionCatalogue`, declared in code     |
| What a role grants              | iam-service               | `role`, `permission`, `role_permission_mapping` |
| Whether this request is allowed | the service handling it   | its own projection, no call to iam              |

Keycloak is the identity provider, not the authorization system. It says who is
calling and which platform-wide roles they hold; it never says what those roles
may do. Fine-grained permissions stay out of the token: a token cannot be made
smaller once it grows, and a permission taken away would keep working until the
token expired.

## A permission is declared where it is enforced

A permission is real only where some code checks it, so the catalogue is code:

```kotlin
@Bean
fun taskPermissionCatalogue(): PermissionCatalogue =
    permissions("task") {
        permission("VIEW_TASKS", "See the tasks of a project", RoleScope.PROJECT)
        permission("MANAGE_TASKS", "Create, edit and archive tasks", RoleScope.PROJECT)

        stockRole("MANAGER", RoleScope.PROJECT, "VIEW_TASKS", "MANAGE_TASKS")
    }
```

Each service registers its catalogue with iam-service at start-up over HTTP
(`POST /internal/authz/catalogue`). HTTP rather than an event, for the same
reason the translation catalogue uses it: a rejected catalogue is a deployment
mistake and belongs in the log of the service that made it.

Registration never blocks a start-up and never gives up: it runs off the start-up
thread and retries every `application.authz.client.registration-retry-interval` until
iam-service accepts it. Order of boot therefore does not matter — a service that
comes up first joins on its own once iam-service answers.

That retry is not only about the panel's module list. Registering is what makes
iam-service announce every role, which is how a projection is filled in the first
place; a service that never registered would fail closed on every permission it
checks.

| Rule                                                 | Why                                                                        |
|------------------------------------------------------|----------------------------------------------------------------------------|
| the module that enforces a permission declares it    | a name nobody checks is a lie in the administration panel                  |
| a permission the module stops declaring is withdrawn | roles holding it are updated and announced, so nothing grants a dead name  |
| a permission names the scope it can be held in       | a project role granted `USERS_MANAGE` would grant something nobody honours |

## Scope

| Scope     | Held                | Reaches Keycloak     | Assigned in              |
|-----------|---------------------|----------------------|--------------------------|
| `GLOBAL`  | across the platform | yes, as a realm role | the administration panel |
| `PROJECT` | inside one project  | no                   | project membership       |

A project role never reaches Keycloak. "MANAGER in project X" is a relation, not
an identity, and with two hundred projects the token would carry two hundred
roles.

## Stock roles

A module may name the roles it ships with and what they start out granting. The
grant applies only where iam-service has no such role yet; a role that already
exists gains only permissions the registry has never seen. So a module shipped
later reaches the roles it belongs to, while an administrator's edits survive
every redeploy.

## `unrestricted`

`ADMIN` holds every permission, including ones registered tomorrow. It is a flag,
not a list: deriving it from "currently holds all" would silently widen a role the
day a new permission appeared.

The last unrestricted role cannot be deleted, and the last person holding one
cannot have it taken away. The way back from locking everybody out is a
handwritten row in the database.

## How a decision is made

`role-permissions-changed` carries what a role grants. Every service that
enforces permissions keeps a projection of it, claimed through the inbox like any
other event, and keeps only the names its own catalogue declares — a permission
another module enforces means nothing locally.

```kotlin
@PreAuthorize("@authz.has('USERS_MANAGE')")
```

`authz` reads the roles from the token and the grants from the local projection.
A service without a projection grants nothing rather than falling back to the
role in the token: authorization that cannot read what it needs fails closed.

Inside a project the same projection answers a different question — the policy
objects (`TaskAccessPolicy`, `ProjectAccessPolicy`) take the member's role and
ask what it grants. A platform-wide role gives no access to a project's tasks:
access follows membership, so an administrator who needs to work in a project
joins it.

## Who holds what

| Service             | Module        | Projection      | Guards                                   |
|---------------------|---------------|-----------------|------------------------------------------|
| iam-service         | `admin`       | own tables      | `USERS_*`, `ROLES_*`                     |
| translation-service | `translation` | `GLOBAL` roles  | `TRANSLATIONS_VIEW`, `TRANSLATIONS_EDIT` |
| mail-service        | `mail`        | `GLOBAL` roles  | `MAIL_LOGS_VIEW`                         |
| project-service     | `project`     | `PROJECT` roles | `ProjectAccessPolicy`                    |
| task-service        | `task`        | `PROJECT` roles | `TaskAccessPolicy`                       |

## Adding a module

1. Declare a `PermissionCatalogue` bean naming the module's permissions and their scope.
2. Depend on `shared-authz` and `shared-authz-client`, scan `com.vertyll.veds.shared.authz.client`, set
   `application.authz.client.base-url`.
3. Keep a `role_permission_projection` table and a consumer of `role-permissions-changed`, filtered to the scope the
   module's permissions use.
4. Expose a `RolePermissionsSource` reading that projection.
5. Guard endpoints with `@PreAuthorize("@authz.has('…')")`.

Nothing else has to change: iam-service learns the module exists when it registers.

## What a service depends on

Nothing on the request path. A decision reads the token and the local projection,
both already in the process. iam-service being down changes no answer.

| Moment             | Talks to               | If it fails                                       |
|--------------------|------------------------|---------------------------------------------------|
| Handling a request | nothing                | —                                                 |
| Start-up           | iam-service, over HTTP | retried in the background until accepted          |
| A role changes     | Kafka, one way         | delivered when the consumer is back, claimed once |

A service that enforces no permissions of its own declares no
`RolePermissionsSource`, and `@authz.has('…')` there refuses everything. That is
the intended answer, not an oversight: a service cannot honor a permission it
has no way to evaluate.


## The transport layer is role-based, on purpose

`api-gateway` gates routes with `hasRole('ADMIN')` rather than a permission. It owns no
role-permission projection, and getting one would make a router the owner of a decision it does not
make; asking iam instead would put a call on the request path. Before routing, the token's realm
roles are the only thing available, so that is what the gateway uses. The decision that counts is
taken by the service the request reaches.

A service does not get the same excuse: it holds a projection, so it names a permission. The
gateway's matchers therefore stop at `authenticated()` for anything a service guards — repeating the
decision there would hard-code a role in front of one already taken correctly, and lock out a
second unrestricted role that iam allows.
