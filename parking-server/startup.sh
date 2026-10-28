#!/bin/bash
# ============================================================================
# Parking-Server 生产环境启动脚本（Linux）
# ----------------------------------------------------------------------------
# 目标并发：非高峰 1000+，高峰 10000+
# 推荐硬件：4核 8G 起步（含 MySQL/Redis 同机），生产建议 8核 16G + 独立 DB
# 若部署在 2核2G 低配机：请将 -Xms/-Xmx 降到 1g，threads.max 降到 100
# ============================================================================

# 进入脚本所在目录（确保相对路径 ./logs ./uploads 可用）
cd "$(dirname "$0")" || exit 1

# 应用信息
APP_NAME="parking-server"
JAR_FILE="target/${APP_NAME}-1.0.0.jar"
PID_FILE="${APP_NAME}.pid"
LOG_DIR="./logs"
LOG_FILE="${LOG_DIR}/startup.log"

mkdir -p "${LOG_DIR}"

# JVM 参数（按 4核8G 服务器调优）
JAVA_OPTS="-Xms2g -Xmx2g \
  -Xss512k \
  -XX:+UseG1GC \
  -XX:MaxGCPauseMillis=100 \
  -XX:G1HeapRegionSize=16m \
  -XX:+ParallelRefProcEnabled \
  -XX:+HeapDumpOnOutOfMemoryError \
  -XX:HeapDumpPath=${LOG_DIR}/heapdump.hprof \
  -Dserver.port=8080 \
  -Dspring.profiles.active=prod \
  -Dfile.encoding=UTF-8 \
  -Duser.timezone=Asia/Shanghai"

# 检查是否已运行
if [ -f "${PID_FILE}" ]; then
  PID=$(cat "${PID_FILE}")
  if kill -0 "${PID}" 2>/dev/null; then
    echo "[ERROR] ${APP_NAME} already running, PID=${PID}"
    exit 1
  else
    rm -f "${PID_FILE}"
  fi
fi

# 检查 jar 是否存在
if [ ! -f "${JAR_FILE}" ]; then
  echo "[ERROR] JAR not found: ${JAR_FILE}"
  echo "        Please run 'mvn clean package -DskipTests' first"
  exit 1
fi

# 后台启动并写入 PID
echo "[INFO] Starting ${APP_NAME}..."
nohup java ${JAVA_OPTS} -jar "${JAR_FILE}" > "${LOG_FILE}" 2>&1 &
echo $! > "${PID_FILE}"

sleep 2
if kill -0 "$(cat "${PID_FILE}")" 2>/dev/null; then
  echo "[OK] ${APP_NAME} started, PID=$(cat ${PID_FILE})"
  echo "     Log: ${LOG_FILE}"
else
  echo "[ERROR] Failed to start, check log: ${LOG_FILE}"
  exit 1
fi
