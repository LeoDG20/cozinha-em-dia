# Prepara o computador e executa o Cozinha em Dia.
#
# Etapas:
#   1. verifica o JDK 21 e o instala, se necessario;
#   2. verifica o Docker Desktop, instala se necessario e aguarda o motor iniciar;
#   3. inicia o banco PostgreSQL (docker compose);
#   4. inicia a aplicacao com o Maven Wrapper e abre o navegador.
#
# Uso: powershell -ExecutionPolicy Bypass -File scripts\preparar-e-executar.ps1
# Com -SomenteVerificar, apenas informa o que esta instalado, sem instalar nem executar nada.
#
# Este arquivo contem apenas caracteres ASCII, para funcionar no Windows PowerShell 5.1.

param([switch]$SomenteVerificar)

$ErrorActionPreference = 'Stop'
$raiz = Split-Path -Parent $PSScriptRoot
Set-Location $raiz

$urlJdk = 'https://aka.ms/download-jdk/microsoft-jdk-21-windows-x64.msi'
$urlDocker = 'https://desktop.docker.com/win/main/amd64/Docker%20Desktop%20Installer.exe'
$caminhoDockerDesktop = Join-Path $env:ProgramFiles 'Docker\Docker\Docker Desktop.exe'
$pastaDockerBin = Join-Path $env:ProgramFiles 'Docker\Docker\resources\bin'

function Escrever($texto) { Write-Host $texto }
function Titulo($texto) { Write-Host ''; Write-Host "== $texto" -ForegroundColor Cyan }

function Encerrar($codigo) {
    Write-Host ''
    Write-Host 'Pressione ENTER para fechar.'
    [void][Console]::ReadLine()
    exit $codigo
}

function Falhar($mensagem) {
    Write-Host ''
    Write-Host "ERRO: $mensagem" -ForegroundColor Red
    Encerrar 1
}

function Atualizar-Path {
    $maquina = [Environment]::GetEnvironmentVariable('Path', 'Machine')
    $usuario = [Environment]::GetEnvironmentVariable('Path', 'User')
    $env:Path = "$maquina;$usuario"
}

function Tem-Comando($nome) { return [bool](Get-Command $nome -ErrorAction SilentlyContinue) }

