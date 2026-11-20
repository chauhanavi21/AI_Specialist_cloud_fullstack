# Windows entry point for the same deployment and evidence checks used on Linux.
[CmdletBinding()]
param([Parameter(Mandatory = $true)][string]$Namespace)
$ErrorActionPreference = 'Stop'
if ($Namespace -notmatch '^[a-z0-9]([-a-z0-9]*[a-z0-9])?$' -or $Namespace.Length -gt 63) {
    throw 'Enter the project/namespace name from the OpenShift console.'
}
$repoRoot = Split-Path $PSScriptRoot -Parent
$clientDir = Join-Path $repoRoot '.tools/openshift'
$bashPath = Join-Path $env:ProgramFiles 'Git/bin/bash.exe'
if (-not (Test-Path -LiteralPath $bashPath)) { throw 'Install Git for Windows (including Git Bash) first.' }
if (-not (Get-Command python -ErrorAction SilentlyContinue)) { throw 'Python 3 is required for the API evidence checks.' }
$previousPath = $env:PATH
try {
    $env:PATH = "$clientDir;$previousPath"
    if (-not (Get-Command oc -ErrorAction SilentlyContinue)) { throw 'Run deploy/setup-openshift.ps1 first.' }
    Push-Location $repoRoot
    try {
        & $bashPath './deploy/deploy-openshift.sh' $Namespace
        if ($LASTEXITCODE -ne 0) { throw 'Deployment or validation failed. Check the evidence directory and console output.' }
    } finally { Pop-Location }
} finally { $env:PATH = $previousPath }
