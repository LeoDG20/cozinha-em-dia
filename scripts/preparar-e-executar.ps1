# Prepara o computador e executa o Cozinha em Dia.
#
# Etapas:
#   1. verifica o JDK 21 e o instala, se necessario;
#   2. verifica o PostgreSQL, instala se necessario e cria o usuario e o banco da aplicacao;
#   3. inicia a aplicacao com o Maven Wrapper e abre o navegador.
#
# Uso: powershell -ExecutionPolicy Bypass -File scripts\preparar-e-executar.ps1
#   -SomenteVerificar : informa o que esta instalado, sem instalar nem executar nada.
#   -SomenteBanco     : prepara o JDK e o banco de dados, mas nao inicia a aplicacao.
#
# Este arquivo contem apenas caracteres ASCII, para funcionar no Windows PowerShell 5.1.

param([switch]$SomenteVerificar, [switch]$SomenteBanco)

$ErrorActionPreference = 'Stop'
$raiz = Split-Path -Parent $PSScriptRoot
Set-Location $raiz

# Configuracao do banco (deve coincidir com os valores padrao de application.properties)
$portaBanco = 5432
$bancoNome = 'cozinha_em_dia'
$bancoUsuario = 'cozinha'
$bancoSenha = 'cozinha123'
# Senha do superusuario "postgres" definida quando este script instala o PostgreSQL
$senhaAdminPadrao = 'postgres123'

$urlJdk = 'https://aka.ms/download-jdk/microsoft-jdk-21-windows-x64.msi'
$urlPostgres = 'https://get.enterprisedb.com/postgresql/postgresql-16.9-1-windows-x64.exe'
$script:psql = $null

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

# ---------------------------------------------------------------- PostgreSQL

function Localizar-Psql {
    if (Tem-Comando 'psql') { return (Get-Command psql).Source }
    $achados = Get-ChildItem "$env:ProgramFiles\PostgreSQL\*\bin\psql.exe" -ErrorAction SilentlyContinue |
        Sort-Object { if ($_.Directory.Parent.Name -match '^\d+') { [int]$Matches[0] } else { 0 } } -Descending
    if ($achados) { return @($achados)[0].FullName }
    return $null
}

# Executa uma instrucao SQL com o psql. Retorna Ok (sucesso) e Saida (texto retornado).
function Executar-Sql($usuario, $senha, $banco, $sql) {
    $anterior = $ErrorActionPreference
    $ErrorActionPreference = 'Continue'
    $env:PGPASSWORD = $senha
    $env:PGCLIENTENCODING = 'UTF8'
    try {
        $saida = & $script:psql -h localhost -p $portaBanco -U $usuario -d $banco -w -v ON_ERROR_STOP=1 -tAc $sql 2>$null
        $ok = ($LASTEXITCODE -eq 0)
        return @{ Ok = $ok; Saida = (($saida | Out-String).Trim()) }
    } finally {
        Remove-Item Env:PGPASSWORD -ErrorAction SilentlyContinue
        $ErrorActionPreference = $anterior
    }
}

function Porta-Aberta {
    $cliente = New-Object Net.Sockets.TcpClient
    try {
        $espera = $cliente.BeginConnect('127.0.0.1', $portaBanco, $null, $null)
        if ($espera.AsyncWaitHandle.WaitOne(1000) -and $cliente.Connected) { return $true }
    } catch { } finally { $cliente.Close() }
    return $false
}

function Aguardar-Porta($segundos) {
    for ($i = 0; $i -lt $segundos; $i++) {
        if (Porta-Aberta) { return $true }
        Start-Sleep -Seconds 1
    }
    return $false
}

function Banco-Pronto {
    if (-not $script:psql) { return $false }
    $r = Executar-Sql $bancoUsuario $bancoSenha $bancoNome 'SELECT 1'
    return ($r.Ok -and $r.Saida -eq '1')
}

