@echo off
REM ============================================================================
REM Parking-Server 启动脚本（Windows，开发/测试用）
REM 生产环境请使用 startup.sh 或直接 java -jar 启动
REM ============================================================================

cd /d "%~dp0"

set APP_NAME=parking-server
set JAR_FILE=target\%APP_NAME%-1.0.0.jar
set LOG_DIR=.\logs
set LOG_FILE=%LOG_DIR%\startup.log

if not exist "%LOG_DIR%" mkdir "%LOG_DIR%"

REM JVM 参数（开发环境，1G 堆；生产请用 startup.sh 的 2G 堆）
set JAVA_OPTS=-Xms1g -Xmx1g -Xss512k -XX:+UseG1GC -XX:MaxGCPauseMillis=100 -Dspring.profiles.active=prod -Dfile.encoding=UTF-8 -Duser.timezone=Asia/Shanghai

if not exist "%JAR_FILE%" (
  echo [ERROR] JAR not found: %JAR_FILE%
  echo         Please run 'mvn clean package -DskipTests' first
  exit /b 1
)

echo [INFO] Starting %APP_NAME%...
echo        Log: %LOG_FILE%
start "parking-server" /MIN cmd /c "java %JAVA_OPTS% -jar %JAR_FILE% > %LOG_FILE% 2>&1"

echo [OK] %APP_NAME% started in background
echo     Use 'taskkill /FI "WindowTitle eq parking-server"' to stop
