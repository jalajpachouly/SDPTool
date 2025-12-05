@echo off
REM Activate virtual environment and run Python script
call "%~dp0venv\Scripts\activate.bat"
python %*
