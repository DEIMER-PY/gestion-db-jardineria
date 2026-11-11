package com.jardineria.domain.pattern.flyweight;

import java.awt.Color;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Fábrica de Flyweights (GoF Flyweight Factory).
 * Gestiona y reutiliza la reserva de objetos GamaFlyweight compartidos.
 */
public class GamaFlyweightFactory {
    private static final Map<String, GamaFlyweight> POOL = new ConcurrentHashMap<>();

    static {
        // Inicializar flyweights canónicos para las gamas de la base de datos
        registrar("Aromáticas", "Plantas aromáticas culinarias y medicinales", new Color(16, 185, 129), "🌿", "aromaticas.jpg");
        registrar("Frutales", "Árboles y arbustos de producción frutal de huerto", new Color(245, 158, 11), "🍎", "frutales.jpg");
        registrar("Herbaceas", "Plantas herbáceas para jardín decorativo y tapizantes", new Color(59, 130, 246), "🌱", "herbaceas.jpg");
        registrar("Herramientas", "Herramientas profesionales para poda, labranza y riego", new Color(107, 114, 128), "🛠️", "herramientas.jpg");
        registrar("Ornamentales", "Flores vistosas y plantas de floración continua para decoración", new Color(236, 72, 153), "🌺", "ornamentales.jpg");
    }

    private static void registrar(String gama, String desc, Color color, String emoji, String url) {
        POOL.put(gama.toLowerCase(), new GamaFlyweight(gama, desc, color, emoji, url));
    }

    public static GamaFlyweight getFlyweight(String gamaName) {
        if (gamaName == null || gamaName.isBlank()) {
            return POOL.get("herbaceas");
        }
        String key = gamaName.trim().toLowerCase();
        // Mapear variaciones con o sin tildes
        if (key.contains("aromatica")) key = "aromáticas";
        if (key.contains("herbacea")) key = "herbaceas";

        return POOL.computeIfAbsent(key, k ->
                new GamaFlyweight(gamaName, "Familia botánica de jardinería", new Color(79, 70, 229), "🌻", "default.jpg")
        );
    }

    public static int getFlyweightsCount() {
        return POOL.size();
    }
}
