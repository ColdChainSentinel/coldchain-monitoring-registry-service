package pe.edu.upc.coldchain.identity.exception;

public class DuplicateUsernameException extends RuntimeException {
    public DuplicateUsernameException() {
        super("Ya existe una cuenta con ese identificador");
    }
}
