# Opens the cluster's sign-in page; credentials go to Red Hat and the token stays in the local kubeconfig.
[CmdletBinding()]
param([Parameter(Mandatory = $true)][string]$Server)
$ErrorActionPreference = 'Stop'
$uri = [uri]$Server
if (-not $uri.IsAbsoluteUri -or $uri.Scheme -ne 'https' -or $uri.UserInfo -or $uri.Query -or $uri.Fragment) {
    throw 'Enter only the HTTPS API server address from the OpenShift console, without a token.'
}
$client = Join-Path (Split-Path $PSScriptRoot -Parent) '.tools/openshift/oc.exe'
if (-not (Test-Path -LiteralPath $client)) { throw 'Run deploy/setup-openshift.ps1 first.' }
& $client login $Server --web
if ($LASTEXITCODE -ne 0) {
    throw 'Browser login failed. Use Copy login command in the console and run it LOCALLY with ./.tools/openshift/oc.exe.'
}
& $client project -q
