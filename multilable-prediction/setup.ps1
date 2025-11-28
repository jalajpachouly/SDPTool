# Hybrid Java + Python setup for SDPTool
$ErrorActionPreference = "Stop"

Write-Host "`n=== Python environment ===" -ForegroundColor Cyan
if (-not (Test-Path .venv)) { python -m venv .venv }
.\.venv\Scripts\Activate.ps1
python -m pip install --upgrade pip
python -m pip install -r multilable-prediction/requirements.txt

Write-Host "`n=== Maven build ===" -ForegroundColor Cyan
mvn -DskipTests package

Write-Host "`nDone. To run:" -ForegroundColor Green
Write-Host "1) Java UI: java -jar target/SDPTool-1.0-SNAPSHOT-jar-with-dependencies.jar"
Write-Host "2) Python pipeline: python multilable-prediction/src/configurable_main.py multilable-prediction/main_config.json"
