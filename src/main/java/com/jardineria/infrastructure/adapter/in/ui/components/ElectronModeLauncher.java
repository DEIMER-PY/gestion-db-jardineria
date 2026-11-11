package com.jardineria.infrastructure.adapter.in.ui.components;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Desktop;
import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/**
 * Lanzador del Modo de Aplicación de Escritorio enriquecida (Enfoque Híbrido: Servidor Local + WebView / Modo App).
 * Inicia Google Chrome o Chromium en una ventana independiente sin marco de navegador (--app),
 * emulando a la perfección una aplicación de escritorio nativa como Electron o Tauri con aceleración GPU.
 */
public final class ElectronModeLauncher {
    private static final Logger log = LoggerFactory.getLogger(ElectronModeLauncher.class);

    private ElectronModeLauncher() {
    }

    public static void openElectronMode(String url) {
        String profileDir = System.getProperty("java.io.tmpdir") + File.separator + "jardineria-app-profile";

        // Rutas candidatas de Chromium / Chrome en Linux, Windows y macOS
        String[] candidateBinaries = {
                "/usr/bin/google-chrome",
                "/usr/bin/chromium",
                "/usr/bin/chromium-browser",
                "/snap/bin/chromium",
                "google-chrome",
                "chromium",
                "C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe",
                "C:\\Program Files (x86)\\Microsoft\\Edge\\Application\\msedge.exe",
                "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome"
        };

        for (String bin : candidateBinaries) {
            File f = new File(bin);
            if (f.exists() && f.canExecute()) {
                if (launchProcess(bin, url, profileDir)) {
                    log.info("Aplicación de Escritorio iniciada con {}", bin);
                    return;
                }
            }
        }

        // Intento directo por comando en el PATH
        if (launchProcess("google-chrome", url, profileDir)) return;
        if (launchProcess("chromium", url, profileDir)) return;

        // Fallback a navegador por defecto
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(new URI(url));
                log.info("Abierto en navegador del sistema: {}", url);
            }
        } catch (Exception e) {
            log.error("No se pudo abrir el navegador del sistema: {}", e.getMessage());
        }
    }

    private static boolean launchProcess(String binary, String url, String profileDir) {
        try {
            List<String> command = new ArrayList<>();
            command.add(binary);
            command.add("--app=" + url);
            command.add("--window-size=1440,920");
            command.add("--user-data-dir=" + profileDir);
            command.add("--class=JardineriaEnterpriseSuite");
            command.add("--enable-features=OverlayScrollbar");
            command.add("--no-first-run");
            command.add("--no-default-browser-check");

            Process process = new ProcessBuilder(command).start();
            return process.isAlive();
        } catch (Exception e) {
            return false;
        }
    }
}
