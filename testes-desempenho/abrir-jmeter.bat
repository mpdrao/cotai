@echo off
setlocal
echo ========================================================
echo   Iniciando Apache JMeter - Plano de Carga Web + API
echo ========================================================

set "SCRIPT_DIR=%~dp0"
set "TEST_PLAN=%SCRIPT_DIR%COTAI-Teste-Angular-Completo.jmx"

where jmeter >nul 2>nul
if %ERRORLEVEL% equ 0 (
    echo JMeter localizado no PATH. Abrindo plano de teste...
    start "" jmeter -t "%TEST_PLAN%"
    exit /b 0
)

if exist "%LOCALAPPDATA%\Programs\JMeter\bin\jmeter.bat" (
    echo JMeter localizado em %LOCALAPPDATA%\Programs\JMeter. Abrindo...
    start "" "%LOCALAPPDATA%\Programs\JMeter\bin\jmeter.bat" -t "%TEST_PLAN%"
    exit /b 0
)

if exist "C:\Ferramentas\apache-jmeter-5.6.3\bin\jmeter.bat" (
    echo JMeter localizado em C:\Ferramentas. Abrindo...
    start "" "C:\Ferramentas\apache-jmeter-5.6.3\bin\jmeter.bat" -t "%TEST_PLAN%"
    exit /b 0
)

echo [ERRO] JMeter nao foi encontrado no PATH do sistema.
echo Verifique se o JMeter esta instalado ou adicione a pasta bin ao PATH.
pause
