package com.jardineria.infrastructure.adapter.in.desktop.views;

import com.jardineria.infrastructure.adapter.in.desktop.components.EmptyState;
import com.jardineria.infrastructure.adapter.in.desktop.navigation.Route;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.layout.StackPane;

/**
 * Vista provisional para rutas cuyo módulo aún no está implementado.
 */
class PlaceholderView extends BaseView {

    PlaceholderView(Route route) {
        super(route);
    }

    @Override
    protected Node buildContent() {
        StackPane box = new StackPane(new EmptyState(route.icon(), route.title(),
                "Este módulo se incorpora en una rama posterior."));
        box.setAlignment(Pos.CENTER);
        box.setMinHeight(420);
        return box;
    }

    @Override
    public void refresh() {
        // Sin datos que cargar
    }
}
