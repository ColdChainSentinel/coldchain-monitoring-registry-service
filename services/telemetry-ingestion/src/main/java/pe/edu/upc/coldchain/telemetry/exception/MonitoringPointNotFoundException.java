package pe.edu.upc.coldchain.telemetry.exception;

public class MonitoringPointNotFoundException extends RuntimeException {
    public MonitoringPointNotFoundException() {
        super("No se encontró el punto de monitoreo en Registry");
    }
}
