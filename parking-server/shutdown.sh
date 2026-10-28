#!/bin/bash
# ============================================================================
# Parking-Server 生产环境停止脚本（Linux）
# 优雅停止：先发 SIGTERM，等待 30s，仍未退出则 SIGKILL
# ============================================================================

cd "$(dirname "$0")" || exit 1

APP_NAME="parking-server"
PID_FILE="${APP_NAME}.pid"

if [ ! -f "${PID_FILE}" ]; then
  echo "[WARN] PID file not found: ${PID_FILE}, process may not be running"
  exit 0
fi

PID=$(cat "${PID_FILE}")
if ! kill -0 "${PID}" 2>/dev/null; then
  echo "[INFO] Process ${PID} not running, cleaning PID file"
  rm -f "${PID_FILE}"
  exit 0
fi

echo "[INFO] Sending SIGTERM to ${APP_NAME} PID=${PID}..."
kill -15 "${PID}"

for i in $(seq 1 30); do
  if ! kill -0 "${PID}" 2>/dev/null; then
    echo "[OK] ${APP_NAME} stopped gracefully (after ${i}s)"
    rm -f "${PID_FILE}"
    exit 0
  fi
  sleep 1
done

echo "[WARN] Graceful shutdown timeout, sending SIGKILL..."
kill -9 "${PID}" 2>/dev/null
rm -f "${PID_FILE}"
echo "[OK] ${APP_NAME} force killed"
