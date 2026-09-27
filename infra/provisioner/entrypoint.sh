#!/bin/bash
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

echo
echo "==> [1/2] Provisioning Kafka topics via Terraform"

# Convert "host:9092,host2:9092" -> ["host:9092","host2:9092"] for list(string).
TF_VAR_bootstrap_servers="$(python3 -c "import json,os; print(json.dumps([s.strip() for s in os.environ['KAFKA_BOOTSTRAP_SERVERS'].split(',') if s.strip()]))")"
export TF_VAR_bootstrap_servers

# Broker TLS (SSL listener). KAFKA_TLS_ENABLED=true + KAFKA_CA_CERT_FILE point
# the provider at the cluster's internal CA (see infra/kafka/main.tf).
export TF_VAR_tls_enabled="${KAFKA_TLS_ENABLED:-false}"
export TF_VAR_ca_cert_file="${KAFKA_CA_CERT_FILE:-}"

cd /workspace/tf
terraform init -input=false

# Adopt topics the broker already has but the state does not know about. The
# two diverge whenever topics are deleted or created outside this run - a broker
# reset leaves the state listing topics that are gone, and any topic created
# outside Terraform is invisible to it. A plan that says "create" for a topic the
# broker still holds fails, and one failure fails the whole apply, so every
# planned creation is offered to `import` first. Import of a topic that really is
# absent fails harmlessly; apply then creates it.
terraform plan -input=false -out=/state/plan.tfplan

terraform show -json /state/plan.tfplan | python3 -c '
import json, sys
for change in json.load(sys.stdin).get("resource_changes", []):
    if change["type"] == "kafka_topic" and "create" in change["change"]["actions"]:
        print(change["address"], change["change"]["after"]["name"])
' | while read -r addr name; do
    echo "  adopting pre-existing topic ${name} (${addr})"
    terraform import -input=false "$addr" "$name" >/dev/null 2>&1 || true
done

rm -f /state/plan.tfplan

terraform apply -auto-approve -input=false

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
