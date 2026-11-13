package com.jardineria.infrastructure.adapter.in.desktop.theme;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

/**
 * Persistencia de los perfiles de apariencia.
 * <ul>
 *   <li>{@link #preferences()}: {@code java.util.prefs} del usuario (registro de Windows,
 *       {@code ~/.java} en Linux, plist en macOS). Lo usa la aplicación real.</li>
 *   <li>{@link #inMemory()}: sin efectos secundarios; lo usan las pruebas.</li>
 * </ul>
 */
public interface ProfileStore {

    /** Perfiles guardados (vacío la primera vez). */
    List<AppearanceProfile> loadProfiles();

    /** Nombre del perfil activo guardado, o {@code null}. */
    String loadActive();

    /** Sustituye todos los perfiles guardados y el activo. */
    void save(List<AppearanceProfile> profiles, String active);

    static ProfileStore inMemory() {
        return new InMemory();
    }

    static ProfileStore preferences() {
        return new PreferencesStore(Preferences.userRoot().node("com/jardineria/desktop/appearance"));
    }

    /** Almacén volátil. */
    final class InMemory implements ProfileStore {
        private final List<AppearanceProfile> profiles = new ArrayList<>();
        private String active;

        @Override
        public synchronized List<AppearanceProfile> loadProfiles() {
            return List.copyOf(profiles);
        }

        @Override
        public synchronized String loadActive() {
            return active;
        }

        @Override
        public synchronized void save(List<AppearanceProfile> all, String activeName) {
            profiles.clear();
            profiles.addAll(all);
            active = activeName;
        }
    }

    /** Almacén basado en {@link Preferences}: un subnodo por perfil ({@code p0}, {@code p1}…). */
    final class PreferencesStore implements ProfileStore {
        private static final Logger log = LoggerFactory.getLogger(PreferencesStore.class);
        private final Preferences root;

        PreferencesStore(Preferences root) {
            this.root = root;
        }

        @Override
        public List<AppearanceProfile> loadProfiles() {
            List<AppearanceProfile> result = new ArrayList<>();
            try {
                int count = root.getInt("count", 0);
                for (int i = 0; i < count; i++) {
                    Preferences node = root.node("p" + i);
                    Map<String, String> values = new HashMap<>();
                    for (String key : node.keys()) {
                        values.put(key, node.get(key, null));
                    }
                    if (!values.isEmpty()) {
                        result.add(AppearanceProfile.fromMap(values));
                    }
                }
            } catch (BackingStoreException | IllegalStateException e) {
                log.warn("No se pudieron leer los perfiles de apariencia: {}", e.getMessage());
            }
            return result;
        }

        @Override
        public String loadActive() {
            return root.get("active", null);
        }

        @Override
        public void save(List<AppearanceProfile> profiles, String active) {
            try {
                for (String child : root.childrenNames()) {
                    root.node(child).removeNode();
                }
                for (int i = 0; i < profiles.size(); i++) {
                    Preferences node = root.node("p" + i);
                    for (Map.Entry<String, String> e : new LinkedHashMap<>(profiles.get(i).toMap()).entrySet()) {
                        node.put(e.getKey(), e.getValue());
                    }
                }
                root.putInt("count", profiles.size());
                if (active != null) {
                    root.put("active", active);
                }
                root.flush();
            } catch (BackingStoreException | IllegalStateException e) {
                log.warn("No se pudieron guardar los perfiles de apariencia: {}", e.getMessage());
            }
        }
    }
}
