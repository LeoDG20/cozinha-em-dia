@echo off
cd /d "%~dp0"

where docker >nul 2>nul
if errorlevel 1 (
  echo Docker nao encontrado. Sera usado o PostgreSQL ja instalado em localhost:5432.
  echo Consulte o README para criar o banco cozinha_em_dia e o usuario cozinha.
) else (
  echo Iniciando o banco PostgreSQL com Docker Compose...
  docker compose up -d --wait
  if errorlevel 1 (
    echo Nao foi possivel iniciar o banco. Verifique se o Docker Desktop esta aberto.
    pause
    exit /b 1
  )
)

echo.
echo Iniciando Cozinha em Dia. Na primeira execucao o Maven Wrapper baixa as dependencias.
echo Quando aparecer "Started CozinhaEmDiaApplication", acesse http://localhost:8081
echo.
call mvnw.cmd spring-boot:run

pause
