package pe.edu.upc.coldchain.alerts.exception;

public class RegistryUnavailableException extends RuntimeException {
    public RegistryUnavailableException() {
        super("Monitoring Registry no está disponible para validar el punto");
    }
}
