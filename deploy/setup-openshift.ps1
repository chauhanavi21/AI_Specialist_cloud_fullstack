# Installs a checksum-verified OpenShift client into this repo; no administrator access needed.
[CmdletBinding()]
param([string]$Version = '5.0.1')
$ErrorActionPreference = 'Stop'
if ($Version -notmatch '^\d+\.\d+\.\d+$') { throw 'Version must be an exact release, for example 5.0.1' }
$repoRoot = Split-Path $PSScriptRoot -Parent
$toolDir = Join-Path $repoRoot '.tools/openshift'
New-Item -ItemType Directory -Force $toolDir | Out-Null
$baseUrl = "https://mirror.openshift.com/pub/openshift-v4/clients/ocp/$Version"
$archive = Join-Path $toolDir "openshift-client-windows-$Version.zip"
$sums = (Invoke-WebRequest -UseBasicParsing "$baseUrl/sha256sum.txt").Content
$match = [regex]::Match($sums, '(?m)^([a-f0-9]{64})\s+openshift-client-windows\.zip\s*$')
if (-not $match.Success) { throw 'The release has no Windows checksum; no client was installed.' }
if (-not (Test-Path -LiteralPath $archive)) {
    Invoke-WebRequest -UseBasicParsing "$baseUrl/openshift-client-windows.zip" -OutFile $archive
}
if ((Get-FileHash -LiteralPath $archive -Algorithm SHA256).Hash.ToLowerInvariant() -ne $match.Groups[1].Value) {
    throw "Checksum mismatch. Remove $archive and run this script again."
}
Expand-Archive -LiteralPath $archive -DestinationPath $toolDir -Force
& (Join-Path $toolDir 'oc.exe') version --client
if ($LASTEXITCODE -ne 0) { throw 'The OpenShift client did not run.' }
Write-Host "Client installed in $toolDir"
Write-Host 'Next: activate https://developers.redhat.com/developer-sandbox and open its OpenShift console.'
Write-Host 'Use your profile menu > Copy login command. Run that command LOCALLY with this client.'
Write-Host 'Do not paste the login token into chat, git, screenshots, or your recording.'
Write-Host 'See docs/RESUBMISSION.md for the exact Windows commands.'
