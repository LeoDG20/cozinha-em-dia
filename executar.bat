@echo off
cd /d "%~dp0"

echo Iniciando Cozinha em Dia...
echo Na primeira execucao, o Maven Wrapper baixa o Maven e as dependencias (precisa de internet).
echo Quando aparecer "Started CozinhaEmDiaApplication", abra http://localhost:8081
echo.
call mvnw.cmd spring-boot:run

pause
