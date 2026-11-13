@echo off
rem Arranque con doble clic en Windows: delega en iniciar.ps1 sin cambiar la política de ejecución del sistema.
rem   iniciar.cmd          interfaz gráfica
rem   iniciar.cmd cli      consola interactiva
rem   iniciar.cmd db       solo la base de datos
chcp 65001 >nul
set "MODO=%~1"
if "%MODO%"=="" set "MODO=gui"
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0iniciar.ps1" -Modo %MODO%
if errorlevel 1 pause
