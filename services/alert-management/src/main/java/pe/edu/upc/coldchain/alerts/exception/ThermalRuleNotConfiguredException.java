package pe.edu.upc.coldchain.alerts.exception;

public class ThermalRuleNotConfiguredException extends RuntimeException {
    public ThermalRuleNotConfiguredException() {
        super("El punto de monitoreo no tiene una regla térmica configurada");
    }
}
