package ez.minar.system.menu;

import ez.minar.utils.render.RenderUtil;
import ez.minar.utils.render.msdf.Msdf;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.PressableTextWidget;

import java.awt.Color;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.IdentityHashMap;
import java.util.Map;

public class TitleScreenMenuRenderer {
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final long TIME_ANIMATION_MS = 720L;
    private static final Map<ClickableWidget, Float> HOVER_PROGRESS = new IdentityHashMap<>();
    private static final Map<ButtonWidget, String> BUTTON_ICONS = new IdentityHashMap<>();
    private static DrawContext lastContext;
    private static long lastFrame;
    private static String displayedTime = "";
    private static String previousTime = "";
    private static long[] timeChangeStarted = new long[0];

    private static final String ALT_ICON = "U";
    private static final String[] TITLE_CHARS = "Achrone".split("");

    public static void setContext(DrawContext context) {
        lastContext = context;
    }

    public static void registerButtonIcon(ButtonWidget button, String icon) {
        BUTTON_ICONS.put(button, icon);
    }

    public static void hideTitleScreenExtras(TitleScreen titleScreen) {
        for (Element element : titleScreen.children()) {
            if (element instanceof ButtonWidget) {
                continue;
            }
            if (element instanceof ClickableWidget widget) {
                widget.visible = false;
                widget.active = false;
            }
        }
    }

