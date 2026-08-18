package ez.minar.system.menu.components.settings;

import ez.minar.system.menu.ThemeManager;
import ez.minar.system.settings.Setting;
import ez.minar.system.settings.impl.MultiSetting;
import ez.minar.utils.render.RenderUtil;
import net.minecraft.client.gui.DrawContext;

import java.awt.Color;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MultiSettingRenderer implements SettingRenderer<MultiSetting> {
    private final Map<String, Float> animProgress = new HashMap<>();
    private final Map<Setting, Long> lastFrameTimes = new HashMap<>();
    private final Map<Setting, String> currentMode = new HashMap<>();
    private final Map<Setting, String> previousMode = new HashMap<>();
    private final Map<Setting, Float> textAnimProgress = new HashMap<>();

    private float easeOutBounce(float x) {
        float n1 = 7.5625f;
        float d1 = 2.75f;
        if (x < 1f / d1) {
            return n1 * x * x;
        } else if (x < 2f / d1) {
            return n1 * (x -= 1.5f / d1) * x + 0.75f;
        } else if (x < 2.5f / d1) {
            return n1 * (x -= 2.25f / d1) * x + 0.9375f;
        } else {
            return n1 * (x -= 2.625f / d1) * x + 0.984375f;
        }
    }

    @Override
    public boolean supports(Setting setting) {
        return setting instanceof MultiSetting;
    }

    @Override
    public void render(DrawContext context, MultiSetting setting, SettingRendererContext rendererContext, float x, float y, float width, float scale, float opacity) {
        long now = System.currentTimeMillis();
        long last = lastFrameTimes.getOrDefault(setting, now);
        float delta = Math.min(50f, now - last) / 1000f;
        lastFrameTimes.put(setting, now);

        float totalComponentHeight = getHeight(setting, rendererContext) * scale;
        float rx = x - 4f * scale;
        float rWidth = width + 8f * scale;
        
        RenderUtil.rect(rx, y, rWidth, totalComponentHeight, 4f * scale, opacity < 1f ? new Color(0, 0, 0, (int) (170 * opacity)) : new Color(0, 0, 0, 170));

        // TOP ROW: Name
        Color label = rendererContext.withOpacity(new Color(210, 210, 214), opacity);
        float textSizeName = 7.7f * scale;
        rendererContext.renderBoundedText(context, setting, "name", setting.getName(), x, y + 4.5f * scale,
                width * 0.55f, SettingRendererContext.SETTING_HEIGHT * scale, textSizeName, label, false,
                rendererContext.isHovered(x, y, width, SettingRendererContext.SETTING_HEIGHT * scale));

        // TOP ROW: Animated Value
        String actualMode = getValue(setting);
        String prevMode = previousMode.getOrDefault(setting, actualMode);
        String currMode = currentMode.getOrDefault(setting, actualMode);

        if (!currMode.equals(actualMode)) {
            previousMode.put(setting, currMode);
            currentMode.put(setting, actualMode);
            textAnimProgress.put(setting, 0f);
            prevMode = currMode;
        }

        float animSpeed = 2.5f;
        float textProgress = textAnimProgress.getOrDefault(setting, 1f);
        if (textProgress < 1f) {
            textProgress = Math.min(1f, textProgress + delta * animSpeed);
            textAnimProgress.put(setting, textProgress);
        }

        Color valueColor = rendererContext.withOpacity(new Color(150, 150, 156), opacity);
        float rightX = x + width - width * 0.45f;
        float maxWidth = width * 0.45f;
        float textYBase = y + 4.5f * scale;
        float height = SettingRendererContext.SETTING_HEIGHT * scale;

        ez.minar.utils.render.scissor.Scissor.push(rightX, y, maxWidth, height);
        if (textProgress < 1f) {
            float eased = easeOutBounce(textProgress);
            float offset = eased * height;

            rendererContext.renderBoundedText(context, setting, "value_old", prevMode, rightX, textYBase - offset,
                    maxWidth, height, textSizeName, rendererContext.withOpacity(valueColor, opacity * (1f - textProgress)), true, false);
            rendererContext.renderBoundedText(context, setting, "value_new", actualMode, rightX, textYBase + height - offset,
                    maxWidth, height, textSizeName, rendererContext.withOpacity(valueColor, opacity * textProgress), true, false);
        } else {
            rendererContext.renderBoundedText(context, setting, "value", actualMode, rightX, textYBase,
                    maxWidth, height, textSizeName, valueColor, true,
                    rendererContext.isHovered(x, y, width, SettingRendererContext.SETTING_HEIGHT * scale));
        }
        ez.minar.utils.render.scissor.Scissor.pop();

        List<String> options = setting.getOptionsList();

        float startY = y + SettingRendererContext.SETTING_HEIGHT * scale;
        
        // Render left vertical line background
        float totalHeight = options.size() * SettingRendererContext.SETTING_HEIGHT * scale;
        RenderUtil.rect(x, startY + 2f * scale, 1.5f * scale, totalHeight - 4f * scale, 0.75f * scale, rendererContext.withOpacity(new Color(60, 60, 65, 100), opacity));

        float optionY = startY;
        float textSize = 7.2f * scale;

        for (String option : options) {
            String key = System.identityHashCode(setting) + ":" + option;
            boolean isEnabled = setting.isEnabled(option);
            float currentProgress = animProgress.getOrDefault(key, isEnabled ? 1f : 0f);
            
            if (isEnabled && currentProgress < 1f) {
                currentProgress = Math.min(1f, currentProgress + delta * 8f);
            } else if (!isEnabled && currentProgress > 0f) {
                currentProgress = Math.max(0f, currentProgress - delta * 8f);
            }
            animProgress.put(key, currentProgress);

            if (currentProgress > 0.01f) {
                RenderUtil.rect(x, optionY + 2f * scale, 1.5f * scale, SettingRendererContext.SETTING_HEIGHT * scale - 4f * scale, 0.75f * scale, 
                        rendererContext.withOpacity(ThemeManager.getThemeColor(), opacity * currentProgress));
            }

            Color idleColor = rendererContext.withOpacity(new Color(170, 170, 175), opacity);
            Color activeColor = rendererContext.withOpacity(ThemeManager.getThemeColor(), opacity);
            Color color = lerpColor(idleColor, activeColor, currentProgress);
            
            rendererContext.renderBoundedText(context, setting, "option:" + option, option, x + 6f * scale, optionY + 4.2f * scale,
                    width - 6f * scale, SettingRendererContext.SETTING_HEIGHT * scale, textSize, color, false,
                    rendererContext.isHovered(x, optionY, width, SettingRendererContext.SETTING_HEIGHT * scale));
            
            optionY += SettingRendererContext.SETTING_HEIGHT * scale;
        }
    }

    private Color lerpColor(Color from, Color to, float progress) {
        float t = Math.clamp(progress, 0f, 1f);
        int red = (int) (from.getRed() + (to.getRed() - from.getRed()) * t);
        int green = (int) (from.getGreen() + (to.getGreen() - from.getGreen()) * t);
        int blue = (int) (from.getBlue() + (to.getBlue() - from.getBlue()) * t);
        int alpha = (int) (from.getAlpha() + (to.getAlpha() - from.getAlpha()) * t);
        return new Color(red, green, blue, alpha);
    }

    @Override
    public boolean click(MultiSetting setting, SettingRendererContext rendererContext, int button, float localX, float localY, float width) {
        if (button != 0) return false;

        List<String> options = setting.getOptionsList();
        if (localY >= SettingRendererContext.SETTING_HEIGHT) {
            int index = (int) ((localY - SettingRendererContext.SETTING_HEIGHT) / SettingRendererContext.SETTING_HEIGHT);
            if (index >= 0 && index < options.size()) {
                setting.toggle(options.get(index));
            }
            return true;
        }

        return false;
    }

    @Override
    public float getHeight(MultiSetting setting, SettingRendererContext rendererContext) {
        return SettingRendererContext.SETTING_HEIGHT * (1f + setting.getOptionsList().size());
    }

    @Override
    public String getValue(MultiSetting setting) {
        return setting.getEnabled().size() + " options";
    }
}
