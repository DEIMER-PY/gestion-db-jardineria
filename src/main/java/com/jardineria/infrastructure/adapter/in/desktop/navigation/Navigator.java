package com.jardineria.infrastructure.adapter.in.desktop.navigation;

import com.jardineria.infrastructure.adapter.in.desktop.support.Motion;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.scene.Node;
import javafx.scene.layout.StackPane;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Enrutador del shell: crea las vistas de forma perezosa (solo al visitarlas),
 * las reutiliza, anima la transición y mantiene un historial para "Atrás" (Alt+←).
 */
public class Navigator {
    private final StackPane host;
    private final Map<Route, Supplier<View>> factories = new EnumMap<>(Route.class);
    private final Map<Route, View> cache = new EnumMap<>(Route.class);
    private final Deque<Route> history = new ArrayDeque<>();
    private final ReadOnlyObjectWrapper<Route> current = new ReadOnlyObjectWrapper<>();

    public Navigator(StackPane host) {
        this.host = host;
    }

    public void register(Route route, Supplier<View> factory) {
        factories.put(route, factory);
    }

    public ReadOnlyObjectProperty<Route> currentProperty() {
        return current.getReadOnlyProperty();
    }

    public Route getCurrent() {
        return current.get();
    }

    public void navigate(Route route) {
        navigate(route, true);
    }

    private void navigate(Route route, boolean recordHistory) {
        if (route == null || route == current.get()) {
            return;
        }
        View view = viewFor(route);
        if (view == null) {
            return;
        }
        if (recordHistory && current.get() != null) {
            history.push(current.get());
            if (history.size() > 30) {
                history.removeLast();
            }
        }
        Node node = view.getNode();
        host.getChildren().setAll(node);
        Motion.enter(node);
        current.set(route);
        view.onShow();
    }

    /** Vuelve a la vista anterior del historial. */
    public boolean back() {
        if (history.isEmpty()) {
            return false;
        }
        navigate(history.pop(), false);
        return true;
    }

    public void refreshCurrent() {
        View view = currentView();
        if (view != null) {
            view.refresh();
        }
    }

    public View currentView() {
        return current.get() == null ? null : cache.get(current.get());
    }

    /** Devuelve (creándola si hace falta) la vista de una ruta. */
    public View viewFor(Route route) {
        View cached = cache.get(route);
        if (cached != null) {
            return cached;
        }
        Supplier<View> factory = factories.get(route);
        if (factory == null) {
            return null;
        }
        View created = factory.get();
        cache.put(route, created);
        return created;
    }
}
