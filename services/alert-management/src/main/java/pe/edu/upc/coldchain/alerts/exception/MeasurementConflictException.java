package pe.edu.upc.coldchain.alerts.exception;

public class MeasurementConflictException extends RuntimeException {
    public MeasurementConflictException() {
        super("measurementId ya fue procesado con otros datos");
    }
}
