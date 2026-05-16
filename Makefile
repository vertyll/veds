# =============================================================================
# VEDS - Makefile
# =============================================================================
# All real logic lives in Gradle. This file is a convenience wrapper for CLI /
# CI users who prefer `make X` over `./gradlew X`. In IntelliJ use the run
# configurations under `.run/` instead.
#
# Common targets:
#   make up                 # start infra (Kafka, Schema Registry, DBs, Keycloak)
#   make down               # stop infra
#   make bootstrap          # provision Kafka topics + register Avro schemas
#   make build              # build everything (all included builds)
#   make test               # run all tests
#   make format             # ktlint format
#   make check              # ktlint + detekt + tests
#   make docs               # generate Dokka HTML for shared-infrastructure
# =============================================================================

GRADLE := ./gradlew

.PHONY: build clean test format check up down logs bootstrap \
        provision-topics register-schemas docs

# --- JVM ---------------------------------------------------------------------
build:            ; $(GRADLE) build
clean:            ; $(GRADLE) clean
test:             ; $(GRADLE) test
format:           ; $(GRADLE) ktlintFormat
check:            ; $(GRADLE) check
docs:             ; $(GRADLE) docs

# --- Local infra (podman compose under the hood) -----------------------------
up:               ; $(GRADLE) infraUp bootstrap
down:             ; $(GRADLE) infraDown
logs:             ; $(GRADLE) infraLogs --console=plain
bootstrap:        ; $(GRADLE) bootstrap
provision-topics: ; $(GRADLE) provisionTopics
register-schemas: ; $(GRADLE) registerSchemas
