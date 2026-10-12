@echo off
setlocal EnableExtensions DisableDelayedExpansion
chcp 65001 >nul
title DataAgent Launcher

set "PROJECT_ROOT=%~dp0"
if "%PROJECT_ROOT:~-1%"=="\" set "PROJECT_ROOT=%PROJECT_ROOT:~0,-1%"
set "FRONTEND_DIR=%PROJECT_ROOT%\frontend"
set "BACKEND_PORT=8081"
set "FRONTEND_PORT=5173"
set "OLLAMA_PORT=11434"
set "OLLAMA_HOST=127.0.0.1:11434"
set "CHECK_ONLY=0"
set "HAS_ERROR=0"

if /I "%~1"=="--check" set "CHECK_ONLY=1"

echo ============================================================
echo   DataAgent Windows Launcher
echo ============================================================
echo [INFO] Project root: %PROJECT_ROOT%

rem Validate the project and required commands before opening any service window.
if not exist "%PROJECT_ROOT%\mvnw.cmd" (
    echo [ERROR] Maven Wrapper was not found: %PROJECT_ROOT%\mvnw.cmd
    set "HAS_ERROR=1"
)

if not exist "%FRONTEND_DIR%\package.json" (
    echo [ERROR] Frontend package.json was not found: %FRONTEND_DIR%\package.json
    set "HAS_ERROR=1"
)

where.exe java.exe >nul 2>&1
if errorlevel 1 (
    if not defined JAVA_HOME (
        echo [ERROR] Java was not found in PATH and JAVA_HOME is not configured.
        set "HAS_ERROR=1"
    ) else if not exist "%JAVA_HOME%\bin\java.exe" (
        echo [ERROR] JAVA_HOME does not contain bin\java.exe.
        set "HAS_ERROR=1"
    )
)

where.exe npm.cmd >nul 2>&1
if errorlevel 1 (
    echo [ERROR] npm was not found in PATH.
    set "HAS_ERROR=1"
)

where.exe curl.exe >nul 2>&1
if errorlevel 1 (
    echo [ERROR] curl was not found in PATH and is required to check Ollama.
    set "HAS_ERROR=1"
)

call :find_ollama
if not defined OLLAMA_EXE (
    echo [ERROR] Ollama was not found in PATH or its standard install directory.
    set "HAS_ERROR=1"
) else (
    for %%I in ("%OLLAMA_EXE%") do set "PATH=%%~dpI;%PATH%"
    echo [OK] Ollama found: %OLLAMA_EXE%
)

rem Credentials are inherited by the backend process and are never printed or saved.
if not defined DATAAGENT_DB_USERNAME (
    echo [ERROR] DATAAGENT_DB_USERNAME is not configured.
    set "HAS_ERROR=1"
)

if not defined DATAAGENT_DB_PASSWORD (
    echo [ERROR] DATAAGENT_DB_PASSWORD is not configured.
    set "HAS_ERROR=1"
)

if "%HAS_ERROR%"=="1" goto :configuration_failed

if "%CHECK_ONLY%"=="1" goto :check_only

rem This launcher always connects the backend to its local Ollama instance.
set "DATAAGENT_AI_BASE_URL=http://127.0.0.1:11434/v1"

echo.
echo [STEP] Checking Ollama...
call :ollama_ready
if errorlevel 1 (
    call :port_is_listening %OLLAMA_PORT%
    if errorlevel 1 (
        echo [START] Ollama is not running. Opening a dedicated service window...
        start "DataAgent Ollama" /D "%PROJECT_ROOT%" cmd.exe /d /k ollama.exe serve
        call :wait_for_ollama 30
        if errorlevel 1 (
            echo [ERROR] Ollama did not become ready after 30 checks.
            goto :launch_failed
        )
        echo [OK] Ollama is ready on port %OLLAMA_PORT%.
    ) else (
        echo [WARN] Port %OLLAMA_PORT% is occupied, but Ollama did not respond.
        echo [ERROR] Close the program using port %OLLAMA_PORT% and run this script again.
        goto :launch_failed
    )
) else (
    echo [SKIP] Ollama is already running.
)

call :configure_local_ai

echo.
echo [STEP] Checking Spring Boot backend port %BACKEND_PORT%...
call :port_is_listening %BACKEND_PORT%
if errorlevel 1 (
    echo [START] Opening the Spring Boot backend in a dedicated window...
    start "DataAgent Backend" /D "%PROJECT_ROOT%" cmd.exe /d /k call mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=mysql -Dspring-boot.run.arguments=--server.port=%BACKEND_PORT%
) else (
    echo [SKIP] Port %BACKEND_PORT% is already listening. Backend start skipped.
    echo [INFO] The existing backend must already have the required DB and AI environment.
)

echo.
echo [STEP] Checking Vue frontend port %FRONTEND_PORT%...
call :port_is_listening %FRONTEND_PORT%
if errorlevel 1 (
    echo [START] Opening the Vue frontend in a dedicated window...
    start "DataAgent Frontend" /D "%FRONTEND_DIR%" cmd.exe /d /k call npm.cmd run dev
) else (
    echo [SKIP] Port %FRONTEND_PORT% is already listening. Frontend start skipped.
)

