#Requires -Version 5.1
<#
.SYNOPSIS
  Crea los Secrets de un ambiente y, salvo con -SecretsOnly, aplica su overlay.

.DESCRIPTION
  Los Secrets nunca se guardan en Git. Este script los genera una vez por ambiente
  (contraseñas aleatorias de 32 bytes) y las reutiliza en ejecuciones posteriores.
  - local: aplica overlays/local y espera los rollouts (desarrollo individual).
  - dev / prod: usar -SecretsOnly; Argo CD aplica los manifiestos desde Git.

.EXAMPLE
  ./Deploy-Local.ps1                                   # Docker Desktop, overlay local
  ./Deploy-Local.ps1 -Context default -Overlay dev -SecretsOnly
#>
param(
    [string]$Context = 'docker-desktop',
    [ValidateSet('local', 'dev', 'prod')][string]$Overlay = 'local',
    [switch]$SecretsOnly
)
$ErrorActionPreference = 'Stop'
$namespace = "coldchain-$Overlay"

function Invoke-Kubectl {
    & kubectl --context $Context @args
    if ($LASTEXITCODE -ne 0) { throw "kubectl falló (código $LASTEXITCODE)." }
}

function Set-EnvironmentSecret($name, $values) {
    $manifest = @{
        apiVersion = 'v1'
        kind = 'Secret'
        metadata = @{ name = $name; namespace = $namespace }
        type = 'Opaque'
        stringData = $values
    } | ConvertTo-Json -Depth 5 -Compress
    $manifest | kubectl --context $Context apply --server-side --field-manager=coldchain-secrets -f -
    if ($LASTEXITCODE -ne 0) { throw "No se pudo aplicar el Secret $name." }
}

function New-RandomSecret {
    $bytes = New-Object byte[] 32
    $rng = [Security.Cryptography.RandomNumberGenerator]::Create()
    try { $rng.GetBytes($bytes) } finally { $rng.Dispose() }
    [Convert]::ToBase64String($bytes)
}

function Get-SecretValue($name, $key) {
    $existing = Invoke-Kubectl -n $namespace get secret $name --ignore-not-found -o json
    if (-not $existing) { return $null }
    $value = ($existing | ConvertFrom-Json).data.$key
    if (-not $value) { return $null }
    [Text.Encoding]::UTF8.GetString([Convert]::FromBase64String($value))
}

if (-not (Invoke-Kubectl get namespace $namespace --ignore-not-found -o name)) {
    Invoke-Kubectl apply -f (Join-Path $PSScriptRoot "overlays/$Overlay/namespace.yaml")
}

$password = Get-SecretValue coldchain-postgres POSTGRES_PASSWORD
if (-not $password) {
    $password = New-RandomSecret
    Set-EnvironmentSecret coldchain-postgres @{ POSTGRES_USER = 'coldchain'; POSTGRES_PASSWORD = $password }
}

# Administrador inicial de Identity Access. La contraseña se genera una sola vez y
# se lee con: kubectl -n <namespace> get secret identity-access-bootstrap -o jsonpath=...
if (-not (Get-SecretValue identity-access-bootstrap BOOTSTRAP_ADMIN_PASSWORD)) {
    Set-EnvironmentSecret identity-access-bootstrap @{
        BOOTSTRAP_ADMIN_USERNAME = 'admin'
        BOOTSTRAP_ADMIN_PASSWORD = New-RandomSecret
    }
}

$databases = @{
    'identity-access' = 'coldchain_identity'
    'monitoring-registry' = 'coldchain'
    'telemetry-ingestion' = 'coldchain_telemetry'
    'alert-management' = 'coldchain_alerts'
}
foreach ($service in $databases.Keys) {
    Set-EnvironmentSecret "$service-db" @{
        DB_URL = "jdbc:postgresql://postgres:5432/$($databases[$service])"
        DB_USER = 'coldchain'
        DB_PASSWORD = $password
    }
}

if ($SecretsOnly) {
    Write-Output "Secrets listos en $namespace. Argo CD sincroniza el resto desde Git."
    return
}

Invoke-Kubectl apply -k (Join-Path $PSScriptRoot "overlays/$Overlay")
Invoke-Kubectl -n $namespace rollout status statefulset/postgres --timeout=240s

# El script de init solo corre con un volumen vacío: en un PVC creado antes de
# integrar Identity, la base coldchain_identity se crea aquí.
$hasIdentityDb = Invoke-Kubectl -n $namespace exec postgres-0 -- `
    psql -U coldchain -d coldchain -tAc "SELECT 1 FROM pg_database WHERE datname = 'coldchain_identity'"
if ("$hasIdentityDb".Trim() -ne '1') {
    Invoke-Kubectl -n $namespace exec postgres-0 -- psql -U coldchain -d coldchain -c 'CREATE DATABASE coldchain_identity'
}

foreach ($service in @('identity-access', 'monitoring-registry', 'telemetry-ingestion', 'alert-management', 'frontend')) {
    Invoke-Kubectl -n $namespace rollout status "deployment/$service" --timeout=360s
}
