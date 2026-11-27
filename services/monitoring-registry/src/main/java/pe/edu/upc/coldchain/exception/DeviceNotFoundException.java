package pe.edu.upc.coldchain.exception;

public class DeviceNotFoundException extends RuntimeException {
    public DeviceNotFoundException() {
        super("Dispositivo no encontrado");
    }
}
