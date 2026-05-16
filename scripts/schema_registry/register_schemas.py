#!/usr/bin/env python3
"""Register Avro schemas to Schema Registry and enforce compatibility mode.

By default sets per-subject compatibility to BACKWARD (consumers can read old data
written with the previous schema version). Use --compatibility to override
(NONE / BACKWARD / BACKWARD_TRANSITIVE / FORWARD / FORWARD_TRANSITIVE / FULL / FULL_TRANSITIVE).
"""
import argparse
import json
from pathlib import Path
from urllib import error, request

CONTENT_TYPE = "application/vnd.schemaregistry.v1+json"


def _put_subject_compatibility(registry_url: str, subject: str, level: str) -> None:
    body = json.dumps({"compatibility": level}).encode("utf-8")
    url = f"{registry_url.rstrip('/')}/config/{subject}"
    req = request.Request(url, data=body, method="PUT", headers={"Content-Type": CONTENT_TYPE})
    with request.urlopen(req, timeout=15) as response:
        payload = response.read().decode("utf-8")
        print(f"Compatibility {subject} -> {level}: {payload}")


def _register_schema(registry_url: str, subject: str, schema_path: Path) -> None:
    schema_json = json.loads(schema_path.read_text(encoding="utf-8"))
    body = json.dumps({"schema": json.dumps(schema_json)}).encode("utf-8")
    url = f"{registry_url.rstrip('/')}/subjects/{subject}/versions"
    req = request.Request(url, data=body, headers={"Content-Type": CONTENT_TYPE})
    try:
        with request.urlopen(req, timeout=15) as response:
            payload = response.read().decode("utf-8")
            print(f"Registered {schema_path} -> {subject}: {payload}")
    except error.HTTPError as e:
        details = e.read().decode("utf-8", errors="replace")
        raise SystemExit(
            f"Failed to register {schema_path} -> {subject}: HTTP {e.code} {details}"
        ) from e


def main() -> int:
    parser = argparse.ArgumentParser(description="Register Avro schemas from contracts/ directory.")
    parser.add_argument("--schemas-dir", default="contracts", help="Root directory with Avro schemas.")
    parser.add_argument(
        "--registry-url",
        default="http://localhost:8081",
        help="Schema Registry URL (e.g., http://localhost:8081).",
    )
    parser.add_argument(
        "--compatibility",
        default="BACKWARD",
        choices=[
            "NONE",
            "BACKWARD",
            "BACKWARD_TRANSITIVE",
            "FORWARD",
            "FORWARD_TRANSITIVE",
            "FULL",
            "FULL_TRANSITIVE",
        ],
        help="Per-subject compatibility level enforced before registration.",
    )
    args = parser.parse_args()

    schemas_dir = Path(args.schemas_dir)
    if not schemas_dir.exists():
        raise SystemExit(f"Schemas directory not found: {schemas_dir}")

    for schema_path in sorted(schemas_dir.rglob("*.avsc")):
        parts = schema_path.parts
        try:
            topic = parts[parts.index("contracts") + 2]
        except (ValueError, IndexError):
            raise SystemExit(f"Unexpected schema path layout: {schema_path}")

        subject = f"{topic}-value"
        # Set compatibility BEFORE registering so the very first version
        # is also validated against the chosen rules going forward.
        _put_subject_compatibility(args.registry_url, subject, args.compatibility)
        _register_schema(args.registry_url, subject, schema_path)

    return 0


if __name__ == "__main__":
    raise SystemExit(main())
