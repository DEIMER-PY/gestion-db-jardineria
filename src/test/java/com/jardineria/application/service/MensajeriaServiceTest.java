package com.jardineria.application.service;

import com.jardineria.application.port.in.MensajeriaUseCase.Sondeo;
import com.jardineria.domain.exception.BusinessValidationException;
import com.jardineria.domain.model.Empleado;
import com.jardineria.domain.model.chat.Canal;
import com.jardineria.domain.model.chat.Mensaje;
import com.jardineria.domain.repository.EmpleadoRepository;
import com.jardineria.domain.repository.MensajeriaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MensajeriaServiceTest {
    private static final Canal GENERAL = new Canal(1, "general", Canal.Tipo.GENERAL, null, "Todos");
    private static final Canal OFICINA = new Canal(2, "oficina-tal-es", Canal.Tipo.OFICINA, "TAL-ES", "Talavera");
    private static final Canal DM_3_5 = new Canal(3, "dm-3-5", Canal.Tipo.DIRECTO, null, null);
    private static final Canal DM_4_7 = new Canal(4, "dm-4-7", Canal.Tipo.DIRECTO, null, null);

    private final MensajeriaRepository repo = mock(MensajeriaRepository.class);
    private final EmpleadoRepository empleados = mock(EmpleadoRepository.class);
    private final MensajeriaService service = new MensajeriaService(repo, empleados);

    @BeforeEach
    void setUp() {
        when(repo.canales()).thenReturn(List.of(DM_4_7, OFICINA, DM_3_5, GENERAL));
        when(empleados.findById(anyInt())).thenAnswer(i -> Optional.of(new Empleado(i.getArgument(0), "N", "A", null,
                "1", "x@y.es", "TAL-ES", null, null)));
    }

    @Test
    @DisplayName("Cada empleado ve el general, las oficinas y solo sus conversaciones directas, en ese orden")
    void canalesVisibles() {
        assertThat(service.canales(3)).containsExactly(GENERAL, OFICINA, DM_3_5);
        assertThat(service.canales(7)).containsExactly(GENERAL, OFICINA, DM_4_7);
    }

    @Test
    @DisplayName("El canal directo se reutiliza si existe y se crea si no; no se puede abrir con uno mismo")
    void canalDirecto() {
        when(repo.canalPorNombre("dm-3-5")).thenReturn(Optional.of(DM_3_5));
        assertThat(service.canalDirecto(5, 3)).isEqualTo(DM_3_5);
        verify(repo, never()).crearCanal(anyString(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());

        Canal nuevo = new Canal(9, "dm-3-8", Canal.Tipo.DIRECTO, null, null);
        when(repo.canalPorNombre("dm-3-8")).thenReturn(Optional.empty());
        when(repo.crearCanal("dm-3-8", Canal.Tipo.DIRECTO, null)).thenReturn(nuevo);
        assertThat(service.canalDirecto(3, 8)).isEqualTo(nuevo);

        assertThatThrownBy(() -> service.canalDirecto(3, 3)).isInstanceOf(BusinessValidationException.class);
        when(empleados.findById(99)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.canalDirecto(3, 99)).hasMessageContaining("99");
    }

    @Test
    @DisplayName("Enviar recorta el texto, lo valida, comprueba que participas y marca el canal como leído")
    void enviar() {
        Mensaje m = new Mensaje(40, 1, 3, "Hola", LocalDateTime.now());
        when(repo.publicar(1, 3, "Hola")).thenReturn(m);
        assertThat(service.enviar(1, 3, "  Hola \n")).isEqualTo(m);
        verify(repo).marcarLeido(1, 3, 40);

        assertThatThrownBy(() -> service.enviar(1, 3, "   ")).isInstanceOf(BusinessValidationException.class);
        assertThatThrownBy(() -> service.enviar(1, 3, "x".repeat(Mensaje.MAX_TEXTO + 1))).hasMessageContaining("2000");
        assertThatThrownBy(() -> service.enviar(4, 3, "Hola")).hasMessageContaining("No participas");
        assertThatThrownBy(() -> service.enviar(77, 3, "Hola")).hasMessageContaining("ya no existe");
    }

    @Test
    @DisplayName("El sondeo descarta mensajes y pendientes de conversaciones ajenas")
    void sondeo() {
        when(repo.posteriores(10, MensajeriaService.MAX_SONDEO)).thenReturn(List.of(
                new Mensaje(11, 1, 5, "general", null),
                new Mensaje(12, 4, 7, "privado ajeno", null),
                new Mensaje(13, 3, 5, "para ti", null)));
        when(repo.noLeidos(3)).thenReturn(Map.of(1, 2L, 4, 1L, 3, 1L));

        Sondeo s = service.sondear(3, 10);

        assertThat(s.nuevos()).extracting(Mensaje::id).containsExactly(11L, 13L);
        assertThat(s.noLeidos()).containsOnly(Map.entry(1, 2L), Map.entry(3, 1L));
        assertThat(s.canales()).containsExactly(GENERAL, OFICINA, DM_3_5);
        assertThat(s.ultimoId(10)).isEqualTo(13);
        assertThat(new Sondeo(List.of(), Map.of(), List.of()).ultimoId(10)).isEqualTo(10);
    }

    @Test
    @DisplayName("Marcar como leído ignora ids no válidos")
    void marcarLeido() {
        service.marcarLeido(1, 3, 0);
        verify(repo, never()).marcarLeido(anyInt(), anyInt(), anyLong());
    }
}
