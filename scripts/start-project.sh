#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
PROFILE="${1:-standalone}"
APP_PID_FILE="$ROOT_DIR/.run/interview-lab.pid"
APP_LOG_FILE="$ROOT_DIR/.run/interview-lab.log"
APP_URL="http://localhost:8080/"

case "$PROFILE" in
  h2|postgres|standalone) ;;
  *) echo "Usage: $0 [h2|postgres|standalone]" >&2; exit 2 ;;
esac

for command in java mvn curl; do
  if ! command -v "$command" >/dev/null 2>&1; then
    echo "Required command not found: $command" >&2
    exit 1
  fi
done

COMPOSE_COMMAND=()
if [[ "$PROFILE" != "standalone" ]]; then
  if command -v docker >/dev/null 2>&1 \
    && docker info >/dev/null 2>&1 \
    && docker compose version >/dev/null 2>&1; then
    COMPOSE_COMMAND=(docker compose)
  elif command -v podman >/dev/null 2>&1 \
    && podman info >/dev/null 2>&1 \
    && podman compose version >/dev/null 2>&1; then
    COMPOSE_COMMAND=(podman compose)
  else
    echo "No running Docker or Podman Compose engine was found." >&2
    echo "Use the 'standalone' profile to run the home page and H2 without containers." >&2
    exit 1
  fi
fi

cd "$ROOT_DIR"
mkdir -p "$ROOT_DIR/.run"

if [[ -f "$APP_PID_FILE" ]] && kill -0 "$(cat "$APP_PID_FILE")" 2>/dev/null; then
  echo "Interview Lab is already running (PID $(cat "$APP_PID_FILE"))."
else
  if [[ "$PROFILE" == "standalone" ]]; then
    echo "Standalone mode: skipping MongoDB, Kafka, RabbitMQ, and PostgreSQL."
  else
    echo "Starting MongoDB, Kafka, RabbitMQ, and PostgreSQL containers..."
    "${COMPOSE_COMMAND[@]}" up -d --wait
  fi

  echo "Packaging the Java 27 application..."
  mvn --batch-mode -DskipTests package

  echo "Starting the application with profile '$PROFILE'..."
  if [[ "$PROFILE" == "postgres" ]]; then
    nohup java -jar target/interview-lab-0.1.0-SNAPSHOT.jar --spring.profiles.active=postgres \
      >"$APP_LOG_FILE" 2>&1 &
  elif [[ "$PROFILE" == "h2" ]]; then
    nohup java -jar target/interview-lab-0.1.0-SNAPSHOT.jar --spring.profiles.active=h2 \
      >"$APP_LOG_FILE" 2>&1 &
  elif [[ "$PROFILE" == "standalone" ]]; then
    nohup java -jar target/interview-lab-0.1.0-SNAPSHOT.jar --spring.profiles.active=standalone \
      >"$APP_LOG_FILE" 2>&1 &
  else
    nohup java -jar target/interview-lab-0.1.0-SNAPSHOT.jar >"$APP_LOG_FILE" 2>&1 &
  fi
  echo "$!" > "$APP_PID_FILE"

  ready=false
  for attempt in {1..60}; do
    if curl --silent --fail "$APP_URL" >/dev/null; then
      ready=true
      break
    fi
    if ! kill -0 "$(cat "$APP_PID_FILE")" 2>/dev/null; then
      echo "The application stopped during startup. Check $APP_LOG_FILE" >&2
      exit 1
    fi
    sleep 2
  done
  if [[ "$ready" != true ]]; then
    echo "The application did not become ready. Check $APP_LOG_FILE" >&2
    exit 1
  fi
fi

echo "Opening $APP_URL"
if command -v cmd.exe >/dev/null 2>&1; then
  cmd.exe /c start "" "$APP_URL" >/dev/null 2>&1 || true
elif command -v xdg-open >/dev/null 2>&1; then
  xdg-open "$APP_URL" >/dev/null 2>&1 &
elif command -v open >/dev/null 2>&1; then
  open "$APP_URL" >/dev/null 2>&1 &
else
  echo "Open this address in your browser: $APP_URL"
fi

echo "Interview Lab is running in the background. Logs: $APP_LOG_FILE"
echo "Stop the app and local containers with: ./scripts/stop-project.sh"
