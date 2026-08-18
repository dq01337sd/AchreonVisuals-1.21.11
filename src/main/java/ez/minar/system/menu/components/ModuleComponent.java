package ez.minar.system.menu.components;

import ez.minar.system.api.Function;
import ez.minar.system.menu.ThemeManager;
import ez.minar.system.menu.components.settings.SettingRenderer;
import ez.minar.system.menu.components.settings.SettingRendererContext;
import ez.minar.system.menu.components.settings.SettingRendererRegistry;
import ez.minar.system.settings.Setting;
import ez.minar.system.settings.impl.KeybindSetting;
import ez.minar.system.settings.impl.TextSetting;
import ez.minar.utils.render.RenderUtil;
import ez.minar.utils.render.msdf.Msdf;
import ez.minar.utils.render.scissor.Scissor;
import net.minecraft.client.gui.DrawContext;
import org.lwjgl.glfw.GLFW;

import java.awt.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ModuleComponent {
    private static final float HEIGHT = 21f;
    private static final float TEXT_SIZE = 8.8f;
    private static final float PADDING = 8f;
    private static final float COLOR_ANIMATION_SPEED = 0.0045f;
    private static final float EXPAND_ANIMATION_SPEED = 0.006f;
    private static final float SETTING_EXPAND_ANIMATION_SPEED = 0.01f;
    private static final float BIND_ANIMATION_SPEED = 0.01f;
    private static final float SETTINGS_GAP = 3f;
    private static ModuleComponent bindingComponent;
    private static final SettingRendererRegistry SETTING_RENDERERS = new SettingRendererRegistry();

    private final Function function;
    private final SettingRendererContext settingRendererContext;

    private float x;
    private float y;
    private float width;
    private float scale = 1f;
    private float enabledProgress;
    private float expandProgress;
    private float bindProgress;
    private long lastFrameTime;
    private boolean expanded;
    private boolean binding;
    private KeybindSetting bindingSetting;
    private Setting draggingSetting;
    private final Set<Setting> expandedSettings = new HashSet<>();
    private final Map<Setting, Float> settingExpandProgress = new HashMap<>();
    private float nameScrollOffset;
    private long nameScrollUpdateTime;
    private float nameScrollElapsed;
    private boolean wasHovered;

    public ModuleComponent(Function function) {
        this.function = function;
        this.settingRendererContext = new SettingRendererContext(
                expandedSettings::contains,
                setting -> settingExpandProgress.getOrDefault(setting, 0f),
                this::toggleExpandedSetting
        );
        this.enabledProgress = function.isEnabled() ? 1f : 0f;
        this.lastFrameTime = System.currentTimeMillis();
    }

    public Function getFunction() {
        return function;
    }


    public void render(DrawContext context, float x, float y, float width, float mouseX, float mouseY) {
        render(context, x, y, width, mouseX, mouseY, 1f, 1f);
    }

    public void render(DrawContext context, float x, float y, float width, float mouseX, float mouseY, float scale, float opacity) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.scale = scale;

        updateAnimations();
        boolean enabled = function.isEnabled();
        float settingsHeight = getSettingsHeight() * expandProgress;
        float settingsGap = SETTINGS_GAP * expandProgress;
        float height = (HEIGHT + settingsGap + settingsHeight) * scale;
        boolean hovered = isInside(mouseX, mouseY, x, y, width, HEIGHT * scale);

        Color disabledBackground = new Color(255, 255, 255, 10);
        Color enabledBase = new Color(ThemeManager.getThemeColor().getRed(), ThemeManager.getThemeColor().getGreen(), ThemeManager.getThemeColor().getBlue(), 140);
        Color background = lerpColor(disabledBackground, enabledBase, enabledProgress);
        Color text = enabled ? new Color(255, 255, 255) : (hovered ? new Color(230, 230, 235) : new Color(190, 190, 195));

        if (hovered && !wasHovered) {
            net.minecraft.client.MinecraftClient.getInstance().getSoundManager().play(net.minecraft.client.sound.PositionedSoundInstance.ui(net.minecraft.sound.SoundEvent.of(net.minecraft.util.Identifier.of("atheryx", "scroll")), 1.0f, 1.0f));
        }
        wasHovered = hovered;

        float rectOpacity = opacity * opacity * opacity;
        if (rectOpacity > 0.02f) {
            Color settingsColor = new Color(255, 255, 255, 12);
            if (expandProgress > 0.02f) {
                float topRadius = (4f - 4f * expandProgress) * scale;
                float bottomRadius = (4f + 2f * expandProgress) * scale;
                RenderUtil.rect(x, y, width, HEIGHT * scale, topRadius, topRadius, bottomRadius, bottomRadius, withOpacity(background, rectOpacity));
                RenderUtil.rect(x, y + (HEIGHT + settingsGap) * scale, width, settingsHeight * scale, 6f * scale, 6f * scale, 0f, 0f, withOpacity(settingsColor, rectOpacity * expandProgress));
            } else {
                RenderUtil.rect(x, y, width, height, 4f * scale, withOpacity(background, rectOpacity));
            }
        }
        float switchWidth = 20f * scale;
        float switchHeight = 9f * scale;
        float knobSize = 7f * scale;
        float switchX = x + width - PADDING * scale - switchWidth;
        float switchY = y + (HEIGHT * scale - switchHeight) / 2f;

        float textX = x + PADDING * scale;
        float textY = y + 2.6f * scale;
        float textWidth = width - PADDING * 2f * scale - switchWidth - 4f * scale;
        float textSize = TEXT_SIZE * scale;

        String renderName = function.getName();
        float actualTextWidth = Msdf.width(renderName, textSize);

        if (actualTextWidth <= textWidth) {
            RenderUtil.text(context, textX, textY, renderName, textSize, withOpacity(text, opacity * (1f - bindProgress)));
            nameScrollOffset = 0f;
            nameScrollUpdateTime = 0L;
            nameScrollElapsed = 0f;
        } else {
            float overflow = actualTextWidth - textWidth + 6f * scale;
            long now = System.currentTimeMillis();
            long last = nameScrollUpdateTime == 0L ? now : nameScrollUpdateTime;
            float delta = Math.min(50f, now - last) / 1000f;
            nameScrollUpdateTime = now;

            if (!hovered) {
                if (nameScrollOffset > 0f) {
                    nameScrollOffset = Math.max(0f, nameScrollOffset - 26f * 1.8f * delta);
                } else if (nameScrollOffset < 0f) {
                    nameScrollOffset = 0f;
                }
                nameScrollElapsed = 0f;
            } else {
                nameScrollElapsed += delta;
                float cycleDuration = Math.max(1.8f, overflow * 2f / 26f);
                float phase = (nameScrollElapsed % cycleDuration) / cycleDuration;
                nameScrollOffset = overflow * (0.5f - 0.5f * (float) Math.cos(phase * Math.PI * 2f));
            }

            Scissor.push(textX, textY - 2f * scale, textWidth, textSize + 6f * scale);
            RenderUtil.text(context, textX - nameScrollOffset, textY, renderName, textSize, withOpacity(text, opacity * (1f - bindProgress)));
            Scissor.pop();
        }

        RenderUtil.text(context, textX, textY, trimToWidth("Нажмите бинд", textWidth, textSize), textSize, withOpacity(text, opacity * bindProgress));

        String desc = function.getDesc();
        if (desc != null && !desc.isEmpty()) {
            float descSize = 5.4f * scale;
            float descY = y + (HEIGHT * scale) - 9f * scale;
            float descWidth = width - PADDING * 2f * scale - 2f * scale;
            RenderUtil.text(context, textX, descY, trimToWidth(desc, descWidth, descSize), descSize, withOpacity(new Color(150, 150, 157), opacity * (1f - bindProgress)));
        }

        if (rectOpacity > 0.02f) {
            float eased = easeInOut(enabledProgress);
            float knobTravel = switchWidth - knobSize;
            float knobX = switchX + knobTravel * eased;
            float knobY = switchY + (switchHeight - knobSize) / 2f;

            Color trackBg = new Color(90, 90, 90, 80);
            Color trackActive = ThemeManager.getThemeColor();
            Color track = lerpColor(trackBg, trackActive, eased);

            RenderUtil.rect(switchX, switchY, switchWidth, switchHeight, 3, withOpacity(track, rectOpacity));

            Color knobColor = withOpacity(Color.WHITE, rectOpacity);
            RenderUtil.rect(knobX, knobY, knobSize, knobSize, knobSize / 2f, knobColor);
        }

        if (expandProgress > 0.02f) {
            float settingY = y + (HEIGHT + settingsGap) * scale;
            float settingOpacity = opacity * expandProgress;
            for (Setting setting : visibleSettings()) {
                float settingHeight = getSettingHeight(setting) * scale;
                renderSetting(context, setting, x + PADDING * scale, settingY, width - PADDING * 2f * scale, mouseX, mouseY, scale, settingOpacity);
                settingY += settingHeight;
            }
        }
    }

    public boolean mouseClicked(float mouseX, float mouseY, int button) {
        if (button == 0) {
            stopEditingTextSettings();
        }

        if (bindingSetting != null) {
            if (KeybindSetting.isBindableMouseButton(button)) {
                bindingSetting.setKeybind(KeybindSetting.toMouseKey(button));
                stopBinding();
                return true;
            }

            return button == 0;
        }

        if (binding) {
            if (KeybindSetting.isBindableMouseButton(button)) {
                function.setKeybind(KeybindSetting.toMouseKey(button));
                stopBinding();
                return true;
            }

            return button == 0;
        }

        if (button != 0 && button != 1 && button != 2) {
            return false;
        }

        if (expanded && expandProgress > 0.85f) {
            float settingY = y + HEIGHT + SETTINGS_GAP;
            for (Setting setting : visibleSettings()) {
                float settingHeight = getSettingHeight(setting);
                if (isInside(mouseX, mouseY, x, settingY, width, settingHeight)) {
                    float settingX = x + PADDING * scale;
                    float settingWidth = width - PADDING * 2f * scale;
                    if (clickSetting(setting, button, mouseX - settingX, mouseY - settingY, settingWidth)) {
                        draggingSetting = setting;
                    }
                    return true;
                }
                settingY += settingHeight;
            }
        }

        if (!isInside(mouseX, mouseY, x, y, width, HEIGHT)) {
            return false;
        }

        if (button == 0) {
            function.toggle();
        } else if (button == 1) {
            expanded = !expanded;
            net.minecraft.client.MinecraftClient.getInstance().getSoundManager().play(net.minecraft.client.sound.PositionedSoundInstance.ui(net.minecraft.sound.SoundEvent.of(net.minecraft.util.Identifier.of("atheryx", "switchcategory")), 1.0f, 1.0f));
        } else {
            startBinding();
        }
        return true;
    }

    public boolean mouseDragged(float mouseX, float mouseY, int button) {
        if (draggingSetting == null) {
            return false;
        }

        SettingRenderer<Setting> renderer = SETTING_RENDERERS.get(draggingSetting);
        float settingX = x + PADDING * scale;
        float settingWidth = width - PADDING * 2f * scale;
        float settingY = getSettingY(draggingSetting);
        return renderer.drag(draggingSetting, settingRendererContext, button, mouseX - settingX, mouseY - settingY, settingWidth);
    }

    public void mouseReleased() {
        draggingSetting = null;
    }

    public boolean keyPressed(int keyCode) {
        TextSetting editingText = getEditingTextSetting();
        if (editingText != null) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
                editingText.setEditing(false);
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
                editingText.backspace();
                return true;
            }
            return true;
        }

        if (bindingSetting != null) {
            bindingSetting.setKeybind(keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_DELETE ? 0 : keyCode);
            bindingSetting = null;
            stopBinding();
            return true;
        }

        if (!binding) {
            return false;
        }

        function.setKeybind(keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_DELETE ? 0 : keyCode);
        stopBinding();
        return true;
    }


    public boolean charTyped(String text) {
        TextSetting editingText = getEditingTextSetting();
        if (editingText == null) {
            return false;
        }

        editingText.append(text);
        return true;
    }

    public float getHeight() {
        return HEIGHT + (SETTINGS_GAP + getSettingsHeight()) * expandProgress;
    }


    private void updateAnimations() {
        long now = System.currentTimeMillis();
        float delta = Math.min(50f, now - lastFrameTime);
        lastFrameTime = now;

        float target = function.isEnabled() ? 1f : 0f;
        if (enabledProgress < target) {
            enabledProgress = Math.min(target, enabledProgress + delta * COLOR_ANIMATION_SPEED);
        } else if (enabledProgress > target) {
            enabledProgress = Math.max(target, enabledProgress - delta * COLOR_ANIMATION_SPEED);
        }

        float expandTarget = expanded ? 1f : 0f;
        if (expandProgress < expandTarget) {
            expandProgress = Math.min(expandTarget, expandProgress + delta * EXPAND_ANIMATION_SPEED);
        } else if (expandProgress > expandTarget) {
            expandProgress = Math.max(expandTarget, expandProgress - delta * EXPAND_ANIMATION_SPEED);
        }

        float bindTarget = binding ? 1f : 0f;
        if (bindProgress < bindTarget) {
            bindProgress = Math.min(bindTarget, bindProgress + delta * BIND_ANIMATION_SPEED);
        } else if (bindProgress > bindTarget) {
            bindProgress = Math.max(bindTarget, bindProgress - delta * BIND_ANIMATION_SPEED);
        }

        for (Setting setting : visibleSettings()) {
            float current = settingExpandProgress.getOrDefault(setting, 0f);
            float targetProgress = expandedSettings.contains(setting) ? 1f : 0f;
            if (current < targetProgress) {
                current = Math.min(targetProgress, current + delta * SETTING_EXPAND_ANIMATION_SPEED);
            } else if (current > targetProgress) {
                current = Math.max(targetProgress, current - delta * SETTING_EXPAND_ANIMATION_SPEED);
            }
            settingExpandProgress.put(setting, current);
        }
    }

    private void startBinding() {
        if (bindingComponent != null && bindingComponent != this) {
            bindingComponent.stopBinding();
        }

        bindingComponent = this;
        binding = true;
    }

    private void stopBinding() {
        binding = false;
        bindingSetting = null;
        if (bindingComponent == this) {
            bindingComponent = null;
        }
    }

    private void renderSetting(DrawContext context, Setting setting, float x, float y, float width, float mouseX, float mouseY, float scale, float opacity) {
        SettingRenderer<Setting> renderer = SETTING_RENDERERS.get(setting);
        settingRendererContext.setMouse(mouseX, mouseY);
        renderer.render(context, setting, settingRendererContext, x, y, width, scale, opacity);
    }

    private boolean clickSetting(Setting setting, int button, float localX, float localY, float width) {
        SettingRenderer<Setting> renderer = SETTING_RENDERERS.get(setting);
        boolean handled = renderer.click(setting, settingRendererContext, button, localX, localY, width);
        if (handled && setting instanceof KeybindSetting keybindSetting && button == 0) {
            startBinding();
            bindingSetting = keybindSetting;
            keybindSetting.setKeybind(-1);
        }
        return handled;
    }

    private void stopEditingTextSettings() {
        for (Setting setting : function.getSettings()) {
            if (setting instanceof TextSetting textSetting) {
                textSetting.setEditing(false);
            }
        }
    }

    private TextSetting getEditingTextSetting() {
        for (Setting setting : function.getSettings()) {
            if (setting instanceof TextSetting textSetting && textSetting.isEditing()) {
                return textSetting;
            }
        }
        return null;
    }

    private float getSettingsHeight() {
        float height = 0f;
        for (Setting setting : visibleSettings()) {
            height += getSettingHeight(setting);
        }
        return height;
    }

    private float getSettingHeight(Setting setting) {
        SettingRenderer<Setting> renderer = SETTING_RENDERERS.get(setting);
        return renderer.getHeight(setting, settingRendererContext);
    }

    private float getSettingY(Setting targetSetting) {
        float settingY = y + HEIGHT + SETTINGS_GAP;
        for (Setting setting : visibleSettings()) {
            if (setting == targetSetting) {
                return settingY;
            }
            settingY += getSettingHeight(setting);
        }
        return y;
    }

    private void toggleExpandedSetting(Setting setting) {
        if (expandedSettings.contains(setting)) {
            expandedSettings.remove(setting);
        } else {
            expandedSettings.add(setting);
        }
        net.minecraft.client.MinecraftClient.getInstance().getSoundManager().play(net.minecraft.client.sound.PositionedSoundInstance.ui(net.minecraft.sound.SoundEvent.of(net.minecraft.util.Identifier.of("atheryx", "switchcategory")), 1.0f, 1.0f));
    }

    private List<Setting> visibleSettings() {
        return function.getSettings().stream()
                .filter(Setting::isVisible)
                .toList();
    }

    private boolean isInside(float mouseX, float mouseY, float x, float y, float width, float height) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

    private Color lerpColor(Color from, Color to, float progress) {
        float t = easeInOut(Math.clamp(progress, 0f, 1f));
        int red = (int) (from.getRed() + (to.getRed() - from.getRed()) * t);
        int green = (int) (from.getGreen() + (to.getGreen() - from.getGreen()) * t);
        int blue = (int) (from.getBlue() + (to.getBlue() - from.getBlue()) * t);
        int alpha = (int) (from.getAlpha() + (to.getAlpha() - from.getAlpha()) * t);
        return new Color(red, green, blue, alpha);
    }

    private float easeInOut(float progress) {
        return progress < 0.5f ? 2f * progress * progress : 1f - (float) Math.pow(-2f * progress + 2f, 2f) / 2f;
    }

    private Color withOpacity(Color color, float opacity) {
        int alpha = (int) (color.getAlpha() * Math.clamp(opacity, 0f, 1f));
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
    }

    private String trimToWidth(String text, float maxWidth, float size) {
        if (Msdf.width(text, size) <= maxWidth) {
            return text;
        }

        String ellipsis = "...";
        String trimmed = text;
        while (!trimmed.isEmpty() && Msdf.width(trimmed + ellipsis, size) > maxWidth) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed.isEmpty() ? ellipsis : trimmed + ellipsis;
    }
}