    public static boolean shouldHideTitleScreenText(String text) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (!(client.currentScreen instanceof TitleScreen) || text == null) {
            return false;
        }
        String lower = text.toLowerCase();
        return lower.contains("account") || lower.contains("аккаунт");
    }

    public static void renderButtons() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (!(client.currentScreen instanceof TitleScreen titleScreen)) {
            return;
        }

        updateAnimations();
        renderTime();
        renderTitle();
        renderFooter();

        for (Element element : titleScreen.children()) {
            if (element instanceof ButtonWidget button) {
                if (!button.visible) {
                    continue;
                }
                String icon = BUTTON_ICONS.get(button);
                if (icon == null) {
                    icon = getTitleButtonIcon(button);
                }
                if (icon != null) {
                    renderIconButton(button, icon);
                    renderTooltip(button, getTitleButtonTooltip(icon));
                }
            }
        }
    }

    private static void renderTime() {
        if (lastContext == null) {
            return;
        }
        String time = LocalTime.now().format(TIME_FORMATTER);
        updateTimeState(time);
        float h = RenderUtil.getFixedScaledHeight();
        float timeSize = clamp(h * 0.05f, 16f, 26f);
        float timeY = clamp(h * 0.10f, 20f, 90f);
        renderAnimatedTime(RenderUtil.getFixedScaledWidth() / 2f, timeY, timeSize);
    }

    private static void renderTitle() {
        if (lastContext == null) {
            return;
        }
        Msdf.hasFonts();

        float w = RenderUtil.getFixedScaledWidth();
        float h = RenderUtil.getFixedScaledHeight();
        float cx = w / 2f;
        float titleSize = clamp(h * 0.14f, 30f, 82f);
        float titleY = clamp(h * 0.24f - titleSize * 0.35f, 24f, h * 0.32f);
        Color theme = ThemeManager.getThemeColor();
        float gap = clamp(titleSize * 0.04f, 2f, 4f);

        float totalWidth = 0f;
        for (String ch : TITLE_CHARS) {
            totalWidth += Msdf.width(Msdf.SF_BOLD, ch, titleSize) + gap;
        }
        totalWidth -= gap;
        if (totalWidth > w * 0.96f) {
            titleSize *= w * 0.96f / totalWidth;
            totalWidth = w * 0.96f;
            titleY = clamp(h * 0.24f - titleSize * 0.35f, 24f, h * 0.32f);
        }

        long now = System.currentTimeMillis();
        float x = cx - totalWidth / 2f;
        for (int i = 0; i < TITLE_CHARS.length; i++) {
            String ch = TITLE_CHARS[i];
            float charWidth = Msdf.width(Msdf.SF_BOLD, ch, titleSize);
            float offset = (now - i * 130L) % 2400L / 2400f;
            if (offset < 0) offset += 1f;
            float t = (float) (Math.sin(offset * Math.PI * 2) * 0.5 + 0.5);
            Color c = lerpColor(theme, new Color(250, 250, 252), t);
            RenderUtil.text(lastContext, Msdf.SF_BOLD, x, titleY, ch, titleSize, c);
            x += charWidth + gap;
        }

        String subtitle = "Ver: 1.21.11";
        float subSize = clamp(h * 0.022f, 11f, 16f);
        float subW = Msdf.width(Msdf.SF_REGULAR, subtitle, subSize);
        RenderUtil.text(lastContext, Msdf.SF_REGULAR, cx - subW / 2f, titleY + titleSize * 0.95f, subtitle, subSize,
                new Color(160, 166, 175));
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private static void renderFooter() {
        if (lastContext == null) {
            return;
        }
        float h = RenderUtil.getFixedScaledHeight();
        String version = "Achrone v1.0";
        float vsize = clamp(h * 0.02f, 9f, 13f);
        RenderUtil.text(lastContext, Msdf.SF_BOLD, 12f, h - Msdf.height(Msdf.SF_BOLD, vsize) - 12f, version, vsize,
                new Color(150, 155, 165, 200));
    }

    private static void updateAnimations() {
        long now = System.currentTimeMillis();
        if (lastFrame == 0L) {
            lastFrame = now;
            return;
        }
        float delta = Math.min(50f, now - lastFrame);
        lastFrame = now;

        HOVER_PROGRESS.entrySet().removeIf(entry -> !entry.getKey().visible);
        for (Map.Entry<ClickableWidget, Float> entry : HOVER_PROGRESS.entrySet()) {
            ClickableWidget widget = entry.getKey();
            float target = widget.isSelected() ? 1f : 0f;
            float factor = 1f - (float) Math.exp(-0.018f * delta);
            float next = entry.getValue() + (target - entry.getValue()) * Math.clamp(factor, 0f, 1f);
            entry.setValue(Math.abs(next - target) < 0.001f ? target : next);
        }
    }

    private static void renderIconButton(ClickableWidget widget, String icon) {
        if (widget instanceof PressableTextWidget) {
            return;
        }

        float alpha = widget.getAlpha();
        float progress = HOVER_PROGRESS.computeIfAbsent(widget, ignored -> 0f);
        float eased = easeOutCubic(progress);
        float width = widget.getWidth();
        float height = widget.getHeight();
        float x = widget.getX();
        float y = widget.getY();
        float radius = 6f;
        Color theme = ThemeManager.getThemeColor();

        RenderUtil.shadow(x + 0.5f, y + 1.4f, width - 1f, height - 0.4f, radius,
                10f, 0.30f * alpha, 2.4f, new Color(0, 0, 0, 170));
        RenderUtil.rect(x, y, width, height, radius, new Color(42, 44, 54, 255));
        RenderUtil.outline(x + 0.45f, y + 0.45f, width - 0.9f, height - 0.9f, radius,
                0.8f + eased * 0.5f, withAlpha(theme, 0.12f + 0.28f * eased));

        float accentH = 2f + 2f * eased;
        RenderUtil.rect(x + 10f, y, width - 20f, accentH, 0f, withAlpha(theme, alpha * (0.5f + 0.5f * eased)));

        Color textColor = widget.active
                ? new Color(248, 250, 252, Math.round(255f * alpha))
                : new Color(185, 190, 196, Math.round(210f * alpha));

        float iconSize = 24f;
        float iconW = Msdf.width(Msdf.WTMICO, icon, iconSize);
        float iconX = x + (width - iconW) / 2f;
        float iconY = y + height * 0.20f;
        RenderUtil.text(lastContext, Msdf.WTMICO, iconX, iconY, icon, iconSize, textColor);

        String label = getTitleButtonLabel(icon);
        if (label != null) {
            float labelSize = 9f;
            float labelW = Msdf.width(Msdf.SF_REGULAR, label, labelSize);
            float labelX = x + (width - labelW) / 2f;
            float labelY = y + height - Msdf.height(Msdf.SF_REGULAR, labelSize) - 7f;
            RenderUtil.text(lastContext, Msdf.SF_REGULAR, labelX, labelY, label, labelSize,
                    new Color(205, 210, 218, Math.round(255f * alpha)));
        }
    }

    private static void renderTooltip(ClickableWidget widget, String tooltip) {
        if (tooltip == null) {
            return;
        }
        float progress = HOVER_PROGRESS.getOrDefault(widget, 0f);
        if (progress <= 0.01f) {
            return;
        }

        float eased = easeOutCubic(progress);
        float alpha = widget.getAlpha() * eased;
        float scale = 0.96f + eased * 0.04f;
        float textSize = 8.4f * scale;
        float paddingX = 7f * scale;
        float paddingY = 4.5f * scale;
        float textWidth = Msdf.width(Msdf.SF_BOLD, tooltip, textSize);
        float textHeight = Msdf.height(Msdf.SF_BOLD, textSize);
        float width = textWidth + paddingX * 2f;
        float height = textHeight + paddingY * 2f;
        float x = widget.getX() + widget.getWidth() / 2f - width / 2f;
        float y = widget.getY() - height - 9f + (1f - eased) * 6f;
        float radius = 4f * scale;

        RenderUtil.shadow(x, y + 1.5f, width, height, radius,
                8f, 0.22f * alpha, 2.0f, new Color(0, 0, 0, 170));
        RenderUtil.rect(x, y, width, height, radius, new Color(150, 150, 150, 20));
        RenderUtil.outline(x + 0.45f, y + 0.45f, width - 0.9f, height - 0.9f, radius,
                0.75f, new Color(255, 255, 255, Math.round(58f * alpha)));
        RenderUtil.text(lastContext, Msdf.SF_BOLD, x + paddingX, y + paddingY - 0.5f,
                tooltip, textSize, new Color(245, 247, 250, Math.round(255f * alpha)));
    }

    private static void updateTimeState(String time) {
        long now = System.currentTimeMillis();
        if (displayedTime.length() != time.length()) {
            displayedTime = time;
            previousTime = time;
            timeChangeStarted = new long[time.length()];
            return;
        }
        if (timeChangeStarted.length != time.length()) {
            timeChangeStarted = new long[time.length()];
        }

        char[] current = displayedTime.toCharArray();
        char[] previous = previousTime.toCharArray();
        for (int i = 0; i < time.length(); i++) {
            char next = time.charAt(i);
            if (current[i] != next) {
                previous[i] = current[i];
                current[i] = next;
                timeChangeStarted[i] = now;
            }
        }
        displayedTime = new String(current);
        previousTime = new String(previous);
    }

    private static void renderAnimatedTime(float centerX, float y, float size) {
        if (displayedTime.isEmpty()) {
            return;
        }

        float digitWidth = Math.max(Msdf.width(Msdf.SF_BOLD, "0", size), Msdf.width(Msdf.SF_BOLD, "8", size));
        float colonWidth = Msdf.width(Msdf.SF_BOLD, ":", size) + 2f;
        float gap = 1.5f;
        float height = Msdf.height(Msdf.SF_BOLD, size);
        float totalWidth = 0f;
        for (int i = 0; i < displayedTime.length(); i++) {
            totalWidth += cellWidth(displayedTime.charAt(i), digitWidth, colonWidth);
            if (i < displayedTime.length() - 1) {
                totalWidth += gap;
            }
        }

        long now = System.currentTimeMillis();
        float x = centerX - totalWidth / 2f;
        Color color = ThemeManager.getThemeColor();

        for (int i = 0; i < displayedTime.length(); i++) {
            char current = displayedTime.charAt(i);
            char previous = previousTime.charAt(i);
            float cellWidth = cellWidth(current, digitWidth, colonWidth);
            float charWidth = Msdf.width(Msdf.SF_BOLD, String.valueOf(current), size);
            float charX = x + (cellWidth - charWidth) / 2f;

            if (current == ':' || timeChangeStarted[i] == 0L) {
                RenderUtil.text(lastContext, Msdf.SF_BOLD, charX, y, String.valueOf(current), size, color);
            } else {
                float progress = Math.clamp((now - timeChangeStarted[i]) / (float) TIME_ANIMATION_MS, 0f, 1f);
                float incoming = easeOutBack(progress);
                float outgoing = easeOutCubic(progress);
                float clipPad = 6f;

                ez.minar.utils.render.scissor.Scissor.push(x - clipPad, y - clipPad, cellWidth + clipPad * 2f, height + clipPad * 2f);
                RenderUtil.text(lastContext, Msdf.SF_BOLD,
                        x + (cellWidth - Msdf.width(Msdf.SF_BOLD, String.valueOf(previous), size)) / 2f,
                        y - height * outgoing,
                        String.valueOf(previous), size, withAlpha(color, 1f - progress * 0.35f));
                RenderUtil.text(lastContext, Msdf.SF_BOLD,
                        charX,
                        y + height * (1f - incoming),
                        String.valueOf(current), size, color);
                ez.minar.utils.render.scissor.Scissor.pop();

                if (progress >= 1f) {
                    timeChangeStarted[i] = 0L;
                    char[] previousChars = previousTime.toCharArray();
                    previousChars[i] = current;
                    previousTime = new String(previousChars);
                }
            }

            x += cellWidth + gap;
        }
    }

    private static float cellWidth(char c, float digitWidth, float colonWidth) {
        return c == ':' ? colonWidth : digitWidth;
    }

    private static Color lerpColor(Color from, Color to, float t) {
        int r = (int) (from.getRed() + (to.getRed() - from.getRed()) * t);
        int g = (int) (from.getGreen() + (to.getGreen() - from.getGreen()) * t);
        int b = (int) (from.getBlue() + (to.getBlue() - from.getBlue()) * t);
        return new Color(r, g, b);
    }

    private static Color withAlpha(Color color, float alpha) {
        return new Color(color.getRed(), color.getGreen(), color.getBlue(),
                Math.round(255f * Math.clamp(alpha, 0f, 1f)));
    }

    private static String getTitleButtonIcon(ClickableWidget widget) {
        String text = widget.getMessage().getString().toLowerCase();
        if (text.contains("singleplayer")) {
            return "P";
        }
        if (text.contains("multiplayer")) {
            return "N";
        }
        if (text.contains("options") || text.contains("settings")) {
            return "O";
        }
        if (text.contains("quit")) {
            return "Q";
        }
        if (text.contains("alt")) {
            return ALT_ICON;
        }
        return null;
    }

    private static String getTitleButtonLabel(String icon) {
        if (icon == null) return null;
        return switch (icon) {
            case "P" -> "SinglePlayer";
            case "N" -> "MultiPlayer";
            case "O" -> "Settings";
            case "Q" -> "Turn Off";
            case ALT_ICON -> "AltManager";
            default -> null;
        };
    }

    private static String getTitleButtonTooltip(String icon) {
        if (icon == null) return null;
        if (icon.equals(ALT_ICON)) return "Open alt manager";
        return switch (icon) {
            case "P" -> "Open singleplayer";
            case "N" -> "Open multiplayer";
            case "O" -> "Open options";
            case "Q" -> "Quit the game";
            default -> null;
        };
    }

    private static float easeOutCubic(float progress) {
        float t = Math.clamp(progress, 0f, 1f) - 1f;
        return t * t * t + 1f;
    }

    private static float easeOutBack(float progress) {
        float t = Math.clamp(progress, 0f, 1f) - 1f;
        float c = 1.70158f;
        return 1f + (c + 1f) * t * t * t + c * t * t;
    }
}
