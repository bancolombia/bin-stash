package co.com.bancolombia.binstash.model;

public class InvalidKeyException extends RuntimeException{
    public InvalidKeyException(String message) {
        super(message);
    }
}
