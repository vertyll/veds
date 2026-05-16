SERVICES = api-gateway iam-service mail-service template-service
SHARED = shared-infrastructure

.PHONY: build-all clean-build-all test-all clean-all format-all check-style-all \
        up down logs bootstrap register-schemas provision-topics

build-all:
	@for dir in $(SERVICES) $(SHARED); do \
		echo "Building $$dir..."; \
		(cd $$dir && ./gradlew build) || exit 1; \
	done

clean-build-all:
	@for dir in $(SERVICES) $(SHARED); do \
	    echo "Cleaning and building $$dir..."; \
	    (cd $$dir && ./gradlew clean build) || exit 1; \
	done

test-all:
	@for dir in $(SERVICES) $(SHARED); do \
		echo "Testing $$dir..."; \
		(cd $$dir && ./gradlew test) || exit 1; \
	done

clean-all:
	@for dir in $(SERVICES) $(SHARED); do \
		echo "Cleaning $$dir..."; \
		(cd $$dir && ./gradlew clean) || exit 1; \
	done

format-all:
	@for dir in $(SERVICES) $(SHARED); do \
		echo "Formatting $$dir..."; \
		(cd $$dir && ./gradlew ktlintFormat) || exit 1; \
	done

check-style-all:
	@for dir in $(SERVICES) $(SHARED); do \
		echo "Checking style $$dir..."; \
		(cd $$dir && ./gradlew ktlintCheck detekt) || exit 1; \
	done

# --- Local infra (podman-compose) ---

up:
	podman-compose up -d kafka schema-registry iam-db mail-db keycloak-db keycloak mail-dev kafka-ui
	$(MAKE) bootstrap

down:
	podman-compose down

logs:
	podman-compose logs -f --tail=200

# Provision topics (Terraform) + register Avro schemas (Python).
# Both are idempotent: re-running on a populated cluster is a no-op.
bootstrap: provision-topics register-schemas

provision-topics:
	podman-compose run --rm topics-init

register-schemas:
	podman-compose run --rm schemas-init
