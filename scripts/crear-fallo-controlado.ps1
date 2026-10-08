$ErrorActionPreference = 'Stop'
Set-Location (Split-Path $PSScriptRoot -Parent)
$branch = git branch --show-current
if ($LASTEXITCODE -ne 0 -or $branch -notlike 'evidence/*') {
    throw 'Ejecuta esto únicamente en una rama evidence/*.'
}
$path = 'src/test/java/pe/edu/upc/coldchain/ControlledFailureTest.java'
if (Test-Path $path) { throw 'El archivo ya existe; no se sobrescribirá.' }
@'
package pe.edu.upc.coldchain;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.fail;

class ControlledFailureTest {
    @Test
    void demonstratesControlledFailure() {
        fail("Fallo controlado para evidenciar que CI detecta pruebas fallidas; eliminar para corregir");
    }
}
'@ | Set-Content -Path $path -Encoding ascii
Write-Host 'Prueba creada. Haz commit y push de esta rama; nunca fusiones el fallo a main.'
