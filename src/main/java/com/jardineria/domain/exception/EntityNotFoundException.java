package com.jardineria.domain.exception;

/**
 * Excepción lanzada cuando una entidad solicitada no existe en el sistema.
 */
public class EntityNotFoundException extends RuntimeException {
    public EntityNotFoundException(String message) {
        super(message);
    }

    public EntityNotFoundException(String entityName, Object id) {
        super(String.format("%s con identificador '%s' no fue encontrado(a).", entityName, id));
    }
}
