package com.jardineria.application.port.in;

import com.jardineria.domain.model.GamaProducto;
import java.util.List;

/**
 * Puerto de entrada (driving port): Casos de uso de las gamas (familias) de producto.
 * <p>
 * Los adaptadores de entrada (GUI JavaFX, CLI) dependen de esta abstracción y no del
 * servicio concreto, cumpliendo el Principio de Inversión de Dependencias (DIP).
 */
public interface GamaProductoUseCase {

    GamaProducto crearGama(GamaProducto gama);

    GamaProducto actualizarGama(GamaProducto gama);

    GamaProducto obtenerPorId(String gama);

    List<GamaProducto> listarTodas();

    boolean eliminarGama(String gama);

    long contarGamas();
}
