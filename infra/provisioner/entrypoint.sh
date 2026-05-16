#!/bin/bash
# =============================================================================
# veds-provisioner entrypoint
# =============================================================================
set -euo pipefail

: "${KAFKA_BOOTSTRAP_SERVERS:?KAFKA_BOOTSTRAP_SERVERS is required}"
: "${SCHEMA_REGISTRY_URL:?SCHEMA_REGISTRY_URL is required}"
SCHEMA_COMPATIBILITY="${SCHEMA_COMPATIBILITY:-BACKWARD}"

echo "=========================================="
echo "VEDS provisioner"
echo "  KAFKA_BOOTSTRAP_SERVERS = ${KAFKA_BOOTSTRAP_SERVERS}"
echo "  SCHEMA_REGISTRY_URL     = ${SCHEMA_REGISTRY_URL}"
echo "  SCHEMA_COMPATIBILITY    = ${SCHEMA_COMPATIBILITY}"
echo "=========================================="

# ----------------------------------------------------------------------------
# Step 1: Terraform - Kafka topics
# ----------------------------------------------------------------------------
echo
echo "==> [1/2] Provisioning Kafka topics via Terraform"

# Convert "host:9092,host2:9092" -> ["host:9092","host2:9092"] for list(string).
TF_VAR_bootstrap_servers="$(python3 -c "import json,os; print(json.dumps([s.strip() for s in os.environ['KAFKA_BOOTSTRAP_SERVERS'].split(',') if s.strip()]))")"
export TF_VAR_bootstrap_servers

cd /workspace/tf
terraform init -input=false
terraform apply -auto-approve -input=false

# ----------------------------------------------------------------------------
# Step 2: Schema Registry - Avro contracts
# ----------------------------------------------------------------------------
echo
echo "==> [2/2] Registering Avro schemas in Schema Registry"

cd /workspace
python3 register_schemas.py \
    --schemas-dir /workspace/contracts \
    --registry-url "${SCHEMA_REGISTRY_URL}" \
    --compatibility "${SCHEMA_COMPATIBILITY}"

echo
echo "=========================================="
echo "OK - Provisioning complete"
echo "=========================================="
