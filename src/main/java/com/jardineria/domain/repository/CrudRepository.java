package com.jardineria.domain.repository;

import java.util.List;
import java.util.Optional;

/**
 * Contrato genérico de persistencia que define las operaciones CRUD fundamentales.
 *
 * @param <T>  Tipo de entidad
 * @param <ID> Tipo de clave primaria
 */
public interface CrudRepository<T, ID> {

    /**
     * Guarda una nueva entidad en el almacenamiento.
     *
     * @param entity Entidad a persistir
     * @return Entidad guardada
     */
    T save(T entity);

    /**
     * Actualiza una entidad existente.
     *
     * @param entity Entidad con los datos actualizados
     * @return Entidad actualizada
     */
    T update(T entity);

    /**
     * Busca una entidad por su clave primaria.
     *
     * @param id Identificador de la entidad
     * @return Optional con la entidad si existe, o vacío si no se encuentra
     */
    Optional<T> findById(ID id);

    /**
     * Recupera todas las entidades registradas.
     *
     * @return Lista de entidades
     */
    List<T> findAll();

    /**
     * Elimina una entidad por su clave primaria.
     *
     * @param id Identificador de la entidad a eliminar
     * @return true si se eliminó, false en caso contrario
     */
    boolean deleteById(ID id);

    /**
     * Comprueba si existe una entidad con el identificador dado.
     *
     * @param id Identificador a verificar
     * @return true si existe, false si no
     */
    boolean existsById(ID id);

    /**
     * Retorna el número total de entidades persistidas.
     *
     * @return Conteo total de registros
     */
    long count();
}
