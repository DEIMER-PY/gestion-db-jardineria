package com.jardineria.infrastructure.adapter.in.desktop.theme;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AppearanceProfileTest {

    @Test
    @DisplayName("Un perfil sobrevive a la serialización clave/valor sin perder ningún ajuste")
    void roundTrip() {
        AppearanceProfile original = AppearanceProfile.defaults()
                .withName("Mi perfil")
                .withTheme(ThemePreset.ARENA)
                .withAccent(Accent.CORAL)
                .withFontScale(1.15)
                .withDensity(AppearanceProfile.Density.COMPACTA)
                .withCorners(AppearanceProfile.Corners.RECTAS)
                .withAmbient(false)
                .withReducedMotion(true)
                .withStartRoute("ANALYTICS")
                .withSidebarCollapsed(true);

        AppearanceProfile restored = AppearanceProfile.fromMap(original.toMap());

        assertThat(restored).isEqualTo(original);
        assertThat(restored.mode()).isEqualTo(ThemeManager.Mode.LIGHT);
        assertThat(restored.activeTheme()).isEqualTo(ThemePreset.ARENA);
    }

    @Test
    @DisplayName("Valores guardados corruptos o antiguos vuelven a los predeterminados")
    void toleraValoresInvalidos() {
        Map<String, String> raw = new HashMap<>();
        raw.put("name", "  Viejo ");
        raw.put("dark", "inexistente");
        raw.put("light", "grafito");          // un tema oscuro no puede ocupar el hueco claro
        raw.put("accent", "fucsia");
        raw.put("fontScale", "9,5");
        raw.put("density", "???");

        AppearanceProfile p = AppearanceProfile.fromMap(raw);

        assertThat(p.name()).isEqualTo("Viejo");
        assertThat(p.dark()).isEqualTo(ThemePreset.GRAFITO);
        assertThat(p.light()).isEqualTo(ThemePreset.NIEBLA);
        assertThat(p.accent()).isEqualTo(Accent.INDIGO);
        assertThat(p.fontScale()).isEqualTo(AppearanceProfile.MAX_SCALE);
        assertThat(p.density()).isEqualTo(AppearanceProfile.Density.COMODA);
    }

    @Test
    @DisplayName("Elegir un tema claro cambia el modo y conserva el tema oscuro del perfil")
    void withThemeConservaLaPareja() {
        AppearanceProfile p = AppearanceProfile.defaults().withTheme(ThemePreset.MEDIANOCHE).withTheme(ThemePreset.ARENA);
        assertThat(p.dark()).isEqualTo(ThemePreset.MEDIANOCHE);
        assertThat(p.light()).isEqualTo(ThemePreset.ARENA);
        assertThat(p.withMode(ThemeManager.Mode.DARK).activeTheme()).isEqualTo(ThemePreset.MEDIANOCHE);
    }

    @Test
    @DisplayName("Los perfiles predefinidos tienen nombres únicos y el primero es el predeterminado")
    void builtInsUnicos() {
        List<AppearanceProfile> builtIns = AppearanceProfile.builtIns();
        assertThat(builtIns).extracting(AppearanceProfile::name).doesNotHaveDuplicates();
        assertThat(builtIns).allMatch(AppearanceProfile::builtIn);
        assertThat(builtIns.get(0)).isEqualTo(AppearanceProfile.defaults());
    }

    @Test
    @DisplayName("El almacén en memoria devuelve lo guardado")
    void almacenEnMemoria() {
        ProfileStore store = ProfileStore.inMemory();
        store.save(AppearanceProfile.builtIns(), "Nocturno");
        assertThat(store.loadProfiles()).hasSize(AppearanceProfile.builtIns().size());
        assertThat(store.loadActive()).isEqualTo("Nocturno");
    }
}
