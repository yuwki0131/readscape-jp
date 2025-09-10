package jp.readscape.inventory.exceptions;

public class DuplicateIsbnException extends RuntimeException {
    public DuplicateIsbnException(String message) {
        super(message);
    }
    
    public DuplicateIsbnException(String message, Throwable cause) {
        super(message, cause);
    }
}