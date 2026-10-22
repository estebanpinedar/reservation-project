package co.edu.uniajc.rsu_auth_service.exeption;

public class ConflictException extends RuntimeException {
    public ConflictException(String message) {
        super(message);
    }
}
