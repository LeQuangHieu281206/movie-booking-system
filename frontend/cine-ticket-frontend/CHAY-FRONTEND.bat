@echo off
cd /d "%~dp0"
where node >nul 2>nul
if errorlevel 1 (
  echo Hay cai Node.js truoc khi chay frontend.
  pause
  exit /b 1
)
start "" http://localhost:5173
node server.mjs
pause
