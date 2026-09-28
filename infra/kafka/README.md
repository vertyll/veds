# Kafka Topics IaC (OpenTofu)

This module automates the provisioning of Kafka topics (both Business and DLT) using the [Mongey/kafka](https://search.opentofu.org/provider/mongey/kafka/latest) provider.

## Requirements

| Prerequisite      | Version / Details                                                 |
|:------------------|:------------------------------------------------------------------|
| **OpenTofu**      | `>= 1.6.0`                                                        |
| **Kafka Cluster** | Accessible broker (e.g., `localhost:29092` for local development) |

## Usage

Navigate to the module directory and initialize OpenTofu to download the required providers:

```bash
cd infra/kafka
tofu init
```

Apply the configuration. By default, it uses `["localhost:29092"]` as the broker address:

```bash
tofu apply
```

> [!IMPORTANT]
> If you need to target a different environment, override the `bootstrap_servers` variable. Since it expects a `list(string)`, use the following syntax:
>
> ```bash
> tofu apply -var='bootstrap_servers=["kafka.production.internal:9092"]'
> ```

The state written by Terraform before the switch needs no migration: `tofu init` reads it as is and rewrites the
provider address to `registry.opentofu.org` on the next apply.
