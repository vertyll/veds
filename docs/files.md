# Files

Uploads and downloads never pass through a JVM. The browser talks to object storage directly with pre-signed URLs, and
`file-service` brackets the transfer: it decides beforehand whether the upload is allowed and verifies afterward that it
happened.

## Why not stream through the service

A 25 MB attachment streamed through Spring occupies a request thread and heap for the whole transfer, and the service
has nothing to say about the contents. The only thing it can usefully decide is *whether* the transfer may happen —
which is exactly what a pre-signed URL encodes.

## The exchange

| Step | Who                  | What                                                                        |
|------|----------------------|-----------------------------------------------------------------------------|
| 1    | SPA → file-service   | `POST /files/upload-ticket` — record created `PENDING`, signed URL returned |
| 2    | SPA → object storage | `PUT` to the signed URL                                                     |
| 3    | SPA → file-service   | `POST /files/{id}/confirm` — size read back from storage                    |

Three steps by necessity. The record has to exist before step 2, or something could be written to storage that this
service never allowed. And step 3 has to verify against storage, because the client saying "done" is a claim while a
reported object size is evidence.

## The bucket is private

No public read policy, no website configuration. Nothing is reachable without a signed URL, so
"my files, served by my application" holds without exposing the store to the internet.

Two consequences worth knowing:

- **A pre-signed URL is a bearer credential.** Whoever holds it can use it until it expires, which is why downloads are
  signed for 5 minutes — a link pasted into a chat should stop working before it is forwarded far. Uploads get 15,
  enough for a slow connection to finish a large attachment.
- **Images cannot use a permanent `src`.** `SignedImageDirective` resolves a file id into a fresh link and re-requests
  once on a failed load. Binding a stale URL would leave the user looking at a broken image with no way to tell why.

## CORS is required, and is not optional

The browser `PUT`s to the store from the SPA's origin, so the bucket must allow it; the rule and why it does not open
the bucket are in [infra/garage](../infra/garage/README.md#cors-is-required).

Two interceptors are scoped to the application's own API for the same reason: `withCredentials`
would leak the session cookie to a third-party origin (and a CORS response cannot both allow credentials and use a
wildcard origin), and a custom `X-Lang` header would be added to the preflight for a header the store has no reason to
allow.

## Local storage: Garage

Locally the store is Garage. Its bootstrap, the CORS rule it applies, its version constraint and the
`response-content-disposition` caveat are in [infra/garage](../infra/garage/README.md).

`pathStyleAccessEnabled(true)` is set in `ObjectStorageConfig` because Garage does not serve virtual-host style buckets
without wildcard DNS, which a compose file does not have.

## What the back end stores

An id, never a URL and never a path.

A URL expires — a stored one is a broken link waiting to happen. A path assumes the service serves the bytes, which none
of them do. An id is the only thing that stays true, and it is what a client exchanges for a signed link at the moment
it needs one.

| Context         | Field                                        |
|-----------------|----------------------------------------------|
| iam-service     | `user.avatar_file_id`                        |
| project-service | `project.icon_file_id`                       |
| task-service    | `task_attachment`, `task_comment_attachment` |

### Keeping references honest

`file-deleted` is consumed by task-service, which removes the id from every task and comment that held it. Without that
a task keeps an attachment resolving to nothing, and the user meets it as a download that fails days later with no clue
why.

Idempotent by construction: dropping an id that is already gone is a no-op, so an at-least-once redelivery costs
nothing.

## Where the front end uses it

| Place                        | Scope             | How the image is resolved                          |
|------------------------------|-------------------|----------------------------------------------------|
| User avatar                  | `USER_AVATAR`     | `<app-image [fileId]>`                             |
| Project icon                 | `PROJECT_ICON`    | `<app-image [fileId]>`, and the project table cell |
| Task and comment attachments | `TASK_ATTACHMENT` | image preview by id; other types download on click |

Two patterns worth stating, because both are easy to get backwards:

- **Upload before save.** The avatar and the project icon are uploaded first, and only the resulting file id is
  submitted with the form. Ordered that way deliberately: a failed upload must not leave a saved record pointing at a
  file that never arrived. The reverse order would.
- **No `href` for a download.** A signed link expires while the page is open, so a link held in the DOM is a dead
  download with nothing explaining why. The link is fetched on click instead.

Both the avatar and the icon distinguish three states — a new file id, `null` to clear, and
`undefined` to leave alone. "Unchanged" and "removed" are different intentions, and collapsing them silently drops
somebody's picture.

## Deletion is two steps

A database transaction and an object store do not commit together. A delete marks the record
`DELETED` and a sweep removes the object afterward.

The order is deliberate: a crash leaks an object, which the sweep collects, instead of losing the record that names the
key — which nothing could ever collect.

The second sweep follows from the same reasoning. Between issuing a URL and the browser finishing, nothing here knows
whether the object arrived, so a record left `PENDING` past its window is an abandoned upload and is purged with its
object.

## What file-service does not decide

Whether a project member may read another member's attachment is a *project* question, and the answer lives in
project-service. file-service enforces ownership only. Duplicating the project rule here would mean two copies of an
access rule that must never disagree.
