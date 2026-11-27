@echo off
setlocal
echo ========================================================
echo   Iniciando Backend Spring Boot (API - Porta 8080)
echo ========================================================

cd /d "%~dp0"
call .\mvnw.cmd -DskipWeb spring-boot:run
