<#
.SYNOPSIS
    Script automatizado para execucao de testes de carga e geracao de relatorios HTML com JMeter no COTAI.
.PARAMETER Usuarios
    Quantidade de usuarios virtuais por grupo (Web e API). Padrao: 10.
.PARAMETER RampUp
    Tempo em segundos para inicializar os usuarios. Padrao: 15.
.PARAMETER LoopCount
    Quantidade de repeticoes de cada usuario. Padrao: 1.
.EXAMPLE
    .\executar-carga-cli.ps1 -Usuarios 10
    .\executar-carga-cli.ps1 -Usuarios 25 -RampUp 30
    .\executar-carga-cli.ps1 -Usuarios 50 -RampUp 60
#>
[CmdletBinding()]
param(
    [int]$Usuarios = 10,
    [int]$RampUp = 15,
    [int]$LoopCount = 1,
    [switch]$SemRelatorioNoNavegador
)

$ErrorActionPreference = "Stop"
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Definition
Set-Location $ScriptDir

Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host "   COTAI - Teste de Carga e Desempenho (CLI Headless)     " -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host "Usuarios por grupo (Web + API): $Usuarios" -ForegroundColor Yellow
Write-Host "Ramp-up (tempo de subida):      $RampUp s" -ForegroundColor Yellow
Write-Host "Repeticoes (Loop):              $LoopCount" -ForegroundColor Yellow

$PlanoJmx = Join-Path $ScriptDir "COTAI-Teste-Angular-Completo.jmx"
if (-not (Test-Path $PlanoJmx)) {
    Write-Error "Plano de teste nao encontrado em: $PlanoJmx"
}

$Timestamp = Get-Date -Format "yyyyMMdd-HHmmss"
$PastaResultados = Join-Path $ScriptDir "resultados"
$PastaRelatorios = Join-Path $ScriptDir "relatorios"

New-Item -ItemType Directory -Force -Path $PastaResultados | Out-Null
New-Item -ItemType Directory -Force -Path $PastaRelatorios | Out-Null

$ArquivoJtl = Join-Path $PastaResultados "resultado-u${Usuarios}-${Timestamp}.jtl"
$ArquivoLog = Join-Path $PastaResultados "jmeter-u${Usuarios}-${Timestamp}.log"
$RelatorioHtmlDir = Join-Path $PastaRelatorios "relatorio-u${Usuarios}-${Timestamp}"

# Localizar binario do JMeter
$JmeterExe = "jmeter"
if (-not (Get-Command jmeter -ErrorAction SilentlyContinue)) {
    $caminhos = @(
        "$env:LOCALAPPDATA\Programs\JMeter\bin\jmeter.bat",
        "C:\Ferramentas\apache-jmeter-5.6.3\bin\jmeter.bat"
    )
    $encontrado = $caminhos | Where-Object { Test-Path $_ } | Select-Object -First 1
    if ($encontrado) {
        $JmeterExe = $encontrado
    } else {
        Write-Error "Executavel do JMeter nao encontrado. Instale o JMeter ou adicione-o ao PATH."
    }
}

Write-Host "`n[1/3] Executando teste de carga..." -ForegroundColor Green
& $JmeterExe `
    -n `
    -t $PlanoJmx `
    "-JwebThreads=$Usuarios" `
    "-JapiThreads=$Usuarios" `
    "-JrampSeconds=$RampUp" `
    "-JloopCount=$LoopCount" `
    -l $ArquivoJtl `
    -j $ArquivoLog

if ($LASTEXITCODE -ne 0) {
    Write-Warning "JMeter finalizou com avisos/erros (exit code $LASTEXITCODE). Verifique o log: $ArquivoLog"
}

Write-Host "`n[2/3] Gerando dashboard HTML de relatorio..." -ForegroundColor Green
& $JmeterExe -g $ArquivoJtl -o $RelatorioHtmlDir

Write-Host "`n[3/3] Relatorio HTML gerado com sucesso em:" -ForegroundColor Cyan
Write-Host "   $RelatorioHtmlDir\index.html" -ForegroundColor White

if (-not $SemRelatorioNoNavegador -and (Test-Path "$RelatorioHtmlDir\index.html")) {
    Write-Host "`nAbrindo relatorio no navegador..." -ForegroundColor Green
    Start-Process "$RelatorioHtmlDir\index.html"
}
