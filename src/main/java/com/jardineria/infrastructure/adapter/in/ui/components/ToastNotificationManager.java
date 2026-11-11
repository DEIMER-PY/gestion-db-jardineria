package com.jardineria.infrastructure.adapter.in.ui.components;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Gestor centralizado de notificaciones flotantes tipo Toast (no bloqueantes).
 * Permite superponer alertas animadas en la esquina inferior derecha de la ventana principal.
 */
public final class ToastNotificationManager {
    private static JFrame targetFrame;
    private static JPanel toastContainer;
    private static final List<ToastNotification> activeToasts = new ArrayList<>();

    private ToastNotificationManager() {
    }

    public static synchronized void initialize(JFrame frame) {
        targetFrame = frame;
        JLayeredPane layeredPane = frame.getLayeredPane();

        toastContainer = new JPanel();
        toastContainer.setLayout(new BoxLayout(toastContainer, BoxLayout.Y_AXIS));
        toastContainer.setOpaque(false);

        updateContainerBounds();
        layeredPane.add(toastContainer, JLayeredPane.POPUP_LAYER);

        frame.addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentResized(java.awt.event.ComponentEvent e) {
                updateContainerBounds();
            }
        });
    }

    private static void updateContainerBounds() {
        if (targetFrame == null || toastContainer == null) return;
        int frameW = targetFrame.getWidth();
        int frameH = targetFrame.getHeight();
        int containerW = 360;
        int containerH = 320;
        int x = frameW - containerW - 24;
        int y = frameH - containerH - 45;
        toastContainer.setBounds(Math.max(0, x), Math.max(0, y), containerW, containerH);
    }

    public static void showSuccess(String title, String message) {
        show(ToastNotification.ToastType.SUCCESS, title, message, 3500);
    }

    public static void showInfo(String title, String message) {
        show(ToastNotification.ToastType.INFO, title, message, 3500);
    }

    public static void showWarning(String title, String message) {
        show(ToastNotification.ToastType.WARNING, title, message, 4500);
    }

    public static void showError(String title, String message) {
        show(ToastNotification.ToastType.ERROR, title, message, 5000);
    }

    public static void show(ToastNotification.ToastType type, String title, String message, int durationMs) {
        SwingUtilities.invokeLater(() -> {
            if (toastContainer == null) return;

            final ToastNotification[] ref = new ToastNotification[1];
            ToastNotification toast = new ToastNotification(type, title, message, durationMs, () -> {
                removeToast(ref[0]);
            });
            ref[0] = toast;

            activeToasts.add(toast);
            toastContainer.add(toast);
            toastContainer.add(Box.createVerticalStrut(8));
            toastContainer.revalidate();
            toastContainer.repaint();
        });
    }

    private static synchronized void removeToast(ToastNotification toast) {
        if (toast == null || toastContainer == null) return;
        SwingUtilities.invokeLater(() -> {
            activeToasts.remove(toast);
            toastContainer.remove(toast);
            toastContainer.revalidate();
            toastContainer.repaint();
        });
    }
}
