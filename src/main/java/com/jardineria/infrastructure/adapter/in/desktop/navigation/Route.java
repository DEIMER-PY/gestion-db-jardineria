package com.jardineria.infrastructure.adapter.in.desktop.navigation;

import javafx.scene.input.KeyCode;
import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.material2.Material2AL;
import com.jardineria.infrastructure.adapter.in.desktop.support.Keys;

/**
 * Rutas de navegación de la aplicación: título, descripción, ícono, sección del menú
 * lateral y atajo de teclado (Ctrl + dígito).
 */
public enum Route {
    DASHBOARD("Panel general", "Pulso operativo del vivero en tiempo real", Feather.GRID, Section.GENERAL, KeyCode.DIGIT1),
    ANALYTICS("Métricas", "Analítica comercial, tendencias y distribución", Feather.BAR_CHART_2, Section.GENERAL, KeyCode.DIGIT2),
    CLIENTES("Clientes", "Cartera, crédito disponible e historial de compra", Feather.USERS, Section.OPERACION, KeyCode.DIGIT3),
    PEDIDOS("Pedidos", "Ciclo de vida de pedidos y sus líneas de detalle", Feather.SHOPPING_BAG, Section.OPERACION, KeyCode.DIGIT4),
    PAGOS("Pagos", "Cobros registrados por cliente y forma de pago", Feather.CREDIT_CARD, Section.OPERACION, KeyCode.DIGIT5),
    PRODUCTOS("Productos", "Catálogo, precios y control de stock", Feather.PACKAGE, Section.CATALOGO, KeyCode.DIGIT6),
    GAMAS("Gamas", "Familias botánicas del catálogo", Material2AL.ECO, Section.CATALOGO, KeyCode.DIGIT7),
    OFICINAS("Oficinas", "Red de oficinas comerciales", Feather.MAP_PIN, Section.ORGANIZACION, KeyCode.DIGIT8),
    EMPLEADOS("Empleados", "Equipo, puestos y jerarquía", Feather.BRIEFCASE, Section.ORGANIZACION, KeyCode.DIGIT9),
    /** Se abre desde el pie del menú lateral, la barra superior o Ctrl+, (no aparece en la lista de módulos). */
    AJUSTES("Ajustes", "Perfiles de apariencia, temas, colores de acento y accesibilidad", Feather.SETTINGS,
            Section.PREFERENCIAS, KeyCode.COMMA);

    private final String title;
    private final String description;
    private final Ikon icon;
    private final Section section;
    private final KeyCode shortcut;

    Route(String title, String description, Ikon icon, Section section, KeyCode shortcut) {
        this.title = title;
        this.description = description;
        this.icon = icon;
        this.section = section;
        this.shortcut = shortcut;
    }

    public String title() {
        return title;
    }

    public String description() {
        return description;
    }

    public Ikon icon() {
        return icon;
    }

    public Section section() {
        return section;
    }

    public KeyCode shortcut() {
        return shortcut;
    }

    /** Texto del atajo para mostrar al usuario, p. ej. "Ctrl 3". */
    public String shortcutLabel() {
        return shortcut == null ? "" : Keys.text("Ctrl ") + keyText();
    }

    /** Tecla del atajo tal como se imprime en el teclado ("3", ","). */
    public String keyText() {
        if (shortcut == null) {
            return "";
        }
        return shortcut == KeyCode.COMMA ? "," : shortcut.getName();
    }

    /** {@code true} si la ruta es un módulo del menú lateral (Ajustes vive en el pie). */
    public boolean inSidebar() {
        return section != Section.PREFERENCIAS;
    }

    /** Secciones del menú lateral. */
    public enum Section {
        GENERAL("General"),
        OPERACION("Operación"),
        CATALOGO("Catálogo"),
        ORGANIZACION("Organización"),
        PREFERENCIAS("Preferencias");

        private final String label;

        Section(String label) {
            this.label = label;
        }

        public String label() {
            return label;
        }
    }
}
