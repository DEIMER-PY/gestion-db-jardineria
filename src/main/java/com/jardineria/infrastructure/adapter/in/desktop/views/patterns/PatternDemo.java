package com.jardineria.infrastructure.adapter.in.desktop.views.patterns;

import com.jardineria.infrastructure.adapter.in.desktop.components.KpiCard;
import javafx.scene.Node;
import org.kordamp.ikonli.Ikon;

import java.util.List;

/**
 * Ficha interactiva de un patrón GoF (estrategia intercambiable de la vista de patrones).
 * Cada implementación describe el patrón y construye una demo que usa las clases reales
 * del dominio con datos de la base de datos.
 */
interface PatternDemo {

    /** Nombre canónico del patrón ("Decorator", "Adapter"…). */
    String name();

    Ikon icon();

    KpiCard.Tone tone();

    /** Frase corta que resume el problema que resuelve. */
    String tagline();

    /** Intención del patrón aplicada al negocio de la jardinería. */
    String intent();

    /** Clases del repositorio que participan, en orden de lectura. */
    List<String> participants();

    /** Dónde se aplica dentro de la aplicación. */
    String usage();

    /** Ilustración en {@code /images/patterns/}. */
    String image();

    /** Construye la demo en vivo. Se llama una vez por carga de datos. */
    Node build(PatternData data);
}
