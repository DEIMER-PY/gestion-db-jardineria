package com.jardineria.infrastructure.adapter.in.desktop.support;

import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.material2.Material2AL;
import org.kordamp.ikonli.material2.Material2MZ;

/**
 * Fábrica de íconos vectoriales (Ikonli). Sustituye por completo a los emojis,
 * que en Windows se renderizaban como cuadros vacíos.
 */
public final class Icons {
    private Icons() {
    }

    public static FontIcon of(Ikon ikon) {
        return new FontIcon(ikon);
    }

    public static FontIcon of(Ikon ikon, int size) {
        FontIcon icon = new FontIcon(ikon);
        icon.setIconSize(size);
        return icon;
    }

    public static FontIcon of(Ikon ikon, int size, String styleClass) {
        FontIcon icon = of(ikon, size);
        icon.getStyleClass().add(styleClass);
        return icon;
    }

    /** Traduce la clave de ícono neutral del dominio (GamaFlyweight.iconKey) a un glifo concreto. */
    public static Ikon forGama(String iconKey) {
        if (iconKey == null) {
            return Material2AL.ECO;
        }
        return switch (iconKey) {
            case "leaf" -> Material2AL.ECO;
            case "apple" -> Material2MZ.NATURE;
            case "sprout" -> Material2AL.GRASS;
            case "tools" -> Material2AL.HANDYMAN;
            case "flower" -> Material2AL.LOCAL_FLORIST;
            default -> Material2MZ.SPA;
        };
    }
}
