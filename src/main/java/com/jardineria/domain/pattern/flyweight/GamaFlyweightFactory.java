package com.jardineria.domain.pattern.flyweight;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Fábrica de Flyweights (GoF Flyweight Factory).
 * Gestiona y reutiliza la reserva de objetos GamaFlyweight compartidos.
 *
 * Arquitectura Hexagonal: Capa de dominio pura sin dependencias de frameworks.
 */
public class GamaFlyweightFactory {
    private static final Map<String, GamaFlyweight> POOL = new ConcurrentHashMap<>();

    static {
        // Inicializar flyweights canónicos para las gamas de la base de datos
        registrar("Aromáticas", "Plantas aromáticas culinarias y medicinales", "#10B981", "🌿", "aromaticas.jpg");
        registrar("Frutales", "Árboles y arbustos de producción frutal de huerto", "#F59E0B", "🍎", "frutales.jpg");
        registrar("Herbaceas", "Plantas herbáceas para jardín decorativo y tapizantes", "#3B82F6", "🌱", "herbaceas.jpg");
        registrar("Herramientas", "Herramientas profesionales para poda, labranza y riego", "#6B7280", "🛠️", "herramientas.jpg");
        registrar("Ornamentales", "Flores vistosas y plantas de floración continua para decoración", "#EC4899", "🌺", "ornamentales.jpg");
    }

    private static void registrar(String gama, String desc, String colorHex, String emoji, String url) {
        POOL.put(gama.toLowerCase(), new GamaFlyweight(gama, desc, colorHex, emoji, url));
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
                new GamaFlyweight(gamaName, "Familia botánica de jardinería", "#4F46E5", "🌻", "default.jpg")
        );
    }

    public static int getFlyweightsCount() {
        return POOL.size();
    }
}
