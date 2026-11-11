package com.jardineria.infrastructure.adapter.in.ui.components;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Utilitario para carga y renderizado optimizado de imágenes con escalado bicúbico y caché en memoria.
 */
public final class ImageHelper {
    private static final Map<String, ImageIcon> CACHE = new ConcurrentHashMap<>();

    private ImageHelper() {}

    public static ImageIcon loadScaledIcon(String resourcePath, int width, int height) {
        String key = resourcePath + "@" + width + "x" + height;
        if (CACHE.containsKey(key)) {
            return CACHE.get(key);
        }

        try (InputStream in = ImageHelper.class.getResourceAsStream(resourcePath)) {
            if (in == null) {
                return null;
            }
            BufferedImage original = ImageIO.read(in);
            if (original == null) {
                return null;
            }

            BufferedImage scaled = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2 = scaled.createGraphics();
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Ajuste proporcional
            double rThumb = (double) width / height;
            double rOrig = (double) original.getWidth() / original.getHeight();
            int drawW = width;
            int drawH = height;
            int drawX = 0;
            int drawY = 0;

            if (rThumb > rOrig) {
                drawW = (int) (height * rOrig);
                drawX = (width - drawW) / 2;
            } else {
                drawH = (int) (width / rOrig);
                drawY = (height - drawH) / 2;
            }

            g2.drawImage(original, drawX, drawY, drawW, drawH, null);
            g2.dispose();

            ImageIcon icon = new ImageIcon(scaled);
            CACHE.put(key, icon);
            return icon;
        } catch (Exception e) {
            return null;
        }
    }

    public static JLabel createPatternImageThumbnail(String patternKey, int w, int h) {
        String path = "/images/patterns/" + patternKey.toLowerCase() + ".jpg";
        ImageIcon icon = loadScaledIcon(path, w, h);
        JLabel lbl = new JLabel();
        lbl.setHorizontalAlignment(SwingConstants.CENTER);
        lbl.setPreferredSize(new Dimension(w, h));
        lbl.setBorder(BorderFactory.createLineBorder(new Color(100, 110, 125, 60), 1, true));

        if (icon != null) {
            lbl.setIcon(icon);
        } else {
            lbl.setText("🖼️ " + patternKey);
            lbl.setOpaque(true);
            lbl.setBackground(new Color(30, 41, 59));
            lbl.setForeground(Color.LIGHT_GRAY);
        }
        return lbl;
    }
}
