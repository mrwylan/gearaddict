#!/usr/bin/env bash
cd "$(dirname "$0")"
echo "Starting GearAddict mockup server on http://localhost:8080"
echo "Press Ctrl+C to stop."
python3 -m http.server 8080
