package pe.edu.upc.coldchain.exception;

public class InactivePointException extends RuntimeException {
    public InactivePointException() {
        super("El punto de monitoreo no está activo");
    }
}
