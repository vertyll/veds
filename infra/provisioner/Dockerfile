ARG OPENTOFU_IMAGE=ghcr.io/opentofu/opentofu:1.12.6-minimal
ARG ALPINE_IMAGE=docker.io/library/alpine:3.24

FROM ${OPENTOFU_IMAGE} AS tofu

FROM ${ALPINE_IMAGE} AS runtime
RUN apk add --no-cache python3 ca-certificates bash
COPY --from=tofu /usr/local/bin/tofu /usr/local/bin/tofu

WORKDIR /workspace

COPY infra/kafka/main.tf /workspace/tf/main.tf
COPY infra/kafka/variables.tf /workspace/tf/variables.tf
COPY infra/kafka/topics.tf /workspace/tf/topics.tf
COPY infra/kafka/.terraform.lock.hcl /workspace/tf/.terraform.lock.hcl

COPY contracts/ /workspace/contracts/

COPY scripts/schema_registry/register_schemas.py /workspace/register_schemas.py

COPY infra/provisioner/entrypoint.sh /usr/local/bin/provisioner-entrypoint.sh
RUN chmod +x /usr/local/bin/provisioner-entrypoint.sh

VOLUME ["/state"]
ENV TF_DATA_DIR=/state/.terraform

ENTRYPOINT ["/usr/local/bin/provisioner-entrypoint.sh"]
