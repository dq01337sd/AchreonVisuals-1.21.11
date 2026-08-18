package ez.minar.system.menu.components.settings;

import ez.minar.system.menu.ThemeManager;
import ez.minar.system.settings.Setting;
import ez.minar.system.settings.impl.BooleanSetting;
import ez.minar.utils.render.RenderUtil;
import net.minecraft.client.gui.DrawContext;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class BooleanSettingRenderer implements SettingRenderer<BooleanSetting> {
    private static final float SWITCH_WIDTH = 20f;
    private static final float SWITCH_HEIGHT = 8f;
    private static final float KNOB_SIZE = 7f;
    private static final float ANIMATION_SPEED = 0.0055f;

    private final Map<BooleanSetting, Float> progress = new HashMap<>();
    private final Map<BooleanSetting, Long> lastFrameTime = new HashMap<>();

    @Override
    public boolean supports(Setting setting) {
        return setting instanceof BooleanSetting;
    }

    @Override
    public void render(DrawContext context, BooleanSetting setting, SettingRendererContext rendererContext, float x, float y, float width, float scale, float opacity) {
        float checkboxProgress = updateProgress(setting);
        float eased = smoothStep(checkboxProgress);
        float textSize = 7.7f * scale;
        float switchWidth = SWITCH_WIDTH * scale;
        float switchHeight = SWITCH_HEIGHT * scale;
        float knobSize = KNOB_SIZE * scale;
        float switchX = x + width - switchWidth;
        float switchY = y + (SettingRendererContext.SETTING_HEIGHT * scale - switchHeight) / 2f;
        float knobTravel = switchWidth - knobSize;
        float knobX = switchX + knobTravel * eased;
        float knobY = switchY + (switchHeight - knobSize) / 2f;

        Color label = rendererContext.withOpacity(new Color(210, 210, 214), opacity);
        Color track = rendererContext.withOpacity(lerpColor(new Color(86, 86, 86), ThemeManager.getThemeColor(), eased), opacity);
        Color knob = rendererContext.withOpacity(Color.WHITE, opacity);
        Color knobShadow = rendererContext.withOpacity(new Color(0, 0, 0), opacity * 0.18f);

        rendererContext.renderBoundedText(context, setting, "name", setting.getName(), x, y + 4.5f * scale,
                width - switchWidth - 8f * scale, SettingRendererContext.SETTING_HEIGHT * scale, textSize, label,
                false, rendererContext.isHovered(x, y, width, SettingRendererContext.SETTING_HEIGHT * scale));
        RenderUtil.rect(switchX, switchY, switchWidth, switchHeight, switchHeight / 2f, track);
        RenderUtil.shadow(knobX, knobY, knobSize, knobSize, knobSize / 2f, 2f * scale, 0.25f, 1f, knobShadow);
        RenderUtil.rect(knobX, knobY, knobSize, knobSize, knobSize / 2f, knob);
    }

    @Override
    public boolean click(BooleanSetting setting, SettingRendererContext rendererContext, int button, float localX, float localY, float width) {
        setting.setEnabled(!setting.isEnabled());
        return true;
    }

    @Override
    public float getHeight(BooleanSetting setting, SettingRendererContext rendererContext) {
        return SettingRendererContext.SETTING_HEIGHT;
    }

    @Override
    public String getValue(BooleanSetting setting) {
        return "";
    }

    private float updateProgress(BooleanSetting setting) {
        long now = System.currentTimeMillis();
        long last = lastFrameTime.getOrDefault(setting, now);
        float delta = Math.min(50f, now - last);
        lastFrameTime.put(setting, now);

        float current = progress.getOrDefault(setting, setting.isEnabled() ? 1f : 0f);
        float target = setting.isEnabled() ? 1f : 0f;
        if (current < target) {
            current = Math.min(target, current + delta * ANIMATION_SPEED);
        } else if (current > target) {
            current = Math.max(target, current - delta * ANIMATION_SPEED);
        }
        progress.put(setting, current);
        return current;
    }

    private Color lerpColor(Color from, Color to, float progress) {
        float t = Math.clamp(progress, 0f, 1f);
        int red = (int) (from.getRed() + (to.getRed() - from.getRed()) * t);
        int green = (int) (from.getGreen() + (to.getGreen() - from.getGreen()) * t);
        int blue = (int) (from.getBlue() + (to.getBlue() - from.getBlue()) * t);
        int alpha = (int) (from.getAlpha() + (to.getAlpha() - from.getAlpha()) * t);
        return new Color(red, green, blue, alpha);
    }

    private float smoothStep(float progress) {
        float t = Math.clamp(progress, 0f, 1f);
        return t * t * (3f - 2f * t);
    }
}
