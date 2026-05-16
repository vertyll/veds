FROM hashicorp/terraform:1.7.5

RUN apk add --no-cache python3 ca-certificates bash

WORKDIR /workspace

COPY infra/kafka/main.tf    /workspace/tf/main.tf
COPY infra/kafka/variables.tf /workspace/tf/variables.tf
COPY infra/kafka/topics.tf  /workspace/tf/topics.tf

COPY contracts/ /workspace/contracts/

COPY scripts/schema_registry/register_schemas.py /workspace/register_schemas.py

COPY infra/provisioner/entrypoint.sh /usr/local/bin/provisioner-entrypoint.sh
RUN chmod +x /usr/local/bin/provisioner-entrypoint.sh

VOLUME ["/state"]
ENV TF_DATA_DIR=/state/.terraform

ENTRYPOINT ["/usr/local/bin/provisioner-entrypoint.sh"]
CMD []
