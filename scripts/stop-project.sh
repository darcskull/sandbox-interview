#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
APP_PID_FILE="$ROOT_DIR/.run/interview-lab.pid"

if [[ -f "$APP_PID_FILE" ]]; then
  app_pid="$(cat "$APP_PID_FILE")"
  if [[ "$app_pid" =~ ^[0-9]+$ ]]; then
    if command -v taskkill.exe >/dev/null 2>&1; then
      taskkill.exe //PID "$app_pid" //T //F >/dev/null 2>&1 || true
    elif kill -0 "$app_pid" 2>/dev/null; then
      kill "$app_pid" 2>/dev/null || true
    fi
  fi
  rm -f "$APP_PID_FILE"
else
  echo "No application PID file found; the app may already be stopped."
fi

cd "$ROOT_DIR"
if command -v docker >/dev/null 2>&1 \
  && docker info >/dev/null 2>&1 \
  && docker compose version >/dev/null 2>&1; then
  docker compose down
elif command -v podman >/dev/null 2>&1 \
  && podman info >/dev/null 2>&1 \
  && podman compose version >/dev/null 2>&1; then
  podman compose down
else
  echo "No running Docker or Podman Compose engine found; skipped stopping containers." >&2
fi

echo "Interview Lab and its local Compose containers are stopped."
