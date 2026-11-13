package com.jardineria.application.port.in;

import com.jardineria.domain.model.Producto;
import java.math.BigDecimal;
import java.util.List;

/**
 * Puerto de entrada (driving port): Casos de uso del catálogo de productos, stock y precios.
 * <p>
 * Los adaptadores de entrada (GUI JavaFX, CLI) dependen de esta abstracción y no del
 * servicio concreto, cumpliendo el Principio de Inversión de Dependencias (DIP).
 */
public interface ProductoUseCase {

    Producto crearProducto(Producto prod);

    Producto actualizarProducto(Producto prod);

    Producto obtenerPorId(String codigoProducto);

    List<Producto> listarTodos();

    boolean eliminarProducto(String codigoProducto);

    List<Producto> buscarPorGama(String gama);

    List<Producto> buscarBajoStock(int umbral);

    List<Producto> buscarPorNombre(String termino);

    long contarProductos();
}
