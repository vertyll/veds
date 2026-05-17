# Kafka Topics IaC (Terraform)

This module automates the provisioning of Kafka topics (both Business and DLT) using the [Mongey/kafka](https://registry.terraform.io/providers/Mongey/kafka/latest) Terraform provider.

## Requirements

| Prerequisite      | Version / Details                                                 |
|:------------------|:------------------------------------------------------------------|
| **Terraform**     | `>= 1.6.0`                                                        |
| **Kafka Cluster** | Accessible broker (e.g., `localhost:29092` for local development) |

## Usage

Navigate to the module directory and initialize Terraform to download the required providers:

```bash
cd infra/kafka
terraform init
```

Apply the configuration. By default, it uses `["localhost:29092"]` as the broker address:

```bash
terraform apply
```

> **Note on overriding variables:**  
> If you need to target a different environment, override the `bootstrap_servers` variable. Since it expects a `list(string)`, use the following syntax:
>
> ```bash
> terraform apply -var='bootstrap_servers=["kafka.production.internal:9092"]'
> ```
