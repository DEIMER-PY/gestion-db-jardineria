package com.jardineria.infrastructure.adapter.in.desktop;

import com.jardineria.application.port.in.EmpleadoUseCase;
import com.jardineria.domain.model.Empleado;
import com.jardineria.infrastructure.adapter.in.desktop.navigation.Route;
import com.jardineria.infrastructure.adapter.in.desktop.shell.AppShell;
import com.jardineria.infrastructure.adapter.in.desktop.theme.ThemeManager;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;
import org.testfx.util.WaitForAsyncUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Ficha de empleado como organigrama: cadena de mando, equipo y navegación entre personas. */
@ExtendWith(ApplicationExtension.class)
class OrgChartTest {

    private AppShell shell;
    private Scene scene;

    @Start
    void start(Stage stage) {
        EmpleadoUseCase empleados = mock(EmpleadoUseCase.class, Mockito.RETURNS_SMART_NULLS);
        when(empleados.listarTodos()).thenReturn(List.of(
                new Empleado(1, "Marcos", "Magaña", "Pérez", "3897", "marcos@jardineria.es", "TAL-ES", null, "Director General"),
                new Empleado(2, "Ruben", "López", "Martinez", "2899", "rlopez@jardineria.es", "TAL-ES", 1, "Subdirector Marketing"),
                new Empleado(3, "Alberto", "Soria", "Carrasco", "2837", "asoria@jardineria.es", "TAL-ES", 2, "Subdirector Ventas"),
                new Empleado(4, "Maria", "Solís", "Jerez", "2847", "msolis@jardineria.es", "TAL-ES", 3, "Secretaria"),
                new Empleado(5, "Felipe", "Rosas", "Marquez", "2844", "frosas@jardineria.es", "TAL-ES", 4, "Representante Ventas")));
        shell = new AppShell(FxTestSupport.services(empleados));
        scene = new Scene(shell, 1280, 800);
        ThemeManager.install(scene);
        stage.setScene(scene);
        stage.show();
    }

    @Test
    @DisplayName("La ficha muestra la cadena de mando y el equipo, y activar una persona del equipo abre su ficha")
    void organigramaNavegable() throws Exception {
        WaitForAsyncUtils.asyncFx(() -> shell.getNavigator().navigate(Route.EMPLEADOS, "3")).get();
        FxTestSupport.settle(600);

        assertThat(selected().getCodigoEmpleado()).isEqualTo(3);
        assertThat(texts()).contains("Cadena de mando (2 niveles)", "Equipo directo (1) · 2 en total",
                "Esta persona", "Secretaria · 1 persona a cargo");

        WaitForAsyncUtils.asyncFx(() -> {
            Node row = scene.getRoot().lookupAll(".timeline-row.interactive-row").stream()
                    .filter(n -> n.getAccessibleText() != null && n.getAccessibleText().contains("Maria"))
                    .findFirst().orElseThrow();
            row.fireEvent(new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.ENTER, false, false, false, false));
        }).get();
        FxTestSupport.settle(200);

        assertThat(selected().getCodigoEmpleado()).isEqualTo(4);
        assertThat(texts()).contains("Cadena de mando (3 niveles)", "Equipo directo (1)");
    }

    @SuppressWarnings("unchecked")
    private Empleado selected() throws Exception {
        return WaitForAsyncUtils.asyncFx(() -> ((TableView<Empleado>) scene.getRoot().lookup(".crud-table"))
                .getSelectionModel().getSelectedItem()).get();
    }

    private List<String> texts() throws Exception {
        return WaitForAsyncUtils.asyncFx(() -> scene.getRoot().lookupAll(".detail-content .label").stream()
                .map(n -> ((Label) n).getText()).toList()).get();
    }
}
