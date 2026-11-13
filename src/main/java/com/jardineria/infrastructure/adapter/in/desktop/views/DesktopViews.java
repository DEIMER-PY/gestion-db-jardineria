package com.jardineria.infrastructure.adapter.in.desktop.views;

import com.jardineria.infrastructure.adapter.in.desktop.DesktopServices;
import com.jardineria.infrastructure.adapter.in.desktop.navigation.Navigator;
import com.jardineria.infrastructure.adapter.in.desktop.navigation.Route;
import com.jardineria.infrastructure.adapter.in.desktop.views.agenda.AgendaView;
import com.jardineria.infrastructure.adapter.in.desktop.views.analytics.AnalyticsView;
import com.jardineria.infrastructure.adapter.in.desktop.views.assistant.AsistenteView;
import com.jardineria.infrastructure.adapter.in.desktop.views.board.BoardView;
import com.jardineria.infrastructure.adapter.in.desktop.views.chat.ChatView;
import com.jardineria.infrastructure.adapter.in.desktop.views.dashboard.DashboardView;
import com.jardineria.infrastructure.adapter.in.desktop.views.modules.ClientesView;
import com.jardineria.infrastructure.adapter.in.desktop.views.modules.EmpleadosView;
import com.jardineria.infrastructure.adapter.in.desktop.views.modules.GamasView;
import com.jardineria.infrastructure.adapter.in.desktop.views.modules.OficinasView;
import com.jardineria.infrastructure.adapter.in.desktop.views.modules.PagosView;
import com.jardineria.infrastructure.adapter.in.desktop.views.modules.PedidosView;
import com.jardineria.infrastructure.adapter.in.desktop.views.modules.ProductosView;
import com.jardineria.infrastructure.adapter.in.desktop.views.settings.SettingsView;

/**
 * Registro de vistas por ruta (fábrica). Cada vista recibe solo los puertos que necesita.
 */
public final class DesktopViews {
    private DesktopViews() {
    }

    public static void registerAll(Navigator navigator, DesktopServices services) {
        navigator.register(Route.DASHBOARD, () -> new DashboardView(services, navigator::navigate));
        navigator.register(Route.ANALYTICS, () -> new AnalyticsView(services.analytics(), navigator::navigate));
        navigator.register(Route.ASISTENTE, () -> new AsistenteView(services.asistente(), navigator::navigate));
        navigator.register(Route.CLIENTES, () -> new ClientesView(services.clientes(), services.empleados(),
                services.analytics(), services.pedidos(), services.pagos()));
        navigator.register(Route.PEDIDOS, () -> new PedidosView(services.pedidos(), services.detalles(),
                services.clientes(), services.productos()));
        navigator.register(Route.PAGOS, () -> new PagosView(services.pagos(), services.clientes()));
        navigator.register(Route.TABLERO, () -> new BoardView(services.pedidos(), services.clientes(), navigator::navigate));
        navigator.register(Route.AGENDA, () -> new AgendaView(services.pedidos(), services.pagos(), services.clientes(),
                navigator::navigate));
        navigator.register(Route.PRODUCTOS, () -> new ProductosView(services.productos(), services.gamas()));
        navigator.register(Route.GAMAS, () -> new GamasView(services.gamas(), services.productos()));
        navigator.register(Route.OFICINAS, () -> new OficinasView(services.oficinas(), services.empleados()));
        navigator.register(Route.EMPLEADOS, () -> new EmpleadosView(services.empleados(), services.oficinas(), services.clientes()));
        navigator.register(Route.CHAT, () -> new ChatView(services.mensajeria(), services.empleados(), navigator::navigate));
        navigator.register(Route.AJUSTES, SettingsView::new);
    }
}
