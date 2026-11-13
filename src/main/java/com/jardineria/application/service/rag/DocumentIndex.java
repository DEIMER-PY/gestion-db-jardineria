package com.jardineria.application.service.rag;

import com.jardineria.application.port.out.DocumentacionPort.Documento;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Índice léxico BM25 sobre la documentación Markdown, troceada por secciones.
 * <p>
 * Es la parte de recuperación del RAG: sin embeddings ni dependencias externas. Normaliza
 * tildes y mayúsculas («apariencia» encuentra «Apariencia», «tema» encuentra «temas»
 * mediante un recorte sencillo de plurales) y descarta palabras vacías del español.
 */
public final class DocumentIndex {
    private static final double K1 = 1.4;
    private static final double B = 0.75;
    private static final int MAX_CHARS = 1400;
    private static final Pattern HEADING = Pattern.compile("^(#{1,3})\\s+(.+)$");
    private static final Pattern NON_WORD = Pattern.compile("[^\\p{L}\\p{N}]+");
    private static final Set<String> STOP = Set.of(
            "el", "la", "los", "las", "un", "una", "unos", "unas", "de", "del", "al", "a", "en", "y", "o", "u",
            "que", "por", "para", "con", "sin", "se", "su", "sus", "es", "son", "lo", "le", "les", "como",
            "mas", "pero", "si", "no", "ya", "este", "esta", "estos", "estas", "ese", "esa", "hay", "muy",
            "me", "mi", "te", "tu", "puedo", "puede", "cual", "cuales", "donde", "cuando", "the", "and", "of");

    /** Fragmento indexado: una sección (o parte de ella) de un documento. */
    public record Fragmento(String fuente, String seccion, String texto) {
    }

    /** Fragmento con su puntuación. */
    public record Resultado(Fragmento fragmento, double puntuacion) {
    }

    private final List<Fragmento> fragmentos = new ArrayList<>();
    private final List<Map<String, Integer>> frecuencias = new ArrayList<>();
    private final List<Integer> longitudes = new ArrayList<>();
    private final Map<String, Integer> documentosConTermino = new HashMap<>();
    private double longitudMedia;

    public DocumentIndex(List<Documento> documentos) {
        for (Documento d : documentos) {
            for (Fragmento f : trocear(d)) {
                List<String> terms = terminos(f.seccion() + " " + f.texto());
                if (terms.isEmpty()) {
                    continue;
                }
                Map<String, Integer> tf = new HashMap<>();
                terms.forEach(t -> tf.merge(t, 1, Integer::sum));
                tf.keySet().forEach(t -> documentosConTermino.merge(t, 1, Integer::sum));
                fragmentos.add(f);
                frecuencias.add(tf);
                longitudes.add(terms.size());
            }
        }
        longitudMedia = longitudes.stream().mapToInt(Integer::intValue).average().orElse(1);
    }

    public int size() {
        return fragmentos.size();
    }

    /** Los {@code limite} fragmentos más relevantes con puntuación positiva, de mayor a menor. */
    public List<Resultado> buscar(String consulta, int limite) {
        List<String> q = terminos(consulta).stream().distinct().toList();
        if (q.isEmpty() || fragmentos.isEmpty()) {
            return List.of();
        }
        int n = fragmentos.size();
        List<Resultado> out = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            Map<String, Integer> tf = frecuencias.get(i);
            double score = 0;
            for (String t : q) {
                Integer f = tf.get(t);
                if (f == null) {
                    continue;
                }
                int df = documentosConTermino.getOrDefault(t, 0);
                double idf = Math.log(1 + (n - df + 0.5) / (df + 0.5));
                score += idf * (f * (K1 + 1)) / (f + K1 * (1 - B + B * longitudes.get(i) / longitudMedia));
            }
            if (score > 0) {
                out.add(new Resultado(fragmentos.get(i), score));
            }
        }
        out.sort(Comparator.comparingDouble(Resultado::puntuacion).reversed());
        return out.subList(0, Math.min(limite, out.size()));
    }

    /** Trocea por encabezados de nivel 1 a 3 y, si una sección es larga, por párrafos. */
    static List<Fragmento> trocear(Documento d) {
        List<Fragmento> out = new ArrayList<>();
        String seccion = d.fuente();
        StringBuilder actual = new StringBuilder();
        boolean enCodigo = false;
        for (String linea : d.markdown().split("\\R")) {
            if (linea.startsWith("```")) {
                enCodigo = !enCodigo;
            }
            var m = HEADING.matcher(linea);
            if (!enCodigo && m.matches()) {
                emitir(out, d.fuente(), seccion, actual);
                seccion = m.group(2).replaceAll("[*`]", "").trim();
                continue;
            }
            if (actual.length() + linea.length() > MAX_CHARS && linea.isBlank()) {
                emitir(out, d.fuente(), seccion, actual);
            }
            actual.append(linea).append('\n');
        }
        emitir(out, d.fuente(), seccion, actual);
        return out;
    }

    private static void emitir(List<Fragmento> out, String fuente, String seccion, StringBuilder texto) {
        String t = texto.toString().strip();
        if (!t.isEmpty()) {
            out.add(new Fragmento(fuente, seccion, t));
        }
        texto.setLength(0);
    }

    /** Minúsculas sin tildes, sin palabras vacías y con un recorte sencillo de plurales. */
    static List<String> terminos(String texto) {
        if (texto == null) {
            return List.of();
        }
        String plano = Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "").toLowerCase(Locale.ROOT);
        List<String> out = new ArrayList<>();
        for (String w : NON_WORD.split(plano)) {
            if (w.length() < 2 || STOP.contains(w)) {
                continue;
            }
            out.add(raiz(w));
        }
        return out;
    }

    private static String raiz(String w) {
        if (w.length() > 4 && w.endsWith("es")) {
            return w.substring(0, w.length() - 2);
        }
        if (w.length() > 3 && w.endsWith("s")) {
            return w.substring(0, w.length() - 1);
        }
        return w;
    }
}
