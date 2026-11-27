package pe.edu.upc.coldchain.identity.exception;

public class InvalidCredentialsException extends RuntimeException {
    public InvalidCredentialsException() {
        super("Credenciales inválidas o cuenta inactiva");
    }
}
