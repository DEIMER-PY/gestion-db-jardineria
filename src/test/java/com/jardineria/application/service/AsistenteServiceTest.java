package com.jardineria.application.service;

import com.jardineria.application.dto.DashboardMetrics;
import com.jardineria.application.port.in.AnalyticsUseCase;
import com.jardineria.application.port.in.AsistenteUseCase.Respuesta;
import com.jardineria.application.port.in.AsistenteUseCase.Turno;
import com.jardineria.application.port.in.ClienteUseCase;
import com.jardineria.application.port.in.DashboardUseCase;
import com.jardineria.application.port.in.DetallePedidoUseCase;
import com.jardineria.application.port.in.PedidoUseCase;
import com.jardineria.application.port.in.ProductoUseCase;
import com.jardineria.application.port.out.DocumentacionPort;
import com.jardineria.application.port.out.ModeloLenguajePort;
import com.jardineria.application.port.out.ModeloLenguajePort.Herramienta;
import com.jardineria.application.port.out.ModeloLenguajePort.Llamada;
import com.jardineria.application.port.out.ModeloLenguajePort.Mensaje;
import com.jardineria.application.port.out.ModeloLenguajePort.Rol;
import com.jardineria.domain.model.Pedido;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.RETURNS_SMART_NULLS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AsistenteServiceTest {

    /** Modelo guionizado: devuelve las respuestas en orden y guarda lo que recibió. */
    static final class ModeloGuion implements ModeloLenguajePort {
        final Deque<Respuesta2> guion = new ArrayDeque<>();
        final List<List<Mensaje>> recibidos = new ArrayList<>();
        boolean configurado = true;

        record Respuesta2(String texto, List<Llamada> llamadas) {
        }

        ModeloGuion llama(String herramienta, Map<String, Object> args) {
            guion.add(new Respuesta2(null, List.of(new Llamada("c" + guion.size(), herramienta, args))));
            return this;
        }

        ModeloGuion responde(String texto) {
            guion.add(new Respuesta2(texto, List.of()));
            return this;
        }

        @Override
        public boolean configurado() {
            return configurado;
        }

        @Override
        public String descripcion() {
            return "Guion · prueba";
        }

        @Override
        public ModeloLenguajePort.Respuesta completar(List<Mensaje> mensajes, List<Herramienta> herramientas) {
            recibidos.add(List.copyOf(mensajes));
            Respuesta2 r = guion.isEmpty() ? new Respuesta2(null, List.of(new Llamada("x", "metricas_generales", Map.of())))
                    : guion.poll();
            return new ModeloLenguajePort.Respuesta(r.texto(), r.llamadas());
        }

        String ultimoResultado() {
            List<Mensaje> last = recibidos.get(recibidos.size() - 1);
            return last.get(last.size() - 1).contenido();
        }
    }

    private final ModeloGuion modelo = new ModeloGuion();
    private final PedidoUseCase pedidos = mock(PedidoUseCase.class, RETURNS_SMART_NULLS);
    private final DashboardUseCase dashboard = mock(DashboardUseCase.class, RETURNS_SMART_NULLS);
    private final DocumentacionPort docs = () -> List.of(new DocumentacionPort.Documento("docs/APARIENCIA.md",
            "# Apariencia\n\n## Temas\n\nCambia el tema en Ajustes o con Ctrl+, desde cualquier pantalla.\n"));
    private final AsistenteService service = new AsistenteService(modelo, docs,
            mock(ClienteUseCase.class, RETURNS_SMART_NULLS), pedidos, mock(DetallePedidoUseCase.class, RETURNS_SMART_NULLS),
            mock(ProductoUseCase.class, RETURNS_SMART_NULLS), dashboard, mock(AnalyticsUseCase.class, RETURNS_SMART_NULLS),
            Clock.fixed(Instant.parse("2026-11-13T09:00:00Z"), ZoneOffset.UTC));

    @Test
    @DisplayName("Ejecuta la herramienta que pide el modelo y le devuelve el resultado antes de la respuesta final")
    void bucleDeHerramientas() {
        when(pedidos.buscarPorEstado("Pendiente")).thenReturn(List.of(
                new Pedido(14, LocalDate.of(2009, 1, 3), LocalDate.of(2009, 1, 9), null, "Pendiente", null, 3),
                new Pedido(20, LocalDate.of(2009, 2, 1), LocalDate.of(2009, 2, 8), null, "Pendiente", null, 5)));
        modelo.llama("buscar_pedidos", Map.of("estado", "Pendiente")).responde("Hay 2 pedidos pendientes: #20 y #14.");

        Respuesta r = service.preguntar(List.of(), "¿Qué pedidos están pendientes?");

        assertThat(r.texto()).isEqualTo("Hay 2 pedidos pendientes: #20 y #14.");
        assertThat(r.herramientas()).containsExactly("buscar_pedidos");
        List<Mensaje> segunda = modelo.recibidos.get(1);
        Mensaje resultado = segunda.get(segunda.size() - 1);
        assertThat(resultado.rol()).isEqualTo(Rol.HERRAMIENTA);
        assertThat(resultado.idLlamada()).isEqualTo("c0");
        assertThat(resultado.contenido()).contains("2 pedidos", "#20", "#14");
        assertThat(resultado.contenido().indexOf("#20")).isLessThan(resultado.contenido().indexOf("#14"));
        assertThat(segunda.get(0).contenido()).contains("2026-11-13", "en español");
    }

    @Test
    @DisplayName("La búsqueda en la documentación devuelve fragmentos con su fuente y la respuesta la cita")
    void ragConFuentes() {
        modelo.llama("buscar_documentacion", Map.of("consulta", "cambiar tema")).responde("Ve a Ajustes.");

        Respuesta r = service.preguntar(List.of(), "¿Cómo cambio el tema?");

        assertThat(modelo.recibidos.get(1).get(modelo.recibidos.get(1).size() - 1).contenido())
                .contains("[Fuente: docs/APARIENCIA.md · Temas]", "Ctrl+,");
        assertThat(r.fuentes()).containsExactly("docs/APARIENCIA.md · Temas");
    }

    @Test
    @DisplayName("Un fallo de la herramienta vuelve al modelo como texto; herramientas desconocidas se rechazan")
    void erroresDeHerramienta() {
        when(dashboard.obtenerMetricas()).thenThrow(new IllegalStateException("MySQL no responde"));
        modelo.llama("metricas_generales", Map.of()).llama("borrar_todo", Map.of()).responde("No pude consultarlo.");

        Respuesta r = service.preguntar(List.of(), "Resumen");

        assertThat(modelo.recibidos.get(1).get(modelo.recibidos.get(1).size() - 1).contenido()).isEqualTo("Error: MySQL no responde");
        assertThat(modelo.ultimoResultado()).contains("no existe");
        assertThat(r.texto()).isEqualTo("No pude consultarlo.");
    }

    @Test
    @DisplayName("Corta el bucle tras el máximo de iteraciones y conserva el historial previo")
    void limiteDeIteraciones() {
        when(dashboard.obtenerMetricas()).thenReturn(new DashboardMetrics(1, 2, 3, 4, 5, BigDecimal.TEN, 1, 1, 0, 0));

        Respuesta r = service.preguntar(List.of(new Turno(true, "Hola"), new Turno(false, "¿Qué necesitas?")), "Insiste");

        assertThat(modelo.recibidos).hasSize(AsistenteService.MAX_ITERACIONES);
        assertThat(r.texto()).contains("demasiadas consultas");
        assertThat(modelo.recibidos.get(0)).extracting(Mensaje::rol)
                .containsExactly(Rol.SISTEMA, Rol.USUARIO, Rol.ASISTENTE, Rol.USUARIO);
    }

    @Test
    @DisplayName("Sin clave no llama al modelo y lo indica")
    void sinConfigurar() {
        modelo.configurado = false;
        assertThat(service.disponible()).isFalse();
        assertThat(service.descripcionProveedor()).contains("GROQ_API_KEY");
        assertThatThrownBy(() -> service.preguntar(List.of(), "Hola")).isInstanceOf(IllegalStateException.class);
        assertThat(modelo.recibidos).isEmpty();
        verify(dashboard, never()).obtenerMetricas();
    }

    @Test
    @DisplayName("Los importes se formatean en euros con coma decimal")
    void euros() {
        assertThat(AsistenteService.euros(new BigDecimal("1234.5"))).isEqualTo("1 234,50");
        assertThat(AsistenteService.euros(null)).isEqualTo("0,00");
    }
}