# Inicia o servico do PostgreSQL instalado, caso esteja parado
function Iniciar-ServicoPostgres {
    $servicos = @(Get-Service -Name 'postgresql*' -ErrorAction SilentlyContinue | Where-Object { $_.Status -ne 'Running' })
    foreach ($servico in $servicos) {
        Escrever ("Iniciando o servico " + $servico.Name + "...")
        try { Start-Service -Name $servico.Name -ErrorAction Stop }
        catch { Start-Process cmd.exe -ArgumentList "/c net start `"$($servico.Name)`"" -Verb RunAs -Wait }
        if (Aguardar-Porta 20) { return }
    }
}

function Instalar-Postgres {
    Escrever 'PostgreSQL nao encontrado. Iniciando a instalacao (o Windows pode pedir permissao de administrador).'
    $argumentos = "--mode unattended --unattendedmodeui none --superpassword $senhaAdminPadrao --serverport $portaBanco --disable-components stackbuilder"
    $instalado = $false
    if (Tem-Comando 'winget') {
        & winget install -e --id PostgreSQL.PostgreSQL.16 --accept-package-agreements --accept-source-agreements --override $argumentos
        $instalado = ($LASTEXITCODE -eq 0)
    }
    if (-not $instalado) {
        $exe = Join-Path $env:TEMP 'postgresql-instalador.exe'
        Baixar $urlPostgres $exe
        $p = Start-Process $exe -ArgumentList $argumentos -Verb RunAs -Wait -PassThru
        $instalado = ($p.ExitCode -eq 0)
    }
    Atualizar-Path
    $script:psql = Localizar-Psql
    if (-not $script:psql) {
        Falhar 'A instalacao do PostgreSQL nao foi concluida. Instale-o manualmente (https://www.postgresql.org/download/windows/) e execute novamente.'
    }
    Escrever 'Aguardando o servico do PostgreSQL iniciar...'
    if (-not (Aguardar-Porta 90)) { Iniciar-ServicoPostgres }
    if (-not (Porta-Aberta)) { Falhar 'O servico do PostgreSQL nao iniciou. Reinicie o computador e execute novamente.' }
}

function Ler-Senha {
    $segura = Read-Host -AsSecureString
    $ponteiro = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($segura)
    try { return [Runtime.InteropServices.Marshal]::PtrToStringBSTR($ponteiro) }
    finally { [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($ponteiro) }
}

function Instrucoes-Manuais {
    return @"
Crie o usuario e o banco manualmente (no psql ou no pgAdmin, como superusuario):

  CREATE USER $bancoUsuario WITH PASSWORD '$bancoSenha';
  CREATE DATABASE $bancoNome OWNER $bancoUsuario;

Depois, execute este programa novamente.
"@
}

# Cria o usuario e o banco da aplicacao, caso ainda nao existam
function Criar-Banco {
    $senha = $senhaAdminPadrao
    if (-not (Executar-Sql 'postgres' $senha 'postgres' 'SELECT 1').Ok) {
        Escrever 'Foi encontrado um PostgreSQL ja instalado neste computador.'
        Escrever 'Informe a senha do usuario "postgres" para criar o banco da aplicacao (ou apenas ENTER para ver as instrucoes manuais):'
        $senha = Ler-Senha
        if (-not $senha -or -not (Executar-Sql 'postgres' $senha 'postgres' 'SELECT 1').Ok) {
            Falhar ("Nao foi possivel acessar o PostgreSQL como administrador.`n" + (Instrucoes-Manuais))
        }
    }

    $papel = Executar-Sql 'postgres' $senha 'postgres' "SELECT 1 FROM pg_roles WHERE rolname='$bancoUsuario'"
    if ($papel.Saida -eq '1') {
        [void](Executar-Sql 'postgres' $senha 'postgres' "ALTER ROLE $bancoUsuario WITH LOGIN PASSWORD '$bancoSenha'")
    } else {
        $r = Executar-Sql 'postgres' $senha 'postgres' "CREATE ROLE $bancoUsuario WITH LOGIN PASSWORD '$bancoSenha'"
        if (-not $r.Ok) { Falhar ("Nao foi possivel criar o usuario do banco.`n" + (Instrucoes-Manuais)) }
    }

    $existe = Executar-Sql 'postgres' $senha 'postgres' "SELECT 1 FROM pg_database WHERE datname='$bancoNome'"
    if ($existe.Saida -ne '1') {
        $r = Executar-Sql 'postgres' $senha 'postgres' "CREATE DATABASE $bancoNome OWNER $bancoUsuario ENCODING 'UTF8' TEMPLATE template0"
        if (-not $r.Ok) { $r = Executar-Sql 'postgres' $senha 'postgres' "CREATE DATABASE $bancoNome OWNER $bancoUsuario" }
        if (-not $r.Ok) { Falhar ("Nao foi possivel criar o banco de dados.`n" + (Instrucoes-Manuais)) }
    }

    if (-not (Banco-Pronto)) { Falhar ("O banco foi criado, mas o acesso com o usuario da aplicacao falhou.`n" + (Instrucoes-Manuais)) }
}

