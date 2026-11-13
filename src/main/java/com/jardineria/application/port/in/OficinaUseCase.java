package com.jardineria.application.port.in;

import com.jardineria.domain.model.Oficina;
import java.util.List;

/**
 * Puerto de entrada (driving port): Casos de uso de las oficinas de la red comercial.
 * <p>
 * Los adaptadores de entrada (GUI JavaFX, CLI) dependen de esta abstracción y no del
 * servicio concreto, cumpliendo el Principio de Inversión de Dependencias (DIP).
 */
public interface OficinaUseCase {

    Oficina crearOficina(Oficina oficina);

    Oficina actualizarOficina(Oficina oficina);

    Oficina obtenerPorId(String codigoOficina);

    List<Oficina> listarTodas();

    boolean eliminarOficina(String codigoOficina);

    List<Oficina> buscarPorPais(String pais);

    List<Oficina> buscarPorCiudad(String ciudad);

    long contarOficinas();
}
