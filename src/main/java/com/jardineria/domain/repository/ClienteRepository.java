package com.jardineria.domain.repository;

import com.jardineria.domain.model.Cliente;

import java.util.List;

/**
 * Puerto de repositorio para la entidad Cliente.
 */
public interface ClienteRepository extends CrudRepository<Cliente, Integer> {

    /**
     * Busca clientes atendidos por un representante de ventas específico.
     *
     * @param codigoEmpleado Código del empleado representante
     * @return Lista de clientes
     */
    List<Cliente> findByRepresentanteVentas(Integer codigoEmpleado);

    /**
     * Busca clientes por país.
     *
     * @param pais Nombre del país
     * @return Lista de clientes
     */
    List<Cliente> findByPais(String pais);

    /**
     * Busca clientes por ciudad.
     *
     * @param ciudad Nombre de la ciudad
     * @return Lista de clientes
     */
    List<Cliente> findByCiudad(String ciudad);
}
