package com.jardineria.infrastructure.adapter.in.ui.components;

import java.awt.Desktop;
import java.io.File;
import java.net.URI;

/**
 * Lanzador del Modo Web / Electron.
 * Ejecuta Google Chrome o Chromium con el flag --app para abrir el panel web
 * en una ventana independiente sin marco de navegador, simulando una app de Electron.
 */
public final class ElectronModeLauncher {
    private ElectronModeLauncher() {}

    public static void openElectronMode(String url) {
        try {
            File chrome = new File("/usr/bin/google-chrome");
            if (chrome.exists() && chrome.canExecute()) {
                new ProcessBuilder("/usr/bin/google-chrome", "--app=" + url).start();
                return;
            }

            File chromium = new File("/usr/bin/chromium");
            if (chromium.exists() && chromium.canExecute()) {
                new ProcessBuilder("/usr/bin/chromium", "--app=" + url).start();
                return;
            }

            File chromiumBrowser = new File("/usr/bin/chromium-browser");
            if (chromiumBrowser.exists() && chromiumBrowser.canExecute()) {
                new ProcessBuilder("/usr/bin/chromium-browser", "--app=" + url).start();
                return;
            }

            // Fallback a Desktop.browse
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(new URI(url));
            }
        } catch (Exception e) {
            try {
                if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                    Desktop.getDesktop().browse(new URI(url));
                }
            } catch (Exception ignored) {
            }
        }
    }
}
