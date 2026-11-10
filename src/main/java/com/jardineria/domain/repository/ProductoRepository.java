package com.jardineria.domain.repository;

import com.jardineria.domain.model.Producto;

import java.util.List;

/**
 * Puerto de repositorio para la entidad Producto.
 */
public interface ProductoRepository extends CrudRepository<Producto, String> {

    /**
     * Busca productos pertenecientes a una gama específica.
     *
     * @param gama Nombre de la gama
     * @return Lista de productos
     */
    List<Producto> findByGama(String gama);

    /**
     * Busca productos cuya cantidad en stock sea inferior o igual al umbral especificado.
     *
     * @param umbral Cantidad límite de stock crítico
     * @return Lista de productos bajo stock
     */
    List<Producto> findBajoStock(int umbral);

    /**
     * Busca productos cuyo nombre contenga el término de búsqueda.
     *
     * @param termino Texto a buscar en el nombre
     * @return Lista de productos coincidentes
     */
    List<Producto> searchByNombre(String termino);
}