echo.
echo [WAIT] Waiting for the backend on port %BACKEND_PORT%...
call :wait_for_port %BACKEND_PORT% 90
if errorlevel 1 (
    echo [ERROR] Backend is not listening after 90 seconds. Check its service window.
    goto :launch_failed
) else (
    echo [OK] Backend port %BACKEND_PORT% is ready.
)

echo [WAIT] Waiting for the frontend on port %FRONTEND_PORT%...
call :wait_for_port %FRONTEND_PORT% 60
if errorlevel 1 (
    echo [ERROR] Frontend is not listening after 60 seconds. Check its service window.
    goto :launch_failed
)

echo [OK] Frontend port %FRONTEND_PORT% is ready.
echo [OPEN] Opening http://localhost:%FRONTEND_PORT%
start "" "http://localhost:%FRONTEND_PORT%"

echo.
echo [DONE] DataAgent launch sequence completed.
echo        Closing this launcher will not stop the service windows.
pause
exit /b 0

:find_ollama
set "OLLAMA_EXE="
for /f "delims=" %%I in ('where.exe ollama.exe 2^>nul') do if not defined OLLAMA_EXE set "OLLAMA_EXE=%%I"
if not defined OLLAMA_EXE if exist "%LOCALAPPDATA%\Programs\Ollama\ollama.exe" set "OLLAMA_EXE=%LOCALAPPDATA%\Programs\Ollama\ollama.exe"
if not defined OLLAMA_EXE if exist "%ProgramFiles%\Ollama\ollama.exe" set "OLLAMA_EXE=%ProgramFiles%\Ollama\ollama.exe"
exit /b 0

:ollama_ready
curl.exe --fail --silent --max-time 2 "http://127.0.0.1:%OLLAMA_PORT%/api/tags" >nul 2>&1
exit /b %errorlevel%

:configure_local_ai
if defined DATAAGENT_AI_MODEL (
    ollama.exe show "%DATAAGENT_AI_MODEL%" >nul 2>&1
    if errorlevel 1 (
        echo [WARN] DATAAGENT_AI_MODEL is not installed in the local Ollama instance.
        set "DATAAGENT_AI_MODEL="
    )
)

if not defined DATAAGENT_AI_MODEL (
    for /f "skip=1 tokens=1" %%M in ('ollama.exe list 2^>nul') do if not defined DATAAGENT_AI_MODEL set "DATAAGENT_AI_MODEL=%%M"
)

if not defined DATAAGENT_AI_MODEL (
    echo [WARN] No Ollama model is installed and DATAAGENT_AI_MODEL is not set.
    echo [WARN] Other features will work, but AI analysis will remain unconfigured.
    exit /b 0
)

rem Ollama ignores this process-only bearer value. Never forward or store a real API key.
set "DATAAGENT_AI_API_KEY=dataagent-local-%RANDOM%-%RANDOM%"
echo [OK] A local Ollama model is configured for AI analysis.
echo [OK] The AI endpoint is configured for the local Ollama service.
exit /b 0

:port_is_listening
netstat.exe -ano -p tcp 2>nul | %SystemRoot%\System32\findstr.exe /R /C:":%~1 .*LISTENING" >nul
exit /b %errorlevel%

:wait_for_ollama
set /a "WAIT_REMAINING=%~1"
:wait_for_ollama_loop
call :ollama_ready
if not errorlevel 1 exit /b 0
if %WAIT_REMAINING% LEQ 0 exit /b 1
timeout.exe /t 1 /nobreak >nul
set /a "WAIT_REMAINING-=1"
goto :wait_for_ollama_loop

:wait_for_port
set /a "WAIT_REMAINING=%~2"
:wait_for_port_loop
call :port_is_listening %~1
if not errorlevel 1 exit /b 0
if %WAIT_REMAINING% LEQ 0 exit /b 1
timeout.exe /t 1 /nobreak >nul
set /a "WAIT_REMAINING-=1"
goto :wait_for_port_loop

:report_port
call :port_is_listening %~1
if errorlevel 1 (
    echo [CHECK] Port %~1 is available.
) else (
    echo [CHECK] Port %~1 is already listening.
)
exit /b 0

:check_only
echo.
echo [CHECK] Required files, commands, and database variables are available.
call :report_port %OLLAMA_PORT%
call :report_port %BACKEND_PORT%
call :report_port %FRONTEND_PORT%
call :ollama_ready
if errorlevel 1 (
    echo [CHECK] Ollama is installed but is not currently responding.
) else (
    echo [CHECK] Ollama is responding.
)
echo [CHECK] No service was started because --check was used.
exit /b 0

:configuration_failed
echo.
echo [FAILED] Prerequisite check failed. No service was started.
echo          Configure the missing environment variables and try again.
if "%CHECK_ONLY%"=="0" pause
exit /b 1

:launch_failed
echo.
echo [FAILED] The full DataAgent stack could not be started.
echo          Review the messages above and the open service windows.
pause
exit /b 1
