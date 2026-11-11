package com.jardineria.infrastructure.adapter.in.ui.components;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Componente Toast no bloqueante con animación de progreso, icono vectorial y estética translúcida.
 */
public class ToastNotification extends JPanel {

    public enum ToastType {
        SUCCESS(new Color(16, 185, 129), VectorIcons.IconType.CHECK, "Éxito"),
        INFO(new Color(59, 130, 246), VectorIcons.IconType.INFO, "Información"),
        WARNING(new Color(245, 158, 11), VectorIcons.IconType.ALERT_TRIANGLE, "Advertencia"),
        ERROR(new Color(239, 68, 68), VectorIcons.IconType.ALERT_CIRCLE, "Error");

        public final Color accentColor;
        public final VectorIcons.IconType iconType;
        public final String defaultTitle;

        ToastType(Color accentColor, VectorIcons.IconType iconType, String defaultTitle) {
            this.accentColor = accentColor;
            this.iconType = iconType;
            this.defaultTitle = defaultTitle;
        }
    }

    private final ToastType type;
    private final String title;
    private final String message;
    private final Runnable onClose;
    private float progress = 1.0f;
    private Timer dismissTimer;

    public ToastNotification(ToastType type, String title, String message, int durationMs, Runnable onClose) {
        this.type = type;
        this.title = (title != null && !title.isBlank()) ? title : type.defaultTitle;
        this.message = message;
        this.onClose = onClose;

        setLayout(new BorderLayout(10, 6));
        setOpaque(false);
        setBorder(new EmptyBorder(10, 14, 8, 12));
        setPreferredSize(new Dimension(340, 72));
        setMaximumSize(new Dimension(380, 80));

        initUI();
        startTimer(durationMs);
    }

    private void initUI() {
        // Icono a la izquierda
        JLabel lblIcon = new JLabel(VectorIcons.get(type.iconType, 20, type.accentColor));
        lblIcon.setVerticalAlignment(SwingConstants.TOP);
        add(lblIcon, BorderLayout.WEST);

        // Texto Central (Título y Mensaje)
        JPanel textPanel = new JPanel();
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
        textPanel.setOpaque(false);

        JLabel lblTitle = new JLabel(title);
        lblTitle.setFont(new Font("SansSerif", Font.BOLD, 12));
        lblTitle.setForeground(ThemeManager.isDarkMode() ? Color.WHITE : new Color(15, 23, 42));

        JLabel lblMsg = new JLabel("<html><body style='width: 230px;'>" + message + "</body></html>");
        lblMsg.setFont(new Font("SansSerif", Font.PLAIN, 11));
        lblMsg.setForeground(ThemeManager.isDarkMode() ? new Color(180, 185, 195) : new Color(71, 85, 105));

        textPanel.add(lblTitle);
        textPanel.add(Box.createVerticalStrut(2));
        textPanel.add(lblMsg);
        add(textPanel, BorderLayout.CENTER);

        // Botón Cerrar a la derecha
        JLabel btnClose = new JLabel(VectorIcons.get(VectorIcons.IconType.CLOSE, 14, new Color(150, 155, 165)));
        btnClose.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnClose.setVerticalAlignment(SwingConstants.TOP);
        btnClose.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                closeToast();
            }
        });
        add(btnClose, BorderLayout.EAST);
    }

    private void startTimer(int durationMs) {
        final int interval = 30;
        final float step = (float) interval / durationMs;
        dismissTimer = new Timer(interval, e -> {
            progress -= step;
            if (progress <= 0) {
                progress = 0;
                dismissTimer.stop();
                closeToast();
            }
            repaint();
        });
        dismissTimer.start();
    }

    private void closeToast() {
        if (dismissTimer != null && dismissTimer.isRunning()) {
            dismissTimer.stop();
        }
        if (onClose != null) {
            onClose.run();
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();

        // Fondo con sombra y bordes redondeados
        Color bg = ThemeManager.isDarkMode()
                ? new Color(24, 30, 46, 235)
                : new Color(255, 255, 255, 240);
        g2.setColor(bg);
        g2.fillRoundRect(0, 0, w, h, 14, 14);

        // Borde exterior
        Color border = ThemeManager.isDarkMode()
                ? new Color(255, 255, 255, 30)
                : new Color(0, 0, 0, 30);
        g2.setColor(border);
        g2.drawRoundRect(0, 0, w - 1, h - 1, 14, 14);

        // Barra de acento lateral izquierda
        g2.setColor(type.accentColor);
        g2.fillRoundRect(0, 0, 4, h, 4, 4);

        // Barra de progreso inferior tipo Toastr
        int progressW = (int) (w * Math.max(0, Math.min(1.0f, progress)));
        g2.setColor(new Color(type.accentColor.getRed(), type.accentColor.getGreen(), type.accentColor.getBlue(), 160));
        g2.fillRoundRect(4, h - 3, progressW, 3, 2, 2);

        g2.dispose();
        super.paintComponent(g);
    }
}
