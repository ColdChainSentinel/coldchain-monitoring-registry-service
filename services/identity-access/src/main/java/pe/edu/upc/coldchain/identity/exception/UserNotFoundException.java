package pe.edu.upc.coldchain.identity.exception;

public class UserNotFoundException extends RuntimeException {
    public UserNotFoundException() {
        super("Cuenta no encontrada");
    }
}
