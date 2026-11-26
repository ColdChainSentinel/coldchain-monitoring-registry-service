#Requires -Version 5.1
param([ValidateRange(1024, 65535)][int]$LocalPort = 18080)

$ErrorActionPreference = 'Stop'
$context = 'docker-desktop'
$namespace = 'coldchain-local'
$baseUrl = "http://127.0.0.1:$LocalPort"
$kubectl = (Get-Command kubectl -CommandType Application).Source

function Invoke-Kubectl {
    & $kubectl --context $context -n $namespace @args
    if ($LASTEXITCODE -ne 0) { throw "kubectl falló (código $LASTEXITCODE)." }
}

function Post-Json($path, $payload) {
    Invoke-RestMethod -Uri "$baseUrl$path" -Method Post -ContentType 'application/json' `
        -Body ($payload | ConvertTo-Json -Compress) -TimeoutSec 15
}

$forward = Start-Process -FilePath $kubectl -WindowStyle Hidden -PassThru -ArgumentList @(
    '--context', $context, '-n', $namespace, 'port-forward', '--address', '127.0.0.1',
    'service/frontend', "${LocalPort}:8080"
)

try {
    $deadline = (Get-Date).AddSeconds(25)
    do {
        if ($forward.HasExited) { throw "Terminó port-forward; revisa si el puerto $LocalPort está disponible." }
        try { $web = Invoke-WebRequest -Uri "$baseUrl/" -UseBasicParsing -TimeoutSec 3; break }
        catch {
            if ((Get-Date) -ge $deadline) { throw }
            Start-Sleep -Milliseconds 400
        }
    } while ($true)
    if ($web.StatusCode -ne 200 -or $web.Content -notmatch 'ColdChain Sentinel') {
        throw 'Frontend: se esperaba la página de ColdChain Sentinel.'
    }

    $code = 'K8S-' + [Guid]::NewGuid().ToString('N').Substring(0, 10).ToUpperInvariant()
    $point = Post-Json '/registry/api/v1/monitoring-points' @{
        code = $code; name = 'Punto de prueba Kubernetes'; type = 'ALMACENAMIENTO'
    }
    $pointId = ([Guid]$point.id).ToString()
    if ($point.code -ne $code) { throw 'Registry: no devolvió el punto registrado.' }

    $rule = @"
coldchain:
  alerts:
    rules:
      "$pointId":
        threshold-min: 2.0
        threshold-max: 8.0
"@
    $patch = ConvertTo-Json -Compress -InputObject @(
        @{ op = 'replace'; path = '/data/thermal-rules.yaml'; value = $rule }
    )
    $patchFile = New-TemporaryFile
    try {
        [IO.File]::WriteAllText($patchFile.FullName, $patch, [Text.UTF8Encoding]::new($false))
        Invoke-Kubectl patch configmap coldchain-thermal-rules --type=json --patch-file $patchFile.FullName
    } finally {
        Remove-Item -LiteralPath $patchFile.FullName -ErrorAction SilentlyContinue
    }
    Invoke-Kubectl rollout restart deployment/alert-management
    Invoke-Kubectl rollout status deployment/alert-management --timeout=240s
    # A read through the Service warms the newly rolled-out API without retrying a write.
    $alertsUrl = "$baseUrl/alerts/api/alerts?monitoringPointId=$pointId"
    $deadline = (Get-Date).AddSeconds(25)
    do {
        try { $null = Invoke-RestMethod -Uri $alertsUrl -TimeoutSec 3; break }
        catch {
            if ((Get-Date) -ge $deadline) { throw }
            Start-Sleep -Milliseconds 400
        }
    } while ($true)

    $measurement = Post-Json '/telemetry/api/measurements' @{
        monitoringPointId = $pointId; temperature = 10.0
        timestamp = [DateTime]::UtcNow.ToString('o')
    }
    if ($measurement.evaluationStatus -ne 'COMPLETED') {
        throw "Telemetry: se esperaba COMPLETED y se obtuvo $($measurement.evaluationStatus)."
    }
    $persisted = Invoke-RestMethod -Uri "$baseUrl/telemetry/api/measurements?monitoringPointId=$pointId" -TimeoutSec 15
    if (-not ($persisted | Where-Object { $_.id -eq $measurement.id })) {
        throw 'Telemetry: la medición creada no aparece en la consulta.'
    }
    $alerts = Invoke-RestMethod -Uri $alertsUrl -TimeoutSec 15
    $alert = $alerts | Where-Object { $_.measurementId -eq $measurement.id }
    if (@($alert).Count -ne 1 -or $alert.type -ne 'HIGH_TEMPERATURE' -or $alert.status -ne 'OPEN') {
        throw 'Alerts: se esperaba una alerta HIGH_TEMPERATURE en estado OPEN para la medición.'
    }
    Write-Output "OK: $code / $pointId / medición $($measurement.id) / alerta $($alert.id)"
} finally {
    if (-not $forward.HasExited) { Stop-Process -Id $forward.Id -ErrorAction SilentlyContinue }
}
