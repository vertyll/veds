# file-service

File metadata and signed URLs. The bytes never pass through it.

| Property       | Value                            |
|----------------|----------------------------------|
| Port           | 8088                             |
| Database       | 5439                             |
| Object storage | Garage (S3 API), bucket private  |
| Publishes      | `file-confirmed`, `file-deleted` |
| Consumes       | nothing                          |

Why the bytes stay out of this service, the three-step exchange, two-step deletion and what it leaves to
project-service are in [Files](../docs/files.md), which describes the whole flow.

## Limits live with the scope

`FileScope` carries its own cap and type list, so "how big may this be" has one answer rather
than one per calling service. Attachments accept any type deliberately — a project may need a
format nobody anticipated — while avatars and icons do not. The declared size only produces an
early error; the real limit is signed in to the URL.
