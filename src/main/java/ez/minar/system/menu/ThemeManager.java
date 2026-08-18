package ez.minar.system.menu;

import net.fabricmc.loader.api.FabricLoader;

import java.awt.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class ThemeManager {
    public static Color Theme_Color = new Color(242, 139, 130);
    public static Color Theme_Color2 = new Color(138, 180, 248);
    public static boolean twoColors = false;

    public static final Color PASTEL_RED = new Color(242, 139, 130);
    public static final Color PASTEL_BLUE = new Color(138, 180, 248);
    public static final List<Color> CUSTOM_THEMES = new ArrayList<>();
    private static Path themeFile;

    private ThemeManager() {
    }

    public static void init() {
        Path minarDir = FabricLoader.getInstance().getGameDir().resolve("Achrone");
        themeFile = minarDir.resolve("themes.properties");

        try {
            Files.createDirectories(minarDir);
            load();
        } catch (IOException ignored) {
        }
    }

    public static void setThemeColor(Color color) {
        Theme_Color = withoutAlpha(color);
        save();
    }

    public static void setThemeColor2(Color color) {
        Theme_Color2 = withoutAlpha(color);
        save();
    }

    public static Color getThemeColor() {
        if (!twoColors) {
            return Theme_Color;
        }
        float time = (System.currentTimeMillis() % 2000L) / 2000f;
        float t = (float) (Math.sin(time * Math.PI * 2) * 0.5f + 0.5f);
        int r = (int) (Theme_Color.getRed() + (Theme_Color2.getRed() - Theme_Color.getRed()) * t);
        int g = (int) (Theme_Color.getGreen() + (Theme_Color2.getGreen() - Theme_Color.getGreen()) * t);
        int b = (int) (Theme_Color.getBlue() + (Theme_Color2.getBlue() - Theme_Color.getBlue()) * t);
        return new Color(r, g, b);
    }

    public static int addCustomTheme(Color color) {
        CUSTOM_THEMES.add(withoutAlpha(color));
        save();
        return CUSTOM_THEMES.size() - 1;
    }

    public static void setCustomTheme(int index, Color color) {
        if (index < 0 || index >= CUSTOM_THEMES.size()) {
            return;
        }

        CUSTOM_THEMES.set(index, withoutAlpha(color));
        save();
    }

    public static void removeCustomTheme(int index) {
        if (index < 0 || index >= CUSTOM_THEMES.size()) {
            return;
        }

        Color removed = CUSTOM_THEMES.remove(index);
        if (sameColor(removed, Theme_Color)) {
            Theme_Color = CUSTOM_THEMES.isEmpty() ? PASTEL_RED : CUSTOM_THEMES.get(Math.min(index, CUSTOM_THEMES.size() - 1));
        }
        save();
    }

    public static void save() {
        if (themeFile == null) {
            return;
        }

        Properties properties = new Properties();
        properties.setProperty("selected", colorToHex(Theme_Color));
        properties.setProperty("selected2", colorToHex(Theme_Color2));
        properties.setProperty("twoColors", String.valueOf(twoColors));
        properties.setProperty("custom.count", String.valueOf(CUSTOM_THEMES.size()));
        for (int i = 0; i < CUSTOM_THEMES.size(); i++) {
            properties.setProperty("custom." + i, colorToHex(CUSTOM_THEMES.get(i)));
        }

        try (var writer = Files.newBufferedWriter(themeFile)) {
            properties.store(writer, "Achrone themes");
        } catch (IOException ignored) {
        }
    }

    private static void load() throws IOException {
        if (!Files.exists(themeFile)) {
            save();
            return;
        }

        Properties properties = new Properties();
        try (var reader = Files.newBufferedReader(themeFile)) {
            properties.load(reader);
        }

        Theme_Color = parseColor(properties.getProperty("selected"), PASTEL_RED);
        Theme_Color2 = parseColor(properties.getProperty("selected2"), PASTEL_BLUE);
        twoColors = Boolean.parseBoolean(properties.getProperty("twoColors", "false"));
        CUSTOM_THEMES.clear();
        int count = parseInt(properties.getProperty("custom.count"), 0);
        for (int i = 0; i < count; i++) {
            CUSTOM_THEMES.add(parseColor(properties.getProperty("custom." + i), PASTEL_RED));
        }
    }

    private static Color parseColor(String value, Color fallback) {
        if (value == null || value.length() != 7 || value.charAt(0) != '#') {
            return fallback;
        }

        try {
            return new Color(Integer.parseInt(value.substring(1), 16));
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private static int parseInt(String value, int fallback) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private static String colorToHex(Color color) {
        return String.format("#%02X%02X%02X", color.getRed(), color.getGreen(), color.getBlue());
    }

    private static Color withoutAlpha(Color color) {
        return new Color(color.getRed(), color.getGreen(), color.getBlue());
    }

    private static boolean sameColor(Color first, Color second) {
        return first.getRed() == second.getRed() && first.getGreen() == second.getGreen() && first.getBlue() == second.getBlue();
    }
}
