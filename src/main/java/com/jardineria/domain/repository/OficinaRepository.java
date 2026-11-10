package com.jardineria.domain.repository;

import com.jardineria.domain.model.Oficina;

import java.util.List;

/**
 * Puerto de repositorio para la entidad Oficina.
 */
public interface OficinaRepository extends CrudRepository<Oficina, String> {

    /**
     * Busca oficinas ubicadas en un país específico.
     *
     * @param pais Nombre del país
     * @return Lista de oficinas
     */
    List<Oficina> findByPais(String pais);

    /**
     * Busca oficinas ubicadas en una ciudad específica.
     *
     * @param ciudad Nombre de la ciudad
     * @return Lista de oficinas
     */
    List<Oficina> findByCiudad(String ciudad);
}
