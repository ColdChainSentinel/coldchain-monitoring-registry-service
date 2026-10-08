package pe.edu.upc.coldchain.exception;

public class DuplicateCodeException extends RuntimeException {
    public DuplicateCodeException() {
        super("Ya existe un punto con ese código");
    }
}
