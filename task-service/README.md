# task-service

Tasks, comments and the board.

| Property  | Value                                                                                         |
|-----------|-----------------------------------------------------------------------------------------------|
| Port      | 8085                                                                                          |
| Database  | 5436                                                                                          |
| Publishes | `task-created`, `task-assigned`, `task-status-changed`, `task-archived`, `task-comment-added` |
| Consumes  | project events, `user-registered`, `user-profile-updated`, `file-deleted`                     |

## Two rules that keep the board quiet

- **A no-op status move returns the same instance.** Dragging a card back into its own column
  emits nothing, so it does not reach every watcher as a notification.
- **A removed category or status repairs the tasks that referenced it**, in the same
  transaction. Left behind, the reference renders as a label nobody can display.

The same applies to `file-deleted`: the attachment id is dropped, because a task keeping a
broken download surfaces days later as a bug report.

## Mechanisms

- [Board query](docs/mechanisms/board-query.md) – How one board page is read in a fixed number of statements.
- [Projections](docs/mechanisms/projections.md) – How task-service knows about projects, categories, statuses and users
  it does not own.
