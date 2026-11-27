#Requires -Version 5.1
$ErrorActionPreference = 'Stop'
$context = 'docker-desktop'
$namespace = 'coldchain-local'

function Invoke-Kubectl {
    & kubectl --context $context @args
    if ($LASTEXITCODE -ne 0) { throw "kubectl falló (código $LASTEXITCODE)." }
}

function Set-LocalSecret($name, $values) {
    $manifest = @{
        apiVersion = 'v1'
        kind = 'Secret'
        metadata = @{ name = $name; namespace = $namespace }
        type = 'Opaque'
        stringData = $values
    } | ConvertTo-Json -Depth 5 -Compress
    $manifest | kubectl --context $context apply --server-side --field-manager=coldchain-local -f -
    if ($LASTEXITCODE -ne 0) { throw "No se pudo aplicar el Secret $name." }
}

if (-not (Invoke-Kubectl get namespace $namespace --ignore-not-found -o name)) {
    Invoke-Kubectl apply -f (Join-Path $PSScriptRoot 'overlays/local/namespace.yaml')
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

$password = Get-SecretValue coldchain-postgres POSTGRES_PASSWORD
if (-not $password) {
    $password = New-RandomSecret
    Set-LocalSecret coldchain-postgres @{ POSTGRES_USER = 'coldchain'; POSTGRES_PASSWORD = $password }
}

# Administrador inicial de Identity Access. La contraseña se genera una sola vez y
# se lee con: kubectl -n coldchain-local get secret identity-access-bootstrap -o jsonpath=...
if (-not (Get-SecretValue identity-access-bootstrap BOOTSTRAP_ADMIN_PASSWORD)) {
    Set-LocalSecret identity-access-bootstrap @{
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
    Set-LocalSecret "$service-db" @{
        DB_URL = "jdbc:postgresql://postgres:5432/$($databases[$service])"
        DB_USER = 'coldchain'
        DB_PASSWORD = $password
    }
}

Invoke-Kubectl apply -k (Join-Path $PSScriptRoot 'overlays/local')
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
