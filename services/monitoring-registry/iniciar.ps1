$ErrorActionPreference = 'Stop'
Set-Location $PSScriptRoot
if (!(Test-Path '.env')) {
    $password = [guid]::NewGuid().ToString('N')
    Set-Content -Path '.env' -Value "DB_PASSWORD=$password" -Encoding ascii
}
docker compose up --build -d
if ($LASTEXITCODE -ne 0) { throw 'No se pudo iniciar. Comprueba que Docker Desktop esté abierto.' }
Write-Host 'Consulta: docker compose logs -f app'
Write-Host 'Cuando termine de arrancar: http://localhost:8080/swagger-ui.html'