function Preparar-Banco {
    $script:psql = Localizar-Psql
    if ($script:psql -and -not (Porta-Aberta)) {
        Iniciar-ServicoPostgres
        [void](Aguardar-Porta 20)
    }
    if ($script:psql -and (Porta-Aberta) -and (Banco-Pronto)) {
        Escrever 'Banco de dados ja configurado.'
        return
    }
    if (-not $script:psql) {
        if (Porta-Aberta) {
            Falhar ("Ha um servidor em execucao na porta $portaBanco, mas o cliente psql nao foi encontrado.`n" + (Instrucoes-Manuais))
        }
        Instalar-Postgres
    } elseif (-not (Porta-Aberta)) {
        Falhar "O PostgreSQL esta instalado, mas nao responde na porta $portaBanco. Inicie o servico (services.msc) e execute novamente."
    }
    Escrever 'Criando o usuario e o banco de dados da aplicacao...'
    Criar-Banco
    Escrever 'Banco de dados pronto.'
}

# ---------------------------------------------------------------- Execucao

Titulo 'Cozinha em Dia: preparacao do ambiente'

if ($SomenteVerificar) {
    Escrever ('JDK 21 ou superior : ' + $(if (Java-Pronto) { 'OK' } else { 'nao encontrado (sera instalado)' }))
    $script:psql = Localizar-Psql
    Escrever ('PostgreSQL (psql)   : ' + $(if ($script:psql) { "OK ($script:psql)" } else { 'nao encontrado (sera instalado)' }))
    $ativo = Porta-Aberta
    Escrever ("Servidor na porta $portaBanco : " + $(if ($ativo) { 'respondendo' } else { 'nao responde' }))
    if ($script:psql -and $ativo) { Escrever ('Banco da aplicacao   : ' + $(if (Banco-Pronto) { 'configurado' } else { 'ainda nao criado (sera criado)' })) }
    Escrever ('winget disponivel   : ' + $(if (Tem-Comando 'winget') { 'sim' } else { 'nao (sera usado download direto)' }))
    Escrever ('Raiz do projeto     : ' + $raiz)
    exit 0
}

Titulo '1/3  Java (JDK 21)'
if (Java-Pronto) { Escrever 'JDK 21 ou superior encontrado.' } else { Instalar-Java; Escrever 'JDK 21 instalado.' }

Titulo '2/3  Banco de dados PostgreSQL'
Preparar-Banco

if ($SomenteBanco) {
    Escrever ''
    Escrever 'Ambiente pronto (opcao -SomenteBanco: a aplicacao nao foi iniciada).'
    exit 0
}

Titulo '3/3  Aplicacao'
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
Escrever 'A aplicacao foi encerrada. O PostgreSQL continua em execucao como servico do Windows.'
Encerrar $codigo
