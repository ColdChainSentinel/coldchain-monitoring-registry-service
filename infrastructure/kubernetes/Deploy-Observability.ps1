#Requires -Version 5.1
<#
.SYNOPSIS
  Despliega Prometheus, Alertmanager, Grafana, Loki, Promtail, Jaeger y
  kube-state-metrics en el namespace monitoring.
.EXAMPLE
  ./Deploy-Observability.ps1 -Context default
#>
param([string]$Context = 'docker-desktop')
$ErrorActionPreference = 'Stop'

function Invoke-Kubectl {
    & kubectl --context $Context @args
    if ($LASTEXITCODE -ne 0) { throw "kubectl falló (código $LASTEXITCODE)." }
}

Invoke-Kubectl apply -f (Join-Path $PSScriptRoot 'observability/namespace.yaml')

# Contraseña de Grafana: aleatoria, fuera de Git, reutilizada si ya existe.
$existing = Invoke-Kubectl -n monitoring get secret grafana-admin --ignore-not-found -o name
if (-not $existing) {
    $bytes = New-Object byte[] 24
    $rng = [Security.Cryptography.RandomNumberGenerator]::Create()
    try { $rng.GetBytes($bytes) } finally { $rng.Dispose() }
    $password = [Convert]::ToBase64String($bytes)
    Invoke-Kubectl -n monitoring create secret generic grafana-admin `
        --from-literal=username=admin "--from-literal=password=$password"
}

Invoke-Kubectl apply -k (Join-Path $PSScriptRoot 'observability')
foreach ($name in @('prometheus', 'alertmanager', 'grafana', 'loki', 'jaeger', 'kube-state-metrics')) {
    Invoke-Kubectl -n monitoring rollout status "deployment/$name" --timeout=300s
}
Invoke-Kubectl -n monitoring rollout status daemonset/promtail --timeout=300s
Write-Output 'Observabilidad lista: http://grafana.localhost:8088, http://prometheus.localhost:8088, http://jaeger.localhost:8088'
