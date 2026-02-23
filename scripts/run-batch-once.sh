#!/usr/bin/env bash
set -euo pipefail

# One-time batch execution for production/deploy environments.

if [[ -f ".env" ]]; then
  # shellcheck disable=SC1091
  source ".env"
fi

missing=()
[[ -z "${SPRING_DATASOURCE_URL:-}" ]] && missing+=("SPRING_DATASOURCE_URL")
[[ -z "${SPRING_DATASOURCE_USERNAME:-}" ]] && missing+=("SPRING_DATASOURCE_USERNAME")
[[ -z "${SPRING_DATASOURCE_PASSWORD:-}" ]] && missing+=("SPRING_DATASOURCE_PASSWORD")
[[ -z "${API_SERVICE_KEY:-}" ]] && missing+=("API_SERVICE_KEY")

if (( ${#missing[@]} > 0 )); then
  echo "Missing required env vars: ${missing[*]}" >&2
  exit 1
fi

RUN_DATE="${RUN_DATE:-$(date +%F)}"
TIMESTAMP="${TIMESTAMP:-$(date +%s)}"
APP_JAR="${APP_JAR:-build/libs/budong-batch-0.0.1-SNAPSHOT.jar}"

if [[ ! -f "$APP_JAR" ]]; then
  echo "Jar not found: $APP_JAR" >&2
  echo "Build it first (e.g., ./gradlew bootJar) or set APP_JAR." >&2
  exit 1
fi

SPRING_BATCH_JOB_ENABLED=true \
java -jar "$APP_JAR" \
  --spring.batch.job.name=dealPipelineJob \
  --runDate="$RUN_DATE" \
  --timestamp="$TIMESTAMP"
