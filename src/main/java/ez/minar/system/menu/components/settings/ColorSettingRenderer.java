package ez.minar.system.menu.components.settings;

import ez.minar.system.settings.Setting;
import ez.minar.system.settings.impl.ColorSetting;
import ez.minar.utils.render.RenderUtil;
import net.minecraft.client.gui.DrawContext;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class ColorSettingRenderer implements SettingRenderer<ColorSetting> {
    private static final float FIELD_SIZE = 62f;
    private static final float HUE_WIDTH = 7f;
    private static final float GAP = 7f;
    private static final int HUE_DOTS = 72;

    private final Map<ColorSetting, PickerState> states = new HashMap<>();

    @Override
    public boolean supports(Setting setting) {
        return setting instanceof ColorSetting;
    }

    @Override
    public void render(DrawContext context, ColorSetting setting, SettingRendererContext rendererContext, float x, float y, float width, float scale, float opacity) {
        Color color = setting.getColor();
        float textSize = 7.7f * scale;
        Color label = rendererContext.withOpacity(new Color(210, 210, 214), opacity);
        Color value = rendererContext.withOpacity(new Color(150, 150, 156), opacity);
        float swatchSize = 10f * scale;
        float swatchX = x + width - swatchSize;
        float swatchY = y + 4f * scale;

        rendererContext.renderBoundedText(context, setting, "name", setting.getName(), x, y + 4.5f * scale,
                width * 0.55f, SettingRendererContext.SETTING_HEIGHT * scale, textSize, label, false,
                rendererContext.isHovered(x, y, width, SettingRendererContext.SETTING_HEIGHT * scale));
        RenderUtil.text(context, swatchX - 6f * scale, y + 4.5f * scale, toHex(color), textSize, value, "right");
        RenderUtil.rect(swatchX, swatchY, swatchSize, swatchSize, 3f * scale, rendererContext.withOpacity(color, opacity));
        RenderUtil.outline(swatchX, swatchY, swatchSize, swatchSize, 3f * scale, 0.8f * scale, rendererContext.withOpacity(new Color(255, 255, 255), opacity * 0.42f));

        float expansion = rendererContext.getExpansion(setting);
        if (expansion <= 0.02f) return;

        PickerState state = state(setting);
        float pickerY = y + SettingRendererContext.SETTING_HEIGHT * scale;
        float pickerHeight = FIELD_SIZE * scale * expansion;
        float fieldSize = FIELD_SIZE * scale;
        float hueWidth = HUE_WIDTH * scale;
        float gap = GAP * scale;
        float fieldX = x;
        float hueX = fieldX + fieldSize + gap;
        float radius = 5f * scale;

        renderSaturationBrightnessField(rendererContext, fieldX, pickerY, fieldSize, pickerHeight, radius, state.hue, opacity * expansion);
        RenderUtil.outline(fieldX, pickerY, fieldSize, pickerHeight, radius, 0.9f * scale,
                rendererContext.withOpacity(new Color(255, 255, 255), opacity * expansion * 0.25f));

        renderHueSlider(rendererContext, hueX, pickerY, hueWidth, pickerHeight, hueWidth / 2f, opacity * expansion);
        RenderUtil.outline(hueX, pickerY, hueWidth, pickerHeight, hueWidth / 2f, 0.9f * scale,
                rendererContext.withOpacity(new Color(255, 255, 255), opacity * expansion * 0.25f));

        float markerX = fieldX + state.saturation * fieldSize;
        float markerY = pickerY + (1f - state.brightness) * fieldSize;
        float hueMarkerY = pickerY + state.hue * fieldSize;
        Color marker = rendererContext.withOpacity(Color.WHITE, opacity * expansion);
        RenderUtil.outline(markerX - 2.5f * scale, markerY - 2.5f * scale, 5f * scale, 5f * scale, 2.5f * scale, 1f * scale, marker);
        RenderUtil.outline(hueX - 1.5f * scale, hueMarkerY - 1.5f * scale, hueWidth + 3f * scale, 3f * scale, 1.5f * scale, 1f * scale, marker);
    }

    @Override
    public boolean click(ColorSetting setting, SettingRendererContext rendererContext, int button, float localX, float localY, float width) {
        if (button == 1) {
            rendererContext.toggleExpanded(setting);
            return false;
        }

        if (!rendererContext.isExpanded(setting) || localY < SettingRendererContext.SETTING_HEIGHT) {
            rendererContext.toggleExpanded(setting);
            return false;
        }

        updateFromMouse(setting, localX, localY);
        return true;
    }

    @Override
    public boolean drag(ColorSetting setting, SettingRendererContext rendererContext, int button, float localX, float localY, float width) {
        if (button != 0 || !rendererContext.isExpanded(setting)) {
            return false;
        }

        updateFromMouse(setting, localX, localY);
        return true;
    }

    @Override
    public float getHeight(ColorSetting setting, SettingRendererContext rendererContext) {
        return SettingRendererContext.SETTING_HEIGHT + FIELD_SIZE * rendererContext.getExpansion(setting);
    }

    @Override
    public String getValue(ColorSetting setting) {
        return toHex(setting.getColor());
    }

    private void updateFromMouse(ColorSetting setting, float localX, float localY) {
        PickerState state = state(setting);
        float pickerY = SettingRendererContext.SETTING_HEIGHT;
        float hueX = FIELD_SIZE + GAP;

        if (localX >= 0f && localX <= FIELD_SIZE && localY >= pickerY && localY <= pickerY + FIELD_SIZE) {
            state.saturation = Math.clamp(localX / FIELD_SIZE, 0f, 1f);
            state.brightness = 1f - Math.clamp((localY - pickerY) / FIELD_SIZE, 0f, 1f);
            apply(setting, state);
        } else if (localX >= hueX && localX <= hueX + HUE_WIDTH && localY >= pickerY && localY <= pickerY + FIELD_SIZE) {
            state.hue = Math.clamp((localY - pickerY) / FIELD_SIZE, 0f, 1f);
            apply(setting, state);
        }
    }

    private PickerState state(ColorSetting setting) {
        PickerState state = states.computeIfAbsent(setting, ignored -> new PickerState());
        Color color = setting.getColor();
        if (state.sourceRgb != color.getRGB()) {
            float[] hsb = Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), null);
            state.hue = hsb[0];
            state.saturation = hsb[1];
            state.brightness = hsb[2];
            state.sourceRgb = color.getRGB();
        }
        return state;
    }

    private void apply(ColorSetting setting, PickerState state) {
        Color color = Color.getHSBColor(state.hue, state.saturation, state.brightness);
        if (setting.getColor().getRGB() == color.getRGB()) return;

        setting.setColor(color);
        state.sourceRgb = color.getRGB();
    }

    private String toHex(Color color) {
        return String.format("#%02X%02X%02X", color.getRed(), color.getGreen(), color.getBlue());
    }

    private void renderSaturationBrightnessField(SettingRendererContext rendererContext, float x, float y,
                                                 float width, float height, float radius, float hue, float opacity) {
        Color topLeft = Color.WHITE;
        Color topMid = mix(Color.WHITE, Color.getHSBColor(hue, 1f, 1f), 0.5f);
        Color topRight = Color.getHSBColor(hue, 1f, 1f);
        Color midLeft = new Color(128, 128, 128);
        Color midMid = Color.getHSBColor(hue, 0.5f, 0.5f);
        Color midRight = Color.getHSBColor(hue, 1f, 0.5f);
        Color black = Color.BLACK;

        RenderUtil.rect(x, y, width, height, radius,
                rendererContext.withOpacity(topLeft, opacity),
                rendererContext.withOpacity(topMid, opacity),
                rendererContext.withOpacity(topRight, opacity),
                rendererContext.withOpacity(midLeft, opacity),
                rendererContext.withOpacity(midMid, opacity),
                rendererContext.withOpacity(midRight, opacity),
                rendererContext.withOpacity(black, opacity),
                rendererContext.withOpacity(black, opacity),
                rendererContext.withOpacity(black, opacity));
    }

    private Color mix(Color first, Color second, float progress) {
        float clamped = Math.clamp(progress, 0f, 1f);
        int red = (int) (first.getRed() + (second.getRed() - first.getRed()) * clamped);
        int green = (int) (first.getGreen() + (second.getGreen() - first.getGreen()) * clamped);
        int blue = (int) (first.getBlue() + (second.getBlue() - first.getBlue()) * clamped);
        int alpha = (int) (first.getAlpha() + (second.getAlpha() - first.getAlpha()) * clamped);
        return new Color(red, green, blue, alpha);
    }

    private void renderHueSlider(SettingRendererContext rendererContext, float x, float y,
                                 float width, float height, float radius, float opacity) {
        if (height <= 0f || width <= 0f) return;

        float dotSize = width;
        float travel = Math.max(0f, height - dotSize);
        float step = HUE_DOTS <= 1 ? 0f : travel / (HUE_DOTS - 1);

        for (int i = 0; i < HUE_DOTS; i++) {
            float progress = i / (float) (HUE_DOTS - 1);
            Color color = rendererContext.withOpacity(Color.getHSBColor(progress, 1f, 1f), opacity);
            RenderUtil.rect(x, y + i * step, dotSize, dotSize, radius, color);
        }
    }

    private static class PickerState {
        private float hue;
        private float saturation;
        private float brightness = 1f;
        private int sourceRgb;
    }
}
