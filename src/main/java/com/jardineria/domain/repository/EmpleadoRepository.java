package com.jardineria.domain.repository;

import com.jardineria.domain.model.Empleado;

import java.util.List;

/**
 * Puerto de repositorio para la entidad Empleado.
 */
public interface EmpleadoRepository extends CrudRepository<Empleado, Integer> {

    /**
     * Busca empleados asignados a una oficina determinada.
     *
     * @param codigoOficina Código de la oficina
     * @return Lista de empleados
     */
    List<Empleado> findByOficina(String codigoOficina);

    /**
     * Busca los subordinados directos de un jefe.
     *
     * @param codigoJefe Código del empleado jefe
     * @return Lista de subordinados
     */
    List<Empleado> findByJefe(Integer codigoJefe);

    /**
     * Busca empleados que no tienen jefe (Directores Generales / Máximos responsables).
     *
     * @return Lista de directores
     */
    List<Empleado> findDirectores();

    /**
     * Busca empleados que desempeñan el rol de Representante de Ventas.
     *
     * @return Lista de representantes de ventas
     */
    List<Empleado> findRepresentantesVentas();
}
