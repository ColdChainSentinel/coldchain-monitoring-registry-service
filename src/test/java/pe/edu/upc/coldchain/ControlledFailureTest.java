package pe.edu.upc.coldchain;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.fail;

class ControlledFailureTest {
    @Test
    void demonstratesControlledFailure() {
        fail("Fallo controlado para evidenciar que CI detecta pruebas fallidas; eliminar para corregir");
    }
}
