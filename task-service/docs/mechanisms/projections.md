# Projections

How task-service knows about projects, categories, statuses and users it does not own.

All four are referenced by id and mirrored locally from the events that own them. That is what
lets a board render without a call to project-service per row, and keeps task listings working
when that service is down.

The trade is stated rather than hidden: **authorization reads the local membership projection**,
so a member removed a second ago may still pass one check. The alternative is a synchronous call
on every task read, coupling the availability of the two services.

`TaskPermission` is this context's own enum, deliberately not project-service's
`ProjectPermission`. Importing another context's enum would make a change over there a compile
break here.
