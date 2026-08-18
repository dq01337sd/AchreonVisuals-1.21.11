package ez.minar.system.menu.components.settings;

import ez.minar.system.menu.ThemeManager;
import ez.minar.system.settings.Setting;
import ez.minar.system.settings.impl.NumberSetting;
import ez.minar.utils.render.RenderUtil;
import net.minecraft.client.gui.DrawContext;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class NumberSettingRenderer implements SettingRenderer<NumberSetting> {
    private static final float HEIGHT = 25f;
    private static final float TRACK_HEIGHT = 5.5f;
    private static final float KNOB_SIZE = 9f;
    private static final float DISPLAY_VALUE_SPEED = 0.012f;
    private static final float SLIDER_ANIMATION_SPEED = 0.02f;

    private final Map<NumberSetting, Double> displayedValues = new HashMap<>();
    private final Map<NumberSetting, Float> displayedProgress = new HashMap<>();
    private final Map<NumberSetting, Long> lastFrameTime = new HashMap<>();

    @Override
    public boolean supports(Setting setting) {
        return setting instanceof NumberSetting;
    }

    @Override
    public void render(DrawContext context, NumberSetting setting, SettingRendererContext rendererContext, float x, float y, float width, float scale, float opacity) {
        float delta = updateFrameTime(setting);
        float textSize = 7.7f * scale;
        float sliderWidth = width;
        float trackHeight = TRACK_HEIGHT * scale;
        float knobSize = KNOB_SIZE * scale;
        float sliderX = x;
        float sliderY = y + 16.5f * scale;
        float progress = getDisplayProgress(setting, delta);
        float fillWidth = Math.max(trackHeight, sliderWidth * progress);
        float knobX = sliderX + (sliderWidth - knobSize) * progress;
        float knobY = sliderY + (trackHeight - knobSize) / 2f;

        Color label = rendererContext.withOpacity(new Color(210, 210, 214), opacity);
        Color value = rendererContext.withOpacity(new Color(150, 150, 156), opacity);
        Color track = rendererContext.withOpacity(new Color(64, 64, 68), opacity);
        Color fill = rendererContext.withOpacity(ThemeManager.getThemeColor(), opacity);
        Color knob = rendererContext.withOpacity(ThemeManager.getThemeColor(), opacity);
        Color knobShadow = rendererContext.withOpacity(new Color(0, 0, 0), opacity * 0.18f);

        boolean hovered = rendererContext.isHovered(x, y, width, HEIGHT * scale);
        rendererContext.renderBoundedText(context, setting, "name", setting.getName(), x, y + 4.2f * scale,
                width * 0.58f, HEIGHT * scale, textSize, label, false, hovered);
        rendererContext.renderBoundedText(context, setting, "value", getDisplayValue(setting, delta), x + width - width * 0.38f, y + 4.2f * scale,
                width * 0.38f, HEIGHT * scale, textSize, value, true, hovered);
        RenderUtil.rect(sliderX, sliderY, sliderWidth, trackHeight, trackHeight / 2f, track);
        RenderUtil.rect(sliderX, sliderY, fillWidth, trackHeight, trackHeight / 2f, fill);
        RenderUtil.shadow(knobX, knobY, knobSize, knobSize, knobSize / 2f, 2.3f * scale, 0.25f, 1f, knobShadow);
        RenderUtil.rect(knobX, knobY, knobSize, knobSize, knobSize / 2f, knob);
    }

    @Override
    public boolean click(NumberSetting setting, SettingRendererContext rendererContext, int button, float localX, float localY, float width) {
        if (button == 1) {
            setting.setValue(setting.getValue() - setting.getStep());
            return false;
        }

        updateValueFromMouse(setting, localX, width, 1f);
        return true;
    }

    @Override
    public boolean drag(NumberSetting setting, SettingRendererContext rendererContext, int button, float localX, float localY, float width) {
        if (button != 0) {
            return false;
        }

        updateValueFromMouse(setting, localX, width, 1f);
        return true;
    }

    @Override
    public float getHeight(NumberSetting setting, SettingRendererContext rendererContext) {
        return HEIGHT;
    }

    @Override
    public String getValue(NumberSetting setting) {
        return formatValue(setting, setting.getValue());
    }

    private float getProgress(NumberSetting setting) {
        double range = setting.getMax() - setting.getMin();
        if (range <= 0d) {
            return 0f;
        }

        return Math.clamp((float) ((setting.getValue() - setting.getMin()) / range), 0f, 1f);
    }

    private String getDisplayValue(NumberSetting setting, float delta) {
        double target = setting.getValue();
        double current = displayedValues.getOrDefault(setting, target);
        double factor = 1d - Math.exp(-DISPLAY_VALUE_SPEED * delta);
        current += (target - current) * Math.clamp((float) factor, 0f, 1f);

        if (Math.abs(target - current) <= Math.max(0.0001d, setting.getStep() * 0.08d)) {
            current = target;
        }

        displayedValues.put(setting, current);
        return formatValue(setting, current);
    }

    private float getDisplayProgress(NumberSetting setting, float delta) {
        float target = getProgress(setting);
        float current = displayedProgress.getOrDefault(setting, target);
        float factor = 1f - (float) Math.exp(-SLIDER_ANIMATION_SPEED * delta);
        current += (target - current) * Math.clamp(factor, 0f, 1f);

        if (Math.abs(target - current) <= 0.001f) {
            current = target;
        }

        displayedProgress.put(setting, current);
        return current;
    }

    private float updateFrameTime(NumberSetting setting) {
        long now = System.currentTimeMillis();
        long last = lastFrameTime.getOrDefault(setting, now);
        float delta = Math.min(50f, now - last);
        lastFrameTime.put(setting, now);
        return delta;
    }

    private String formatValue(NumberSetting setting, double value) {
        double step = setting.getStep();
        if (step >= 1d || value == Math.rint(value)) {
            return String.valueOf((int) Math.round(value));
        }

        return String.format("%.1f", value);
    }

    private void updateValueFromMouse(NumberSetting setting, float localX, float width, float scale) {
        float sliderWidth = width;
        float sliderX = 0f;
        float progress = Math.clamp((localX - sliderX) / sliderWidth, 0f, 1f);
        double range = setting.getMax() - setting.getMin();
        double rawValue = setting.getMin() + range * progress;
        double step = setting.getStep();
        double steppedValue = step > 0d ? Math.round(rawValue / step) * step : rawValue;
        setting.setValue(steppedValue);
    }
}
