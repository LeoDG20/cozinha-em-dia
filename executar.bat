@echo off
rem Equivalente ao CozinhaEmDia.exe: prepara o ambiente (JDK 21 e PostgreSQL) e executa o sistema.
cd /d "%~dp0"
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "scripts\preparar-e-executar.ps1" %*
