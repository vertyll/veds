# Kafka Topics IaC (Terraform)

This module provisions Kafka topics using the open-source Terraform Apache Kafka provider.

## Requirements

- Terraform >= 1.6
- Access to the Apache Kafka broker (e.g., `localhost:29092` for local development)

## Usage

```
cd infra/kafka
terraform init
terraform apply -var="bootstrap_servers=localhost:29092"
```
