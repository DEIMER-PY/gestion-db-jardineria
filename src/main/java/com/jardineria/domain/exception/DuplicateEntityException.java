package com.jardineria.domain.exception;

/**
 * Excepción lanzada cuando se intenta registrar una entidad con clave primaria duplicada.
 */
public class DuplicateEntityException extends RuntimeException {
    public DuplicateEntityException(String message) {
        super(message);
    }

    public DuplicateEntityException(String entityName, Object id) {
        super(String.format("Ya existe un(a) %s con el identificador '%s'.", entityName, id));
    }
}
