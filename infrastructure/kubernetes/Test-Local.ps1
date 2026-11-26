#Requires -Version 5.1
param(
    [ValidateRange(1024, 65535)][int]$LocalPort = 18080,
    [string]$Context = 'docker-desktop',
    [string]$Namespace = 'coldchain-local'
)

$ErrorActionPreference = 'Stop'
$context = $Context
$namespace = $Namespace
$baseUrl = "http://127.0.0.1:$LocalPort"
$kubectl = (Get-Command kubectl -CommandType Application).Source

function Invoke-Kubectl {
    & $kubectl --context $context -n $namespace @args
    if ($LASTEXITCODE -ne 0) { throw "kubectl falló (código $LASTEXITCODE)." }
}

$script:headers = @{}

function Send-Json($method, $path, $payload) {
    Invoke-RestMethod -Uri "$baseUrl$path" -Method $method -ContentType 'application/json' `
        -Headers $script:headers -Body ($payload | ConvertTo-Json -Compress) -TimeoutSec 15
}

function Post-Json($path, $payload) { Send-Json Post $path $payload }

function Get-Json($path) {
    Invoke-RestMethod -Uri "$baseUrl$path" -Headers $script:headers -TimeoutSec 15
}

function Get-SecretValue($name, $key) {
    $encoded = & $kubectl --context $context -n $namespace get secret $name -o "jsonpath={.data.$key}"
    if ($LASTEXITCODE -ne 0 -or -not $encoded) { throw "No se pudo leer $key del Secret $name." }
    [Text.Encoding]::UTF8.GetString([Convert]::FromBase64String($encoded))
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

    $login = Post-Json '/identity/api/v1/auth/login' @{
        username = Get-SecretValue identity-access-bootstrap BOOTSTRAP_ADMIN_USERNAME
        password = Get-SecretValue identity-access-bootstrap BOOTSTRAP_ADMIN_PASSWORD
    }
    if (-not $login.token) { throw 'Identity: el inicio de sesión no devolvió un token.' }
    $script:headers = @{ Authorization = "Bearer $($login.token)" }

    $code = 'K8S-' + [Guid]::NewGuid().ToString('N').Substring(0, 10).ToUpperInvariant()
    $point = Post-Json '/registry/api/v1/monitoring-points' @{
        code = $code; name = 'Punto de prueba Kubernetes'; type = 'ALMACENAMIENTO'
    }
    $pointId = ([Guid]$point.id).ToString()
    if ($point.code -ne $code) { throw 'Registry: no devolvió el punto registrado.' }

    # HU-09: el rango térmico se configura por API (persistido en Alert Management).
    $rule = Send-Json Put "/alerts/api/alerts/rules/$pointId" @{ thresholdMin = 2.0; thresholdMax = 8.0 }
    if ([decimal]$rule.thresholdMax -ne 8.0) { throw 'Alerts: no se guardó la regla térmica de 2 a 8 °C.' }
    $alertsPath = "/alerts/api/alerts?monitoringPointId=$pointId"

    $measurement = Post-Json '/telemetry/api/measurements' @{
        monitoringPointId = $pointId; temperature = 10.0
        timestamp = [DateTime]::UtcNow.ToString('o')
    }
    if ($measurement.evaluationStatus -ne 'COMPLETED') {
        throw "Telemetry: se esperaba COMPLETED y se obtuvo $($measurement.evaluationStatus)."
    }
    $persisted = Get-Json "/telemetry/api/measurements?monitoringPointId=$pointId"
    if (-not ($persisted | Where-Object { $_.id -eq $measurement.id })) {
        throw 'Telemetry: la medición creada no aparece en la consulta.'
    }
    $alerts = Get-Json $alertsPath
    $alert = $alerts | Where-Object { $_.measurementId -eq $measurement.id }
    if (@($alert).Count -ne 1 -or $alert.type -ne 'HIGH_TEMPERATURE' -or $alert.status -ne 'OPEN') {
        throw 'Alerts: se esperaba una alerta HIGH_TEMPERATURE en estado OPEN para la medición.'
    }
    Write-Output "OK: $code / $pointId / medición $($measurement.id) / alerta $($alert.id)"
} finally {
    if (-not $forward.HasExited) { Stop-Process -Id $forward.Id -ErrorAction SilentlyContinue }
}
