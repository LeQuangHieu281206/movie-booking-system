@echo off
cd /d "%~dp0"
where node >nul 2>nul
if errorlevel 1 (
  echo Please install Node.js LTS from https://nodejs.org/ and reopen this file.
  pause
  exit /b 1
)
node -e "const [a,b]=process.versions.node.split('.').map(Number);process.exit((a===20&&b>=19)||(a===22&&b>=12)||a>=23?0:1)"
if errorlevel 1 (
  echo Node.js version is too old. Use Node 20.19+ or 22.12+.
  pause
  exit /b 1
)
if not exist node_modules\.package-lock.json (
  call npm.cmd ci
  if errorlevel 1 (
    echo Installation failed. See the message above.
    pause
    exit /b 1
  )
)
echo Open http://localhost:5173 after Vite starts.
echo Keep this window open. Press Ctrl+C to stop.
call npm.cmd run dev
pause
