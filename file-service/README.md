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
project-service are in [Files](../docs/mechanisms/files.md), which describes the whole flow.

## Mechanisms

- [File scopes](docs/mechanisms/file-scopes.md) – How "how big may this be" has one answer per kind of file.
