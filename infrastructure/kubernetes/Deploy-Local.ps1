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

$existing = Invoke-Kubectl -n $namespace get secret coldchain-postgres --ignore-not-found -o json
if ($existing) {
    $data = ($existing | ConvertFrom-Json).data
    $password = [Text.Encoding]::UTF8.GetString([Convert]::FromBase64String($data.POSTGRES_PASSWORD))
} else {
    $bytes = New-Object byte[] 32
    $rng = [Security.Cryptography.RandomNumberGenerator]::Create()
    try { $rng.GetBytes($bytes) } finally { $rng.Dispose() }
    $password = [Convert]::ToBase64String($bytes)
    Set-LocalSecret coldchain-postgres @{ POSTGRES_USER = 'coldchain'; POSTGRES_PASSWORD = $password }
}

$databases = @{
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
foreach ($service in @('monitoring-registry', 'telemetry-ingestion', 'alert-management', 'frontend')) {
    Invoke-Kubectl -n $namespace rollout status "deployment/$service" --timeout=360s
}
