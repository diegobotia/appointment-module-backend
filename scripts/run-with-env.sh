#!/usr/bin/env bash
# Loads .env (if present) and runs the Spring Boot app via Maven.
set -e
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"

if [ -f "$ROOT_DIR/.env" ]; then
  echo "Loading environment from .env"
  set -a
  # shellcheck source=/dev/null
  . "$ROOT_DIR/.env"
  set +a
else
  echo "Warning: .env not found in project root. Relying on environment variables."
fi

SPRING_PROFILES="${SPRING_PROFILES_ACTIVE:-default}"
echo "Active Spring profile: $SPRING_PROFILES"
exec mvn -f "$ROOT_DIR/pom.xml" spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=$SPRING_PROFILES"
