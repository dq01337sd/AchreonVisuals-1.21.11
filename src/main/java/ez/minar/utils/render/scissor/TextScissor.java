package ez.minar.utils.render.scissor;

import ez.minar.utils.render.RenderUtil;
import ez.minar.utils.render.msdf.*;
import ez.minar.utils.render.msdf.Msdf;
import ez.minar.utils.render.msdf.MsdfFont;
import net.minecraft.client.gui.DrawContext;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class TextScissor {

    private static final Map<String, Float> scrollOffsets = new HashMap<>();
    private static final Map<String, Long> lastUpdateTimes = new HashMap<>();

    public static void render(DrawContext context, MsdfFont font, String text,
                              float x, float y, float fontSize,
                              float maxWidth, float fadeWidth,
                              int textColor, int bgColor, boolean scaled,
                              String key, float mouseX, float mouseY, float areaHeight) {
        if (font == null || text == null || text.isEmpty()) return;

        float textWidth = Msdf.width(font, text, fontSize);

        if (textWidth <= maxWidth) {
            Msdf.text(context, font, text, x, y, fontSize, textColor, scaled);
            return;
        }

        long now = System.currentTimeMillis();
        long lastTime = lastUpdateTimes.getOrDefault(key, now);
        float dt = Math.min((now - lastTime) / 1000f, 0.05f);
        lastUpdateTimes.put(key, now);

        float currentOffset = scrollOffsets.getOrDefault(key, 0f);

        boolean hovered = mouseX >= x && mouseX <= x + maxWidth
                && mouseY >= y - 2 && mouseY <= y + areaHeight + 2;

        float overflow = textWidth - maxWidth;

        float targetOffset = 0f;
        if (hovered) {
            targetOffset = -(overflow + 4);
        }

        float hoverSpeed = Math.max(0.8f, 3.0f - (overflow / 60f));
        float returnSpeed = Math.max(1.5f, 5.0f - (overflow / 80f));
        float speed = hovered ? hoverSpeed : returnSpeed;

        currentOffset = currentOffset + (targetOffset - currentOffset) * Math.min(dt * speed, 1.0f);

        if (Math.abs(currentOffset - targetOffset) < 0.05f) {
            currentOffset = targetOffset;
        }

        scrollOffsets.put(key, currentOffset);

        float renderX = x + currentOffset;

        Scissor.push(x, y - 2, maxWidth, fontSize + 6, scaled);
        Msdf.text(context, font, text, renderX, y, fontSize, textColor, scaled);
        Scissor.pop(scaled);

        int bgR = (bgColor >> 16) & 0xFF;
        int bgG = (bgColor >> 8) & 0xFF;
        int bgB = bgColor & 0xFF;
        int bgA = (bgColor >> 24) & 0xFF;

        Color transparent = new Color(bgR, bgG, bgB, 0);
        Color opaque = new Color(bgR, bgG, bgB, bgA);

        float fadeX = x + maxWidth - fadeWidth;
        float fadeY = y - 2;
        float fadeH = fontSize + 6;

        RenderUtil.rect(fadeX, fadeY, fadeWidth, fadeH, 0, 0, 0, 0,
                transparent, opaque, opaque, transparent);
    }

    public static void render(DrawContext context, MsdfFont font, String text,
                              float x, float y, float fontSize,
                              float maxWidth,
                              int textColor, int bgColor, boolean scaled,
                              String key, float mouseX, float mouseY, float areaHeight) {
        render(context, font, text, x, y, fontSize, maxWidth, 14f, textColor, bgColor, scaled,
                key, mouseX, mouseY, areaHeight);
    }

    public static void render(DrawContext context, MsdfFont font, String text,
                              float x, float y, float fontSize,
                              float maxWidth, float fadeWidth,
                              int textColor, int bgColor, boolean scaled) {
        if (font == null || text == null || text.isEmpty()) return;

        float textWidth = Msdf.width(font, text, fontSize);

        if (textWidth <= maxWidth) {
            Msdf.text(context, font, text, x, y, fontSize, textColor, scaled);
            return;
        }

        Scissor.push(x, y - 2, maxWidth, fontSize + 6, scaled);
        Msdf.text(context, font, text, x, y, fontSize, textColor, scaled);
        Scissor.pop(scaled);

        int bgR = (bgColor >> 16) & 0xFF;
        int bgG = (bgColor >> 8) & 0xFF;
        int bgB = bgColor & 0xFF;
        int bgA = (bgColor >> 24) & 0xFF;

        Color transparent = new Color(bgR, bgG, bgB, 0);
        Color opaque = new Color(bgR, bgG, bgB, bgA);

        float fadeX = x + maxWidth - fadeWidth;
        float fadeY = y - 2;
        float fadeH = fontSize + 6;

        RenderUtil.rect(fadeX, fadeY, fadeWidth, fadeH, 0, 0, 0, 0,
                transparent, opaque, opaque, transparent);
    }

    public static void cleanup(String key) {
        scrollOffsets.remove(key);
        lastUpdateTimes.remove(key);
    }
}