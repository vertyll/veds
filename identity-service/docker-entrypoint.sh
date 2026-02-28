#!/bin/bash
set -e

echo "=========================================="
echo "Starting Service..."
echo "=========================================="

# Check if DB_URL is required (API Gateway might not need it)
if [ -n "$DB_URL" ]; then
    # Extract database host and port from JDBC URL
    # Format: jdbc:postgresql://host:port/database
    DB_HOST=$(echo "$DB_URL" | sed -n 's|.*://\([^:]*\):.*|\1|p')
    DB_PORT=$(echo "$DB_URL" | sed -n 's|.*://[^:]*:\([0-9]*\)/.*|\1|p')

    if [ -z "$DB_HOST" ] || [ -z "$DB_PORT" ]; then
        echo "Error: Could not extract database host or port from DB_URL"
        echo "DB_URL format should be: jdbc:postgresql://host:port/database"
        exit 1
    fi

    echo "Database configuration:"
    echo "  Host: $DB_HOST"
    echo "  Port: $DB_PORT"

    # Wait for database
    echo "Waiting for database at $DB_HOST:$DB_PORT..."
    TIMEOUT=60
    ELAPSED=0

    while ! nc -z "$DB_HOST" "$DB_PORT" 2>/dev/null; do
        if [ $ELAPSED -ge $TIMEOUT ]; then
            echo "Error: Database connection timeout after ${TIMEOUT}s"
            exit 1
        fi
        echo "Database is unavailable - sleeping (${ELAPSED}/${TIMEOUT}s)"
        sleep 2
        ELAPSED=$((ELAPSED + 2))
    done

    echo "✓ Database is up!"
else
    echo "Skipping database wait (DB_URL not set)"
fi

echo "=========================================="
echo "Starting Spring Boot application..."
echo "=========================================="

# Execute the main command
exec java $JAVA_OPTS -jar app.jar
