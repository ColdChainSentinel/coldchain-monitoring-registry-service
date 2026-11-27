package pe.edu.upc.coldchain.alerts.exception;

public class ForbiddenAccessException extends RuntimeException {
    public ForbiddenAccessException() {
        super("No tiene permisos para operar sobre este punto de monitoreo");
    }
}
