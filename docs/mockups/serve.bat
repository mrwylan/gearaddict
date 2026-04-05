@echo off
echo Starting GearAddict mockup server on http://localhost:8080
echo Press Ctrl+C to stop.
echo.
cd /d "%~dp0"
start "" "http://localhost:8080"
python -m http.server 8080
pause
