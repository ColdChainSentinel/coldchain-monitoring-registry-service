package pe.edu.upc.coldchain.identity.exception;

public class UnauthorizedSessionException extends RuntimeException {
    public UnauthorizedSessionException() {
        super("Sesión inválida, revocada o expirada");
    }
}
