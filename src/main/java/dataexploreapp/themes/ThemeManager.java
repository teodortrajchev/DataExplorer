package dataexploreapp.themes;

import javafx.scene.Scene;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

/**
 * Tracks the app-wide dark/light theme and keeps every open {@link Scene}
 * in sync.
 * <p>
 * Every page/dialog builds its own {@link Scene} rather than sharing one,
 * so instead of hardcoding "global.css" in each of them, they ask this
 * class for the current stylesheet and register their scene here. When
 * the theme is toggled, every registered scene gets its stylesheet swapped
 * live, and any page built afterwards picks up the new theme automatically.
 */
public final class ThemeManager {

    public enum Theme {
        DARK("/dataexploreapp/pages/global_dark.css"),
        LIGHT("/dataexploreapp/pages/global_light.css");

        private final String resourcePath;

        Theme(String resourcePath) {
            this.resourcePath = resourcePath;
        }

        public String resourcePath() {
            return resourcePath;
        }
    }

    private static Theme currentTheme = Theme.DARK;

    // Weak references so scenes/stages that get closed don't leak here forever.
    private static final List<WeakReference<Scene>> scenes = new ArrayList<>();

    private ThemeManager() {
    }

    public static Theme getCurrentTheme() {
        return currentTheme;
    }

    public static boolean isDark() {
        return currentTheme == Theme.DARK;
    }

    public static String currentStylesheetUrl() {
        var url = ThemeManager.class.getResource(currentTheme.resourcePath());
        return url != null ? url.toExternalForm() : null;
    }

    /**
     * Applies the current theme to a scene and registers it so future
     * toggles keep this window in sync. Call this once, right after
     * creating each page's Scene.
     */
    public static void register(Scene scene) {
        applyTheme(scene, currentTheme);
        scenes.add(new WeakReference<>(scene));
    }

    /** Flips the theme and immediately re-applies it to every open, still-alive scene. */
    public static void toggleTheme() {
        currentTheme = (currentTheme == Theme.DARK) ? Theme.LIGHT : Theme.DARK;

        scenes.removeIf(ref -> ref.get() == null);
        for (WeakReference<Scene> ref : scenes) {
            Scene scene = ref.get();
            if (scene != null) {
                applyTheme(scene, currentTheme);
            }
        }
    }

    private static void applyTheme(Scene scene, Theme theme) {
        var url = ThemeManager.class.getResource(theme.resourcePath());
        if (url == null) {
            return;
        }
        scene.getStylesheets().removeIf(s -> s.endsWith("global_dark.css") || s.endsWith("global_light.css"));
        scene.getStylesheets().add(url.toExternalForm());
    }
}