package pe.edu.upc.coldchain.telemetry.exception;

public class RegistryUnavailableException extends RuntimeException {
    public RegistryUnavailableException() {
        super("Monitoring Registry no está disponible para validar el punto");
    }
}
