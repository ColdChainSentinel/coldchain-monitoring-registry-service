package pe.edu.upc.coldchain.exception;

public class PointNotFoundException extends RuntimeException {
    public PointNotFoundException() {
        super("No se encontró el punto de monitoreo");
    }
}
