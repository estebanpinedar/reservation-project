package co.edu.uniajc.rsu_auth_service.exeption;

public class InvalidCredentialsException extends RuntimeException {
    public InvalidCredentialsException() {
        super("Invalid email or password");
    }
}
