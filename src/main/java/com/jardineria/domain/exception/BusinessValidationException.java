package com.jardineria.domain.exception;

/**
 * Excepción lanzada cuando una operación viola una regla de negocio.
 */
public class BusinessValidationException extends RuntimeException {
    public BusinessValidationException(String message) {
        super(message);
    }

    public BusinessValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
