package com.jardineria.infrastructure.adapter.in.desktop.views;

import com.jardineria.infrastructure.adapter.in.desktop.DesktopServices;
import com.jardineria.infrastructure.adapter.in.desktop.navigation.Navigator;
import com.jardineria.infrastructure.adapter.in.desktop.navigation.Route;

/**
 * Registro de vistas por ruta (fábrica). Cada vista recibe solo los puertos que necesita.
 */
public final class DesktopViews {
    private DesktopViews() {
    }

    public static void registerAll(Navigator navigator, DesktopServices services) {
        for (Route route : Route.values()) {
            navigator.register(route, () -> new PlaceholderView(route));
        }
    }
}