function Baixar($url, $destino) {
    Escrever "Baixando $url"
    [Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
    $ProgressPreference = 'SilentlyContinue'
    Invoke-WebRequest -Uri $url -OutFile $destino -UseBasicParsing
}

# ---------------------------------------------------------------- Java

function Versao-Java($executavel) {
    try {
        $saida = (cmd /c "`"$executavel`" -version 2>&1") | Out-String
        if ($saida -match 'version "(\d+)') { return [int]$Matches[1] }
    } catch { }
    return 0
}

function Localizar-Jdk21 {
    $candidatos = @()
    if ($env:JAVA_HOME) { $candidatos += $env:JAVA_HOME }
    foreach ($base in @("$env:ProgramFiles\Microsoft", "$env:ProgramFiles\Eclipse Adoptium", "$env:ProgramFiles\Java", "$env:ProgramFiles\Amazon Corretto", "$env:ProgramFiles\Zulu")) {
        if (Test-Path $base) {
            $candidatos += (Get-ChildItem $base -Directory -Filter '*jdk*' -ErrorAction SilentlyContinue | Sort-Object Name -Descending | ForEach-Object { $_.FullName })
        }
    }
    foreach ($pasta in $candidatos) {
        $java = Join-Path $pasta 'bin\java.exe'
        if ((Test-Path $java) -and ((Versao-Java $java) -ge 21)) { return $pasta }
    }
    return $null
}

function Java-Pronto {
    # 1) JDK 21 ou superior no PATH, sem JAVA_HOME contraditorio
    if ((Tem-Comando 'java') -and ((Versao-Java 'java') -ge 21)) {
        if ($env:JAVA_HOME -and -not ((Test-Path "$env:JAVA_HOME\bin\java.exe") -and ((Versao-Java "$env:JAVA_HOME\bin\java.exe") -ge 21))) {
            Remove-Item Env:JAVA_HOME -ErrorAction SilentlyContinue
        }
        return $true
    }
    # 2) JDK 21 instalado em pasta conhecida, mas fora do PATH
    $pasta = Localizar-Jdk21
    if ($pasta) {
        $env:JAVA_HOME = $pasta
        $env:Path = "$pasta\bin;$env:Path"
        return $true
    }
    return $false
}

function Instalar-Java {
    Escrever 'JDK 21 nao encontrado. Iniciando a instalacao (o Windows pode pedir permissao de administrador).'
    $instalado = $false
    if (Tem-Comando 'winget') {
        & winget install -e --id Microsoft.OpenJDK.21 --accept-package-agreements --accept-source-agreements
        $instalado = ($LASTEXITCODE -eq 0)
    }
    if (-not $instalado) {
        $msi = Join-Path $env:TEMP 'jdk21.msi'
        Baixar $urlJdk $msi
        $p = Start-Process msiexec.exe -ArgumentList "/i `"$msi`" /qn ADDLOCAL=FeatureMain,FeatureEnvironment,FeatureJavaHome" -Verb RunAs -Wait -PassThru
        $instalado = ($p.ExitCode -eq 0 -or $p.ExitCode -eq 3010)
    }
    Atualizar-Path
    if (-not (Java-Pronto)) { Falhar 'Nao foi possivel instalar o JDK 21. Instale-o manualmente (https://learn.microsoft.com/java/openjdk/download) e execute novamente.' }
}

# ---------------------------------------------------------------- Docker

function Docker-Instalado {
    if (Tem-Comando 'docker') { return $true }
    if (Test-Path (Join-Path $pastaDockerBin 'docker.exe')) {
        $env:Path = "$pastaDockerBin;$env:Path"
        return $true
    }
    return $false
}

function Docker-Ativo {
    try { cmd /c 'docker info >nul 2>&1'; return ($LASTEXITCODE -eq 0) } catch { return $false }
}

function Instalar-Docker {
    Escrever 'Docker nao encontrado. Iniciando a instalacao do Docker Desktop (pode levar alguns minutos).'
    $instalado = $false
    if (Tem-Comando 'winget') {
        & winget install -e --id Docker.DockerDesktop --accept-package-agreements --accept-source-agreements
        $instalado = ($LASTEXITCODE -eq 0)
    }
    if (-not $instalado) {
        $exe = Join-Path $env:TEMP 'DockerDesktopInstaller.exe'
        Baixar $urlDocker $exe
        $p = Start-Process $exe -ArgumentList 'install --quiet --accept-license' -Verb RunAs -Wait -PassThru
        $instalado = ($p.ExitCode -eq 0)
    }
    Atualizar-Path
    if (-not (Docker-Instalado)) {
        Falhar 'A instalacao do Docker Desktop nao foi concluida. Reinicie o computador e execute novamente, ou instale-o manualmente em https://www.docker.com/products/docker-desktop'
    }
}

function Aguardar-Docker {
    if (Docker-Ativo) { return }
    if (-not (Test-Path $caminhoDockerDesktop)) { Falhar 'Docker Desktop nao encontrado em Program Files.' }
    Escrever 'Iniciando o Docker Desktop. Na primeira vez, aceite os termos de uso se a janela do Docker pedir.'
    Start-Process $caminhoDockerDesktop
    for ($i = 0; $i -lt 90; $i++) {
        Start-Sleep -Seconds 4
        if (Docker-Ativo) { Escrever 'Docker pronto.'; return }
        if ($i % 5 -eq 4) { Escrever 'Aguardando o Docker iniciar...' }
    }
    Falhar 'O Docker nao ficou pronto em 6 minutos. Se acabou de ser instalado, reinicie o computador (e, se solicitado, finalize a configuracao do WSL 2) e execute novamente.'
}

# ---------------------------------------------------------------- Execucao

Titulo 'Cozinha em Dia: preparacao do ambiente'

if ($SomenteVerificar) {
    Escrever ('JDK 21 ou superior : ' + $(if (Java-Pronto) { 'OK' } else { 'nao encontrado (sera instalado)' }))
    $dockerOk = Docker-Instalado
    Escrever ('Docker instalado    : ' + $(if ($dockerOk) { 'OK' } else { 'nao encontrado (sera instalado)' }))
    if ($dockerOk) { Escrever ('Docker em execucao  : ' + $(if (Docker-Ativo) { 'sim' } else { 'nao (sera iniciado)' })) }
    Escrever ('winget disponivel   : ' + $(if (Tem-Comando 'winget') { 'sim' } else { 'nao (sera usado download direto)' }))
    Escrever ('Raiz do projeto     : ' + $raiz)
    exit 0
}

Titulo '1/4  Java (JDK 21)'
if (Java-Pronto) { Escrever 'JDK 21 ou superior encontrado.' } else { Instalar-Java; Escrever 'JDK 21 instalado.' }

Titulo '2/4  Docker'
if (-not (Docker-Instalado)) { Instalar-Docker }
Aguardar-Docker

Titulo '3/4  Banco de dados PostgreSQL'
& docker compose up -d --wait
if ($LASTEXITCODE -ne 0) {
    Falhar 'Nao foi possivel iniciar o banco. Verifique se a porta 5432 esta livre (outro PostgreSQL em execucao?) e se o Docker Desktop esta aberto.'
}
Escrever 'Banco de dados em execucao.'

Titulo '4/4  Aplicacao'
Escrever 'Na primeira execucao, o Maven Wrapper baixa as dependencias (pode levar alguns minutos).'
Escrever 'O navegador abrira em http://localhost:8081 quando a aplicacao estiver pronta.'
Escrever 'Para encerrar, pressione Ctrl+C ou feche esta janela.'
Escrever ''

Start-Job -ScriptBlock {
    for ($i = 0; $i -lt 180; $i++) {
        try {
            $r = Invoke-WebRequest 'http://localhost:8081/login' -UseBasicParsing -TimeoutSec 2
            if ($r.StatusCode -eq 200) { Start-Process 'http://localhost:8081'; break }
        } catch { }
        Start-Sleep -Seconds 2
    }
} | Out-Null

& (Join-Path $raiz 'mvnw.cmd') spring-boot:run
$codigo = $LASTEXITCODE

Escrever ''
Escrever 'A aplicacao foi encerrada. O banco continua em execucao; para para-lo, use: docker compose stop'
Encerrar $codigo
