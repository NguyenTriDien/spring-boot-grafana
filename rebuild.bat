@echo off
setlocal enabledelayedexpansion

echo 🔨 Building Spring Boot app...
call mvn clean package -DskipTests
if errorlevel 1 exit /b 1

echo 🛑 Stopping containers...
call docker compose down
if errorlevel 1 exit /b 1

echo 🏗️ Rebuilding containers...
call docker compose up --build -d
if errorlevel 1 exit /b 1

echo ✅ Done!
