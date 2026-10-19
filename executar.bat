@echo off
cd /d "%~dp0"
where mvn >nul 2>nul
if errorlevel 1 (
  echo Maven nao foi encontrado no PATH.
  echo Instale Java 21 e Maven 3.9 ou superior. Depois, abra um terminal nesta pasta e execute: mvn spring-boot:run
  pause
  exit /b 1
)
mvn spring-boot:run
