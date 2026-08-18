package ez.minar.system.menu;

import ez.minar.system.api.Function;
import ez.minar.system.api.FunctionManager;
import ez.minar.system.features.misc.Ambience;
import ez.minar.system.features.misc.AntiAFK;
import ez.minar.system.features.misc.Bots;
import ez.minar.system.menu.components.ModuleComponent;
import ez.minar.utils.math.Easings;
import ez.minar.utils.render.RenderUtil;
import ez.minar.utils.render.msdf.Msdf;
import ez.minar.utils.render.scissor.Scissor;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class ClickGui extends Screen {
    private static final int CAT_MISC = 0;
    private static final int CAT_VISUAL = 1;
    private static final int CAT_THEMES = 2;

    private static final float SIDEBAR_WIDTH = 92f;
    private static final float MODULE_PANEL_WIDTH = 330f;
    private static final float PANEL_GAP = 6f;
    private static final float PANEL_HEIGHT = 250f;
    private static final float PANEL_PADDING = 8f;
    private static final float SIDEBAR_HEADER_HEIGHT = 30f;
    private static final float CATEGORY_ITEM_HEIGHT = 32f;
    private static final float MODULE_HEADER_HEIGHT = 28f;
    private static final float SEARCH_HEIGHT = 24f;
    private static final float TELEGRAM_WIDTH = 108f;
    private static final float MODULE_HEIGHT = 21f;
    private static final float MODULE_ROW_GAP = 4f;
    private static final float THEME_ITEM_HEIGHT = 22f;
    private static final float THEME_GRADIENT_HEIGHT = 42f;
    private static final float THEME_MENU_WIDTH = 150f;
    private static final float THEME_MENU_ROW_HEIGHT = 18f;
    private static final float PICKER_WIDTH = 132f;
    private static final float PICKER_HEIGHT = 108f;
    private static final float PICKER_FIELD_SIZE = 78f;
    private static final float PICKER_HUE_WIDTH = 10f;
    private static final float ANIMATION_DURATION = 180f;
    private static final float PICKER_ANIMATION_DURATION = 180f;
    private static final float MENU_ANIMATION_SPEED = 0.02f;
    private static final float PULSE_ANIMATION_SPEED = 0.018f;
    private static final float SCROLL_ANIMATION_SPEED = 0.025f;
    private static final float SCROLL_STEP = MODULE_HEIGHT + MODULE_ROW_GAP;

    private final List<ModuleComponent> miscModules = new ArrayList<>();
    private final List<ModuleComponent> visualModules = new ArrayList<>();
    private int selectedCategory = CAT_MISC;
    private float scrollOffset;
    private float targetScrollOffset;
    private long animationStart;
    private boolean closing;
    private boolean pickerOpen;
    private boolean draggingPickerField;
    private boolean draggingPickerHue;
    private int editingCustomTheme = -1;
    private float pickerHue;
    private float pickerSaturation;
    private float pickerBrightness;
    private float pickerHue2;
    private float pickerSaturation2;
    private float pickerBrightness2;
    private boolean draggingPickerField2;
    private boolean draggingPickerHue2;
    private float pickerProgress;
    private long pickerAnimationStart;
    private long lastUiFrameTime;
    private float selectedPulse;
    private float contextMenuProgress;
    private int contextThemeIndex = Integer.MIN_VALUE;
    private float contextMenuX;
    private float contextMenuY;
    private boolean plusMenuOpen;
    private float plusMenuProgress;
    private long plusMenuAnimationStart;
    private ModuleComponent hoveredComponent;
    private String displayedTooltip = "";
    private String prevTooltip = "";
    private long tooltipChangeStart = 0L;

    private String searchText = "";
    private boolean searchFocused = false;
    private float placeholderAnimation = 0f;
    private static class AnimatedChar {
        String character;
        float animation;
        AnimatedChar(String character) {
            this.character = character;
            this.animation = 0f;
        }
    }
    private final List<AnimatedChar> searchChars = new ArrayList<>();

    public ClickGui() {
        super(Text.of("Achrone"));
        rebuildModules();
    }

    @Override
    public void onDisplayed() {
        closing = false;
        animationStart = System.currentTimeMillis();
        lastUiFrameTime = 0L;
        pickerProgress = pickerOpen ? 1f : 0f;
        plusMenuOpen = false;
        plusMenuProgress = 0f;
        float pickerOffset = pickerOpen ? pickerProgress : 1f - pickerProgress;
        pickerAnimationStart = System.currentTimeMillis() - (long) (pickerOffset * PICKER_ANIMATION_DURATION);
        contextMenuProgress = isContextMenuOpen() ? 1f : 0f;
        net.minecraft.client.MinecraftClient.getInstance().getSoundManager().play(net.minecraft.client.sound.PositionedSoundInstance.ui(net.minecraft.sound.SoundEvent.of(net.minecraft.util.Identifier.of("atheryx", "gui_open")), 1.0f, 1.0f));
        super.onDisplayed();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        float fixedMouseX = RenderUtil.convertX(mouseX);
        float fixedMouseY = RenderUtil.convertY(mouseY);
        updateUiAnimations();
        hoveredComponent = null;
        float animation = getAnimation();
        if (closing && animation <= 0.01f) {
            client.setScreen(null);
            return;
        }

        float eased = Easings.OutCubic(animation);
        float scale = 0.86f + eased * 0.14f;
        float opacity = eased;
        float centerX = RenderUtil.getFixedScaledWidth() / 2f;
        float centerY = RenderUtil.getFixedScaledHeight() / 2f;

        renderSidebar(context, fixedMouseX, fixedMouseY, centerX, centerY, scale, opacity);
        renderModulePanel(context, fixedMouseX, fixedMouseY, centerX, centerY, scale, opacity);

        if (hoveredComponent != null && hoveredComponent.getFunction().getDesc() != null) {
            String desc = hoveredComponent.getFunction().getDesc();
            if (displayedTooltip.isEmpty()) {
                displayedTooltip = desc;
                prevTooltip = "";
                tooltipChangeStart = System.currentTimeMillis();
            } else if (!desc.equals(displayedTooltip)) {
                prevTooltip = displayedTooltip;
                displayedTooltip = desc;
                tooltipChangeStart = System.currentTimeMillis();
            }
        } else {
            if (!displayedTooltip.isEmpty()) {
                prevTooltip = displayedTooltip;
                displayedTooltip = "";
                tooltipChangeStart = System.currentTimeMillis();
            }
        }

        renderTooltipAnim(context, fixedMouseX, fixedMouseY, scale, opacity);

        super.render(context, mouseX, mouseY, deltaTicks);
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float deltaTicks) {}

    @Override
    public void close() {
        if (closing) {
            return;
        }

        ThemeManager.save();
        closing = true;
        animationStart = System.currentTimeMillis();
        net.minecraft.client.MinecraftClient.getInstance().getSoundManager().play(net.minecraft.client.sound.PositionedSoundInstance.ui(net.minecraft.sound.SoundEvent.of(net.minecraft.util.Identifier.of("atheryx", "gui_close")), 1.0f, 1.0f));
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (closing || getAnimation() < 0.95f) {
            return true;
        }

        float fixedMouseX = RenderUtil.convertX((float) click.x());
        float fixedMouseY = RenderUtil.convertY((float) click.y());
        int button = click.button();

        if (button == 0 || button == 1) {
            if (handleSidebarClick(fixedMouseX, fixedMouseY, button)) {
                return true;
            }
        }

        if (button == 0 || button == 1) {
            float searchX = modulePanelFixedX() + PANEL_PADDING;
            float searchY = panelFixedY() + MODULE_HEADER_HEIGHT + 6f;
            searchFocused = isInside(fixedMouseX, fixedMouseY, searchX, searchY, 140f, SEARCH_HEIGHT);
            if (searchFocused) {
                return true;
            }
        }

        if (button == 0) {
            float teleX = modulePanelFixedX() + MODULE_PANEL_WIDTH - PANEL_PADDING - TELEGRAM_WIDTH;
            float teleY = panelFixedY() + MODULE_HEADER_HEIGHT + 6f;
            if (isInside(fixedMouseX, fixedMouseY, teleX, teleY, TELEGRAM_WIDTH, SEARCH_HEIGHT)) {
                net.minecraft.util.Util.getOperatingSystem().open("https://t.me/AchroneVisuals");
                return true;
            }
        }

        if (handleThemeMenuClick(fixedMouseX, fixedMouseY, button)) {
            return true;
        }

        if (selectedCategory == CAT_MISC || selectedCategory == CAT_VISUAL) {
            List<ModuleComponent> components = getActiveModules();
            float viewportX = modulePanelFixedX() + (MODULE_PANEL_WIDTH - getModuleWidth()) / 2f;
            float viewportY = getModuleViewportY();
            float viewportHeight = getModuleViewportHeight();

            if (isInside(fixedMouseX, fixedMouseY, viewportX, viewportY, getModuleWidth(), viewportHeight)) {
                for (ModuleComponent component : components) {
                    if (component.mouseClicked(fixedMouseX, fixedMouseY, button)) {
                        return true;
                    }
                }
            }
        }

        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        int keyCode = input.key();
        for (ModuleComponent component : getActiveModules()) {
            if (component.keyPressed(keyCode)) {
                if (keyCode == 259) playKeyboardSound();
                return true;
            }
        }

        if (searchFocused) {
            if (keyCode == 259 && !searchText.isEmpty()) {
                searchText = searchText.substring(0, searchText.length() - 1);
                if (!searchChars.isEmpty()) {
                    searchChars.remove(searchChars.size() - 1);
                }
                rebuildModules();
                playKeyboardSound();
                return true;
            } else if (keyCode == 257 || keyCode == 335 || keyCode == 256) {
                searchFocused = false;
                return true;
            }
        }

        return super.keyPressed(input);
    }

    @Override
    public boolean charTyped(CharInput input) {
        if (!input.isValidChar()) {
            return super.charTyped(input);
        }

        for (ModuleComponent component : getActiveModules()) {
            if (component.charTyped(input.asString())) {
                playKeyboardSound();
                return true;
            }
        }

        if (searchFocused) {
            searchText += input.asString();
            searchChars.add(new AnimatedChar(input.asString()));
            rebuildModules();
            playKeyboardSound();
            return true;
        }

        return super.charTyped(input);
    }

    private void playKeyboardSound() {
        int randomId = 1 + (int) (Math.random() * 7);
        net.minecraft.client.MinecraftClient.getInstance().getSoundManager().play(net.minecraft.client.sound.PositionedSoundInstance.ui(net.minecraft.sound.SoundEvent.of(net.minecraft.util.Identifier.of("atheryx", "keyboard_" + randomId)), 1.0f, 1.0f));
    }

    @Override
    public boolean mouseDragged(Click click, double offsetX, double offsetY) {
        if ((pickerOpen || pickerProgress > 0.02f) && (draggingPickerField || draggingPickerHue || draggingPickerField2 || draggingPickerHue2)) {
            float fixedMouseX = RenderUtil.convertX((float) click.x());
            float fixedMouseY = RenderUtil.convertY((float) click.y());
            updatePickerColor(fixedMouseX, fixedMouseY);
            return true;
        }

        float fixedMouseX = RenderUtil.convertX((float) click.x());
        float fixedMouseY = RenderUtil.convertY((float) click.y());
        int button = click.button();
        for (ModuleComponent component : getActiveModules()) {
            if (component.mouseDragged(fixedMouseX, fixedMouseY, button)) {
                return true;
            }
        }

        return super.mouseDragged(click, offsetX, offsetY);
    }

    @Override
    public boolean mouseReleased(Click click) {
        draggingPickerField = false;
        draggingPickerHue = false;
        draggingPickerField2 = false;
        draggingPickerHue2 = false;
        for (ModuleComponent component : getActiveModules()) {
            component.mouseReleased();
        }
        if (editingCustomTheme >= 0 && editingCustomTheme < ThemeManager.CUSTOM_THEMES.size()) {
            ThemeManager.setCustomTheme(editingCustomTheme, ThemeManager.Theme_Color);
        } else {
            ThemeManager.save();
        }
        return super.mouseReleased(click);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (closing || getAnimation() < 0.95f) {
            return true;
        }

        float fixedMouseX = RenderUtil.convertX((float) mouseX);
        float fixedMouseY = RenderUtil.convertY((float) mouseY);
        if ((selectedCategory == CAT_MISC || selectedCategory == CAT_VISUAL)
                && isInside(fixedMouseX, fixedMouseY, modulePanelFixedX() + (MODULE_PANEL_WIDTH - getModuleWidth()) / 2f, getModuleViewportY(), getModuleWidth(), getModuleViewportHeight())) {
            float next = targetScrollOffset - (float) verticalAmount * SCROLL_STEP;
            targetScrollOffset = Math.clamp(next, 0f, getMaxScroll());
            return true;
        }

        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    private float getAnimation() {
        float progress = (System.currentTimeMillis() - animationStart) / ANIMATION_DURATION;
        progress = Math.clamp(progress, 0f, 1f);
        return closing ? 1f - progress : progress;
    }

    private void updateUiAnimations() {
        long now = System.currentTimeMillis();
        if (lastUiFrameTime == 0L) {
            lastUiFrameTime = now;
            return;
        }

        float delta = Math.min(50f, now - lastUiFrameTime);
        lastUiFrameTime = now;
        pickerProgress = getPickerAnimation();
        plusMenuProgress = getPlusMenuAnimation();
        contextMenuProgress = animate(contextMenuProgress, isContextMenuOpen() ? 1f : 0f, MENU_ANIMATION_SPEED, delta);
        selectedPulse = animate(selectedPulse, 0f, PULSE_ANIMATION_SPEED, delta);

        float current = scrollOffset;
        float target = Math.clamp(targetScrollOffset, 0f, getMaxScroll());
        targetScrollOffset = target;
        scrollOffset = animate(current, target, SCROLL_ANIMATION_SPEED, delta);

        for (AnimatedChar ac : searchChars) {
            ac.animation = animate(ac.animation, 1f, 0.03f, delta);
        }

        float targetPlaceholder = (searchFocused || !searchText.isEmpty() || !searchChars.isEmpty()) ? 1f : 0f;
        placeholderAnimation = animate(placeholderAnimation, targetPlaceholder, 0.04f, delta);
    }

    private boolean handleSidebarClick(float mouseX, float mouseY, int button) {
        if (button != 0) {
            return false;
        }

        int item = getSidebarItem(mouseX, mouseY);
        if (item < 0) {
            return false;
        }

        if (item == 0 || item == 1) {
            setCategory(item == 0 ? CAT_MISC : CAT_VISUAL);
        } else if (item == 4) {
            setCategory(CAT_THEMES);
        }
        return true;
    }

    private int getSidebarItem(float mouseX, float mouseY) {
        float x = panelLeftFixedX();
        float y = panelFixedY() + SIDEBAR_HEADER_HEIGHT + 12f;
        if (!isInside(mouseX, mouseY, x, y, SIDEBAR_WIDTH, CATEGORY_ITEM_HEIGHT * 5f)) {
            return -1;
        }
        int index = (int) ((mouseY - y) / CATEGORY_ITEM_HEIGHT);
        return index < 0 || index > 4 ? -1 : index;
    }

    private void setCategory(int category) {
        if (selectedCategory == category) {
            return;
        }
        selectedCategory = category;
        scrollOffset = 0f;
        targetScrollOffset = 0f;
        closeContextMenu();
        closePicker();
        searchText = "";
        searchChars.clear();
        rebuildModules();
        net.minecraft.client.MinecraftClient.getInstance().getSoundManager().play(net.minecraft.client.sound.PositionedSoundInstance.ui(net.minecraft.sound.SoundEvent.of(net.minecraft.util.Identifier.of("atheryx", "switchcategory")), 1.0f, 1.0f));
    }

    private void renderSidebar(DrawContext context, float mouseX, float mouseY, float centerX, float centerY, float scale, float opacity) {
        float x = panelLeftFixedX();
        float y = panelFixedY();
        float renderX = scaleX(x, centerX, scale);
        float renderY = scaleY(y, centerY, scale);
        float renderWidth = SIDEBAR_WIDTH * scale;
        float renderHeight = PANEL_HEIGHT * scale;

        RenderUtil.rect(renderX, renderY, renderWidth, renderHeight, 9f * scale, withOpacity(new Color(0, 0, 0, 170), opacity));

        renderSidebarTitle(context, renderX, renderY, scale, opacity);

        float dividerY = renderY + (SIDEBAR_HEADER_HEIGHT + 4f) * scale;
        RenderUtil.rect(renderX + PANEL_PADDING * scale, dividerY, renderWidth - PANEL_PADDING * 2f * scale, 1f * scale, 0.5f * scale, withOpacity(new Color(255, 255, 255, 22), opacity));

        for (int i = 0; i < 5; i++) {
            float itemY = renderY + (SIDEBAR_HEADER_HEIGHT + 12f + i * CATEGORY_ITEM_HEIGHT) * scale;
            renderCategoryItem(context, i, renderX, itemY, renderWidth, CATEGORY_ITEM_HEIGHT * scale, mouseX, mouseY, scale, opacity);
        }
    }

    private void renderSidebarTitle(DrawContext context, float x, float y, float scale, float opacity) {
        net.minecraft.client.MinecraftClient mc = net.minecraft.client.MinecraftClient.getInstance();
        Color theme = ThemeManager.getThemeColor();
        int alpha = (int) (255 * opacity);
        int color = ((alpha << 24) & 0xFF000000) | ((theme.getRed() << 16) & 0xFF0000) | ((theme.getGreen() << 8) & 0xFF00) | (theme.getBlue() & 0xFF);
        float textWidth = mc.textRenderer.getWidth("Achrone");
        float textHeight = mc.textRenderer.fontHeight;
        float textX = x + (SIDEBAR_WIDTH * scale - textWidth) / 2f;
        float textY = y + (SIDEBAR_HEADER_HEIGHT * scale - textHeight) / 2f;
        context.drawTextWithShadow(mc.textRenderer, Text.literal("Achrone"), (int) textX, (int) textY, color);
    }

    private void renderCategoryItem(DrawContext context, int index, float x, float y, float width, float height, float mouseX, float mouseY, float scale, float opacity) {
        String label;
        int category = -1;
        if (index == 0) {
            label = "Misc";
            category = CAT_MISC;
        } else if (index == 1) {
            label = "Visual";
            category = CAT_VISUAL;
        } else if (index == 4) {
            label = "Themes";
            category = CAT_THEMES;
        } else {
            label = "";
        }

        if (label.isEmpty()) {
            return;
        }

        boolean selected = selectedCategory == category;
        boolean hovered = isInside(mouseX, mouseY, x, y, width, height);

        if (selected) {
            RenderUtil.rect(x, y, width, height, 5f * scale, withOpacity(new Color(ThemeManager.getThemeColor().getRed(), ThemeManager.getThemeColor().getGreen(), ThemeManager.getThemeColor().getBlue(), 120), opacity * opacity * opacity));
            RenderUtil.rect(x, y + 8f * scale, 2f * scale, height - 16f * scale, 1f * scale, withOpacity(ThemeManager.Theme_Color, opacity));
        } else if (hovered) {
            RenderUtil.rect(x, y, width, height, 5f * scale, withOpacity(new Color(255, 255, 255, 12), opacity * opacity * opacity));
        }

        String iconGlyph = index == 0 ? "U" : (index == 1 ? "W" : "V");
        float glyphSize = 12f * scale;
        float iconWidth = Msdf.width(Msdf.ICONS, iconGlyph, glyphSize);
        float iconHeight = Msdf.height(Msdf.ICONS, glyphSize);
        float iconX = x + 12f * scale;
        float iconY = y + (height - iconHeight) / 2f;
        RenderUtil.text(context, Msdf.ICONS, iconX, iconY, iconGlyph, glyphSize, withOpacity(ThemeManager.Theme_Color, opacity));

        Color textColor = selected ? Color.WHITE : (hovered ? new Color(235, 235, 238) : new Color(160, 160, 165));
        float textSize = 11f * scale;
        float textX = iconX + iconWidth + 8f * scale;
        float textY = y + (height - Msdf.height(textSize)) / 2f;
        RenderUtil.text(context, textX, textY, label, textSize, withOpacity(textColor, opacity));
    }

    private void renderModulePanel(DrawContext context, float mouseX, float mouseY, float centerX, float centerY, float scale, float opacity) {
        float x = modulePanelFixedX();
        float y = panelFixedY();
        float renderX = scaleX(x, centerX, scale);
        float renderY = scaleY(y, centerY, scale);
        float renderWidth = MODULE_PANEL_WIDTH * scale;
        float renderHeight = PANEL_HEIGHT * scale;

        RenderUtil.rect(renderX, renderY, renderWidth, renderHeight, 9f * scale, withOpacity(new Color(0, 0, 0, 170), opacity));

        String categoryName = selectedCategory == CAT_MISC ? "Misc" : (selectedCategory == CAT_VISUAL ? "Visual" : "Themes");
        String catIcon = selectedCategory == CAT_MISC ? "U" : (selectedCategory == CAT_VISUAL ? "W" : "V");
        float titleSize = 11f * scale;
        float iconSize = 12f * scale;
        float iconHeight = Msdf.height(Msdf.ICONS, iconSize);
        float titleHeight = Msdf.height(titleSize);
        float headerCenter = renderY + MODULE_HEADER_HEIGHT * scale / 2f;
        RenderUtil.text(context, Msdf.ICONS, renderX + 12f * scale, headerCenter - iconHeight / 2f, catIcon, iconSize, withOpacity(ThemeManager.Theme_Color, opacity));
        RenderUtil.text(context, renderX + 12f * scale + iconSize + 6f * scale, headerCenter - titleHeight / 2f, categoryName, titleSize, withOpacity(new Color(235, 235, 238), opacity));

        renderSearchBar(context, renderX, renderY + MODULE_HEADER_HEIGHT * scale, renderWidth, scale, opacity);

        renderTelegramBox(context, renderX, renderY + MODULE_HEADER_HEIGHT * scale, renderWidth, scale, opacity);

        if (selectedCategory == CAT_THEMES) {
            renderThemeMenu(context, renderX, renderY, renderWidth, renderHeight, mouseX, mouseY, scale, opacity);
        } else {
            renderModuleList(context, renderX, renderY, renderWidth, renderHeight, mouseX, mouseY, scale, opacity);
        }
    }

    private void renderSearchBar(DrawContext context, float x, float y, float width, float scale, float opacity) {
        float searchWidth = 140f;
        float headerWidth = 24f;
        float searchHeight = SEARCH_HEIGHT;
        float renderX = x + PANEL_PADDING * scale;
        float renderY = y + 6f * scale;

        float rectOpacity = opacity * opacity * opacity;
        RenderUtil.hudBlur(renderX, renderY, searchWidth * scale, searchHeight * scale, 6f * scale, 25f, opacity, new Color(18, 18, 22, 140));

        float iconSize = 9.8f * scale;
        float iconHeight = Msdf.height(Msdf.WTMICO, iconSize);
        float iconWidth = Msdf.width(Msdf.WTMICO, "V", iconSize);
        float iconX = renderX + (headerWidth * scale - iconWidth) / 2f;
        float iconY = renderY + (searchHeight * scale - iconHeight) / 2f;
        RenderUtil.text(context, Msdf.WTMICO, iconX, iconY, "V", iconSize, withOpacity(ThemeManager.Theme_Color, opacity));

        float inputXOffset = headerWidth * scale + 8f * scale;

        Scissor.push(renderX + headerWidth * scale, renderY, searchWidth * scale - headerWidth * scale, searchHeight * scale);

        if (placeholderAnimation < 0.99f) {
            float placeholderOpacity = opacity * (1f - placeholderAnimation);
            RenderUtil.text(context, renderX + inputXOffset, renderY + (searchHeight * scale - Msdf.height(9f * scale)) / 2f, "Search modules...", 9f * scale, withOpacity(new Color(150, 150, 150), Math.max(0f, placeholderOpacity)));
        }

        float textX = renderX + inputXOffset;
        for (AnimatedChar ac : searchChars) {
            float charOpacity = opacity * Easings.OutCubic(ac.animation);
            float charY = renderY + (searchHeight * scale - Msdf.height(9f * scale)) / 2f + (1f - Easings.OutCubic(ac.animation)) * 5f * scale;
            RenderUtil.text(context, textX, charY, ac.character, 9f * scale, withOpacity(Color.WHITE, charOpacity));
            textX += Msdf.width(ac.character, 9f * scale) * ac.animation;
        }

        if (searchFocused) {
            float cursorAnim = (float) (Math.sin(System.currentTimeMillis() / 250.0) * 0.5 + 0.5);
            float cursorHeight = searchHeight * scale - 12f * scale;
            float animatedHeight = cursorHeight * cursorAnim;
            float cursorY = renderY + 6f * scale + (cursorHeight - animatedHeight) / 2f;
            float cursorOpacity = opacity * (0.2f + 0.8f * cursorAnim);
            float cX = (searchText.isEmpty() && searchChars.isEmpty()) ? (renderX + inputXOffset) : (textX + 2f * scale);
            RenderUtil.rect(cX, cursorY, 1f * scale, animatedHeight, 0.5f * scale, withOpacity(Color.WHITE, cursorOpacity));
        }

        Scissor.pop();
    }

    private void renderTelegramBox(DrawContext context, float x, float y, float width, float scale, float opacity) {
        float teleWidth = TELEGRAM_WIDTH * scale;
        float teleHeight = SEARCH_HEIGHT * scale;
        float teleX = x + width - PANEL_PADDING * scale - teleWidth;
        float teleY = y + 6f * scale;

        RenderUtil.rect(teleX, teleY, teleWidth, teleHeight, 6f * scale, withOpacity(new Color(0, 0, 0, 170), opacity * opacity * opacity));

        float textSize = 6f * scale;
        float textY = teleY + (teleHeight - Msdf.height(textSize)) / 2f;
        RenderUtil.text(context, teleX + 8f * scale, textY, "t.me/AchroneVisuals", textSize, withOpacity(new Color(190, 190, 197), opacity));
    }

    private float panelLeftFixedX() {
        return (RenderUtil.getFixedScaledWidth() - (SIDEBAR_WIDTH + PANEL_GAP + MODULE_PANEL_WIDTH)) / 2f;
    }

    private float modulePanelFixedX() {
        return panelLeftFixedX() + SIDEBAR_WIDTH + PANEL_GAP;
    }

    private float panelFixedY() {
        return (RenderUtil.getFixedScaledHeight() - PANEL_HEIGHT) / 2f;
    }

    private float getModuleWidth() {
        return MODULE_PANEL_WIDTH - PANEL_PADDING * 2f;
    }

    private float getModuleViewportY() {
        return panelFixedY() + MODULE_HEADER_HEIGHT + SEARCH_HEIGHT + 14f;
    }

    private float getModuleViewportHeight() {
        return PANEL_HEIGHT - MODULE_HEADER_HEIGHT - SEARCH_HEIGHT - 20f;
    }

    private void renderModuleList(DrawContext context, float x, float y, float width, float height, float mouseX, float mouseY, float scale, float opacity) {
        List<ModuleComponent> components = getActiveModules();
        float viewportWidth = getModuleWidth() * scale;
        float viewportX = x + (width - viewportWidth) / 2f;
        float viewportY = y + (MODULE_HEADER_HEIGHT + SEARCH_HEIGHT + 14f) * scale;
        float viewportHeight = (PANEL_HEIGHT - MODULE_HEADER_HEIGHT - SEARCH_HEIGHT - 20f) * scale;

        Scissor.push(viewportX, viewportY, viewportWidth, viewportHeight);
        if (components.isEmpty()) {
            float moduleY = viewportY - scrollOffset * scale;
            RenderUtil.text(context, viewportX + viewportWidth / 2f, moduleY + 8.2f * scale, "Empty", 10.2f * scale, withOpacity(new Color(105, 105, 111), opacity), "center");
            Scissor.pop();
            return;
        }

        float moduleOpacity = opacity * opacity;
        float moduleWidth = (viewportWidth - MODULE_ROW_GAP * scale) / 2f;
        float leftX = viewportX;
        float rightX = viewportX + moduleWidth + MODULE_ROW_GAP * scale;

        float leftY = viewportY - scrollOffset * scale;
        float rightY = viewportY - scrollOffset * scale;

        for (int i = 0; i < components.size(); i++) {
            ModuleComponent component = components.get(i);
            boolean left = i % 2 == 0;
            float componentX = left ? leftX : rightX;
            float componentY = left ? leftY : rightY;
            float componentHeight = component.getHeight() * scale;
            if (isInside(mouseX, mouseY, componentX, componentY, moduleWidth, componentHeight) &&
                isInside(mouseX, mouseY, viewportX, viewportY, viewportWidth, viewportHeight)) {
                hoveredComponent = component;
            }
            component.render(context, componentX, componentY, moduleWidth, mouseX, mouseY, scale, moduleOpacity);
            if (left) {
                leftY += componentHeight + MODULE_ROW_GAP * scale;
            } else {
                rightY += componentHeight + MODULE_ROW_GAP * scale;
            }
        }
        Scissor.pop();

        renderScrollbar(x, y, scale, opacity);
    }

    private void renderScrollbar(float x, float y, float scale, float opacity) {
        float maxScroll = getMaxScroll();
        if (maxScroll <= 0f) {
            return;
        }

        float trackHeight = (PANEL_HEIGHT - MODULE_HEADER_HEIGHT - SEARCH_HEIGHT - 20f) * scale;
        float trackX = x + MODULE_PANEL_WIDTH * scale - 5f * scale;
        float trackY = y + (MODULE_HEADER_HEIGHT + SEARCH_HEIGHT + 14f) * scale;
        float contentHeight = getContentHeight();
        float thumbHeight = Math.max(24f * scale, trackHeight * ((PANEL_HEIGHT - MODULE_HEADER_HEIGHT - SEARCH_HEIGHT - 20f) / contentHeight));
        float progress = scrollOffset / maxScroll;
        float thumbY = trackY + (trackHeight - thumbHeight) * Math.clamp(progress, 0f, 1f);

        RenderUtil.rect(trackX, trackY, 2f * scale, trackHeight, 1f * scale, withOpacity(new Color(27, 27, 29), opacity));
        RenderUtil.rect(trackX, thumbY, 2f * scale, thumbHeight, 1f * scale, withOpacity(ThemeManager.Theme_Color, opacity));
    }

    private float getMaxScroll() {
        float viewportHeight = PANEL_HEIGHT - MODULE_HEADER_HEIGHT - SEARCH_HEIGHT - 20f;
        return Math.max(0f, getContentHeight() - viewportHeight);
    }

    private float getContentHeight() {
        List<ModuleComponent> components = getActiveModules();
        if (components.isEmpty()) {
            return MODULE_HEIGHT;
        }

        float leftHeight = 0f;
        float rightHeight = 0f;
        int leftCount = 0;
        int rightCount = 0;
        for (int i = 0; i < components.size(); i++) {
            float moduleHeight = components.get(i).getHeight();
            if (i % 2 == 0) {
                leftHeight += moduleHeight;
                leftCount++;
            } else {
                rightHeight += moduleHeight;
                rightCount++;
            }
        }
        float leftTotal = leftHeight + Math.max(0, leftCount - 1) * MODULE_ROW_GAP;
        float rightTotal = rightHeight + Math.max(0, rightCount - 1) * MODULE_ROW_GAP;
        return Math.max(leftTotal, rightTotal);
    }

    private List<ModuleComponent> getActiveModules() {
        return selectedCategory == CAT_MISC ? miscModules : visualModules;
    }

    private void rebuildModules() {
        miscModules.clear();
        visualModules.clear();

        for (Function function : FunctionManager.getFunctions()) {
            if (function instanceof Ambience || function instanceof AntiAFK || function instanceof Bots) {
                if (searchText.isEmpty() || function.getName().toLowerCase().contains(searchText.toLowerCase())) {
                    miscModules.add(new ModuleComponent(function));
                }
            } else if (function.getCategory().name().equals("RENDER") || function.getCategory().name().equals("MISC")) {
                if (searchText.isEmpty() || function.getName().toLowerCase().contains(searchText.toLowerCase())) {
                    visualModules.add(new ModuleComponent(function));
                }
            }
        }
    }

    private int getThemeItemCount() {
        return 2 + ThemeManager.CUSTOM_THEMES.size();
    }

    private float getThemeItemsStartY() {
        return panelFixedY() + MODULE_HEADER_HEIGHT + SEARCH_HEIGHT + 14f + THEME_GRADIENT_HEIGHT + 10f;
    }

    private void renderThemeMenu(DrawContext context, float x, float y, float width, float height, float mouseX, float mouseY, float scale, float opacity) {
        float contentX = x + PANEL_PADDING * scale;
        float contentW = width - PANEL_PADDING * 2f * scale;
        float contentTop = y + (MODULE_HEADER_HEIGHT + SEARCH_HEIGHT + 14f) * scale;

        renderGradientBar(context, contentX, contentTop, contentW, THEME_GRADIENT_HEIGHT * scale, scale, opacity);

        float itemY = contentTop + THEME_GRADIENT_HEIGHT * scale + 10f * scale;
        renderThemeItem(context, contentX, itemY, contentW, THEME_ITEM_HEIGHT * scale, "Красная", ThemeManager.PASTEL_RED, mouseX, mouseY, scale, opacity);
        itemY += THEME_ITEM_HEIGHT * scale;

        renderThemeItem(context, contentX, itemY, contentW, THEME_ITEM_HEIGHT * scale, "Синяя", ThemeManager.PASTEL_BLUE, mouseX, mouseY, scale, opacity);
        itemY += THEME_ITEM_HEIGHT * scale;

        for (int i = 0; i < ThemeManager.CUSTOM_THEMES.size(); i++) {
            renderThemeItem(context, contentX, itemY, contentW, THEME_ITEM_HEIGHT * scale, "Кастом " + (i + 1), ThemeManager.CUSTOM_THEMES.get(i), mouseX, mouseY, scale, opacity);
            itemY += THEME_ITEM_HEIGHT * scale;
        }

        boolean plusHovered = isInside(mouseX, mouseY, contentX, itemY, contentW, THEME_ITEM_HEIGHT * scale);
        Color plusColor = plusHovered ? withOpacity(ThemeManager.Theme_Color, opacity) : withOpacity(new Color(72, 72, 76), opacity);
        RenderUtil.text(context, contentX + contentW / 2f, itemY + (THEME_ITEM_HEIGHT * scale - Msdf.height(12f * scale)) / 2f, "+", 12f * scale, plusColor, "center");

        if (plusMenuOpen || plusMenuProgress > 0.02f) {
            float plusMenuEase = Easings.OutCubic(plusMenuProgress);
            float plusMenuOpacity = opacity * plusMenuEase;
            renderPlusMenu(context, contentX, itemY - 40f * scale, scale, plusMenuOpacity);
        }

        if (pickerOpen || pickerProgress > 0.02f) {
            float pickerEase = Easings.OutCubic(pickerProgress);
            float pickerScale = scale * (0.94f + pickerEase * 0.06f);
            float pickerOpacity = opacity * pickerEase;
            float pickerX = scaleX(getPickerX(), RenderUtil.getFixedScaledWidth() / 2f, scale);
            float pickerY = scaleY(getPickerY(), RenderUtil.getFixedScaledHeight() / 2f, scale);
            renderColorPicker(context, pickerX, pickerY, pickerScale, pickerOpacity);
        }

        if (isContextMenuOpen() || contextMenuProgress > 0.02f) {
            renderThemeContextMenu(context, scaleX(contextMenuX, RenderUtil.getFixedScaledWidth() / 2f, scale), scaleY(contextMenuY, RenderUtil.getFixedScaledHeight() / 2f, scale), scale, opacity * contextMenuProgress);
        }
    }

    private void renderGradientBar(DrawContext context, float x, float y, float width, float height, float scale, float opacity) {
        Color c1 = ThemeManager.Theme_Color2;
        Color c2 = mix(ThemeManager.Theme_Color, c1, 0.5f);
        Color c3 = ThemeManager.Theme_Color;
        RenderUtil.rect(x, y, width, height, 7f * scale, withOpacity(new Color(0, 0, 0, 150), opacity));
        RenderUtil.rect(x, y, width, height, 7f * scale,
                withOpacity(c1, opacity), withOpacity(c2, opacity), withOpacity(c3, opacity),
                withOpacity(c1, opacity), withOpacity(c2, opacity), withOpacity(c3, opacity),
                withOpacity(c1, opacity), withOpacity(c2, opacity), withOpacity(c3, opacity));
        RenderUtil.outline(x, y, width, height, 7f * scale, 0.9f * scale, withOpacity(new Color(255, 255, 255, 40), opacity));
    }

    private void renderThemeItem(DrawContext context, float x, float y, float width, float height, String name, Color color, float mouseX, float mouseY, float scale, float opacity) {
        boolean hovered = isInside(mouseX, mouseY, x, y, width, height);
        if (hovered) {
            RenderUtil.rect(x, y, width, height, 4f * scale, withOpacity(new Color(255, 255, 255, 10), opacity * opacity * opacity));
        }
        float dotSize = 8f * scale;
        RenderUtil.rect(x + 8f * scale, y + (height - dotSize) / 2f, dotSize, dotSize, dotSize / 2f, withOpacity(color, opacity));

        if (sameColor(color, ThemeManager.Theme_Color)) {
            RenderUtil.outline(x + 8f * scale - 1.5f * scale, y + (height - dotSize) / 2f - 1.5f * scale, dotSize + 3f * scale, dotSize + 3f * scale, (dotSize + 3f * scale) / 2f, 1f * scale, withOpacity(Color.WHITE, opacity));
        }

        RenderUtil.text(context, x + 22f * scale, y + (height - Msdf.height(8f * scale)) / 2f, name, 8f * scale, withOpacity(Color.WHITE, opacity));
    }

    private boolean handleThemeMenuClick(float mouseX, float mouseY, int button) {
        if (selectedCategory != CAT_THEMES) {
            return false;
        }

        if (handleContextMenuClick(mouseX, mouseY, button)) {
            return true;
        }

        if (button == 0 && (pickerOpen || pickerProgress > 0.02f) && handlePickerClick(mouseX, mouseY)) {
            return true;
        }

        float contentX = modulePanelFixedX() + PANEL_PADDING;
        float contentW = MODULE_PANEL_WIDTH - PANEL_PADDING * 2f;
        float itemY = getThemeItemsStartY();

        if (plusMenuOpen) {
            float pmWidth = 70f;
            float pmHeight = 36f;
            float pmX = contentX + contentW - pmWidth;
            float pmY = itemY + getThemeItemCount() * THEME_ITEM_HEIGHT - pmHeight;
            if (isInside(mouseX, mouseY, pmX, pmY, pmWidth, pmHeight)) {
                if (button == 0) {
                    ThemeManager.twoColors = mouseY >= pmY + 18f;
                    setPlusMenuOpen(false);
                    openCustomThemePicker();
                }
                return true;
            } else if (button == 0) {
                setPlusMenuOpen(false);
            }
        }

        if (tryHandleThemeItem(mouseX, mouseY, button, contentX, itemY, contentW, THEME_ITEM_HEIGHT, -1, ThemeManager.PASTEL_RED)) return true;
        itemY += THEME_ITEM_HEIGHT;

        if (tryHandleThemeItem(mouseX, mouseY, button, contentX, itemY, contentW, THEME_ITEM_HEIGHT, -2, ThemeManager.PASTEL_BLUE)) return true;
        itemY += THEME_ITEM_HEIGHT;

        for (int i = 0; i < ThemeManager.CUSTOM_THEMES.size(); i++) {
            if (tryHandleThemeItem(mouseX, mouseY, button, contentX, itemY, contentW, THEME_ITEM_HEIGHT, i, ThemeManager.CUSTOM_THEMES.get(i))) return true;
            itemY += THEME_ITEM_HEIGHT;
        }

        if (button == 0 && isInside(mouseX, mouseY, contentX, itemY, contentW, THEME_ITEM_HEIGHT)) {
            closeContextMenu();
            setPlusMenuOpen(!plusMenuOpen);
            return true;
        }

        return false;
    }

    private boolean tryHandleThemeItem(float mouseX, float mouseY, int button, float x, float y, float width, float height, int index, Color color) {
        if (!isInside(mouseX, mouseY, x, y, width, height)) {
            return false;
        }

        if (button == 1) {
            openContextMenu(mouseX, mouseY, index);
        } else {
            trySelectTheme(color);
        }
        return true;
    }

    private boolean trySelectTheme(Color color) {
        closeContextMenu();
        editingCustomTheme = -1;
        ThemeManager.setThemeColor(color);
        updatePickerFromColor(color);
        closePicker();
        draggingPickerField = false;
        draggingPickerHue = false;
        draggingPickerField2 = false;
        draggingPickerHue2 = false;
        selectedPulse = 1f;
        return true;
    }

    private void openCustomThemePicker() {
        updatePickerFromColor(ThemeManager.Theme_Color);
        editingCustomTheme = ThemeManager.addCustomTheme(ThemeManager.Theme_Color);
        closeContextMenu();
        openPicker();
        selectedPulse = 1f;
    }

    private void renderColorPicker(DrawContext context, float x, float y, float scale, float opacity) {
        if (ThemeManager.twoColors) {
            float picker2X = x + PICKER_WIDTH * scale + 8f * scale;
            renderSingleColorPicker(context, x, y, scale, opacity, pickerHue, pickerSaturation, pickerBrightness);
            renderSingleColorPicker(context, picker2X, y, scale, opacity, pickerHue2, pickerSaturation2, pickerBrightness2);
        } else {
            renderSingleColorPicker(context, x, y, scale, opacity, pickerHue, pickerSaturation, pickerBrightness);
        }
    }

    private void renderSingleColorPicker(DrawContext context, float x, float y, float scale, float opacity, float hue, float saturation, float brightness) {
        float fieldX = x + 8f * scale;
        float fieldY = y + 8f * scale;
        float fieldSize = PICKER_FIELD_SIZE * scale;
        float radius = 6f * scale;

        renderSaturationBrightnessField(fieldX, fieldY, fieldSize, fieldSize, radius, hue, opacity);
        RenderUtil.outline(fieldX, fieldY, fieldSize, fieldSize, radius, 0.9f * scale,
                withOpacity(new Color(255, 255, 255), opacity * 0.25f));

        float markerX = fieldX + saturation * fieldSize;
        float markerY = fieldY + (1f - brightness) * fieldSize;
        RenderUtil.outline(markerX - 2.5f * scale, markerY - 2.5f * scale, 5f * scale, 5f * scale, 2.5f * scale, 1f * scale, withOpacity(Color.WHITE, opacity));

        float hueX = fieldX + fieldSize + 8f * scale;
        renderHueSlider(hueX, fieldY, PICKER_HUE_WIDTH * scale, fieldSize, opacity);
        RenderUtil.outline(hueX, fieldY, PICKER_HUE_WIDTH * scale, fieldSize, (PICKER_HUE_WIDTH * scale) / 2f, 0.9f * scale,
                withOpacity(new Color(255, 255, 255), opacity * 0.25f));

        float hueMarkerY = fieldY + hue * fieldSize;
        RenderUtil.outline(hueX - 2f * scale, hueMarkerY - 1.5f * scale, (PICKER_HUE_WIDTH + 4f) * scale, 3f * scale, 1.5f * scale, 1f * scale, withOpacity(Color.WHITE, opacity));
    }

    private void renderSaturationBrightnessField(float x, float y, float width, float height, float radius, float hue, float opacity) {
        Color topLeft = Color.WHITE;
        Color topMid = mix(Color.WHITE, Color.getHSBColor(hue, 1f, 1f), 0.5f);
        Color topRight = Color.getHSBColor(hue, 1f, 1f);
        Color midLeft = new Color(128, 128, 128);
        Color midMid = Color.getHSBColor(hue, 0.5f, 0.5f);
        Color midRight = Color.getHSBColor(hue, 1f, 0.5f);
        Color black = Color.BLACK;

        RenderUtil.rect(x, y, width, height, radius,
                withOpacity(topLeft, opacity),
                withOpacity(topMid, opacity),
                withOpacity(topRight, opacity),
                withOpacity(midLeft, opacity),
                withOpacity(midMid, opacity),
                withOpacity(midRight, opacity),
                withOpacity(black, opacity),
                withOpacity(black, opacity),
                withOpacity(black, opacity));
    }

    private void renderHueSlider(float x, float y, float width, float height, float opacity) {
        if (height <= 0f || width <= 0f) return;

        int dots = 80;
        float travel = Math.max(0f, height - width);
        float step = dots <= 1 ? 0f : travel / (dots - 1);
        float radius = width / 2f;

        for (int i = 0; i < dots; i++) {
            float progress = i / (float) (dots - 1);
            RenderUtil.rect(x, y + i * step, width, width, radius,
                    withOpacity(Color.getHSBColor(progress, 1f, 1f), opacity));
        }
    }

    private void renderPlusMenu(DrawContext context, float x, float y, float scale, float opacity) {
        float menuWidth = 70f * scale;
        float menuHeight = 36f * scale;
        RenderUtil.rect(x, y, menuWidth, menuHeight, 5f * scale, withOpacity(new Color(0, 0, 0, 170), opacity));
        RenderUtil.text(context, x + 7f * scale, y + 5.2f * scale, "Один цвет", 7.5f * scale, withOpacity(new Color(226, 226, 230), opacity));
        RenderUtil.text(context, x + 7f * scale, y + (18f + 5.2f) * scale, "Два цвета", 7.5f * scale, withOpacity(new Color(226, 226, 230), opacity));
    }

    private void renderThemeContextMenu(DrawContext context, float x, float y, float scale, float opacity) {
        boolean custom = contextThemeIndex >= 0;
        float menuHeight = THEME_MENU_ROW_HEIGHT * 2f * scale;
        RenderUtil.rect(x, y, THEME_MENU_WIDTH * scale, menuHeight, 5f * scale, withOpacity(new Color(0, 0, 0, 170), opacity));
        RenderUtil.text(context, x + 7f * scale, y + 5.2f * scale, "Change", 7.5f * scale, withOpacity(new Color(226, 226, 230), opacity));
        Color deleteColor = custom ? new Color(245, 150, 150) : new Color(96, 96, 102);
        RenderUtil.text(context, x + 7f * scale, y + (THEME_MENU_ROW_HEIGHT + 5.2f) * scale, "Delete", 7.5f * scale, withOpacity(deleteColor, opacity));
    }

    private boolean handlePickerClick(float mouseX, float mouseY) {
        float pickerX = getPickerX();
        float pickerY = getPickerY();
        float totalPickerWidth = ThemeManager.twoColors ? PICKER_WIDTH * 2 + 8f : PICKER_WIDTH;
        if (!isInside(mouseX, mouseY, pickerX, pickerY, totalPickerWidth, PICKER_HEIGHT)) {
            return false;
        }

        float fieldX = pickerX + 8f;
        float fieldY = pickerY + 8f;
        float hueX = fieldX + PICKER_FIELD_SIZE + 8f;
        draggingPickerField = isInside(mouseX, mouseY, fieldX, fieldY, PICKER_FIELD_SIZE, PICKER_FIELD_SIZE);
        draggingPickerHue = isInside(mouseX, mouseY, hueX, fieldY, PICKER_HUE_WIDTH, PICKER_FIELD_SIZE);
        if (ThemeManager.twoColors) {
            float picker2X = pickerX + PICKER_WIDTH + 8f;
            float field2X = picker2X + 8f;
            float hue2X = field2X + PICKER_FIELD_SIZE + 8f;
            draggingPickerField2 = isInside(mouseX, mouseY, field2X, fieldY, PICKER_FIELD_SIZE, PICKER_FIELD_SIZE);
            draggingPickerHue2 = isInside(mouseX, mouseY, hue2X, fieldY, PICKER_HUE_WIDTH, PICKER_FIELD_SIZE);
        }
        updatePickerColor(mouseX, mouseY);
        return true;
    }

    private void updatePickerColor(float mouseX, float mouseY) {
        float pickerX = getPickerX();
        float pickerY = getPickerY();
        float fieldX = pickerX + 8f;
        float fieldY = pickerY + 8f;
        float hueX = fieldX + PICKER_FIELD_SIZE + 8f;

        if (draggingPickerField) {
            pickerSaturation = Math.clamp((mouseX - fieldX) / PICKER_FIELD_SIZE, 0f, 1f);
            pickerBrightness = 1f - Math.clamp((mouseY - fieldY) / PICKER_FIELD_SIZE, 0f, 1f);
        } else if (draggingPickerHue || isInside(mouseX, mouseY, hueX, fieldY, PICKER_HUE_WIDTH, PICKER_FIELD_SIZE)) {
            pickerHue = Math.clamp((mouseY - fieldY) / PICKER_FIELD_SIZE, 0f, 1f);
        }

        if (ThemeManager.twoColors) {
            float picker2X = pickerX + PICKER_WIDTH + 8f;
            float field2X = picker2X + 8f;
            float hue2X = field2X + PICKER_FIELD_SIZE + 8f;
            if (draggingPickerField2) {
                pickerSaturation2 = Math.clamp((mouseX - field2X) / PICKER_FIELD_SIZE, 0f, 1f);
                pickerBrightness2 = 1f - Math.clamp((mouseY - fieldY) / PICKER_FIELD_SIZE, 0f, 1f);
            } else if (draggingPickerHue2 || isInside(mouseX, mouseY, hue2X, fieldY, PICKER_HUE_WIDTH, PICKER_FIELD_SIZE)) {
                pickerHue2 = Math.clamp((mouseY - fieldY) / PICKER_FIELD_SIZE, 0f, 1f);
            }
        }

        Color picked = Color.getHSBColor(pickerHue, pickerSaturation, pickerBrightness);
        ThemeManager.Theme_Color = picked;
        if (ThemeManager.twoColors) {
            ThemeManager.Theme_Color2 = Color.getHSBColor(pickerHue2, pickerSaturation2, pickerBrightness2);
        }
        if (editingCustomTheme >= 0 && editingCustomTheme < ThemeManager.CUSTOM_THEMES.size()) {
            ThemeManager.CUSTOM_THEMES.set(editingCustomTheme, picked);
        }
    }

    private void updatePickerFromColor(Color color) {
        float[] hsb = Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), null);
        pickerHue = hsb[0];
        pickerSaturation = hsb[1];
        pickerBrightness = hsb[2];
        if (ThemeManager.twoColors) {
            float[] hsb2 = Color.RGBtoHSB(ThemeManager.Theme_Color2.getRed(), ThemeManager.Theme_Color2.getGreen(), ThemeManager.Theme_Color2.getBlue(), null);
            pickerHue2 = hsb2[0];
            pickerSaturation2 = hsb2[1];
            pickerBrightness2 = hsb2[2];
        }
    }

    private float getPickerX() {
        return modulePanelFixedX() + MODULE_PANEL_WIDTH + 10f;
    }

    private float getPickerY() {
        return panelFixedY();
    }

    private boolean handleContextMenuClick(float mouseX, float mouseY, int button) {
        if (!isContextMenuOpen()) {
            return false;
        }

        boolean custom = contextThemeIndex >= 0;
        float menuHeight = THEME_MENU_ROW_HEIGHT * 2f;
        if (!isInside(mouseX, mouseY, contextMenuX, contextMenuY, THEME_MENU_WIDTH, menuHeight)) {
            if (button == 0) {
                closeContextMenu();
            }
            return false;
        }

        if (button != 0) {
            return true;
        }

        int row = (int) ((mouseY - contextMenuY) / THEME_MENU_ROW_HEIGHT);
        if (row == 0) {
            editContextTheme();
        } else {
            if (custom) {
                ThemeManager.removeCustomTheme(contextThemeIndex);
                editingCustomTheme = -1;
                closePicker();
                selectedPulse = 1f;
            }
            closeContextMenu();
        }
        return true;
    }

    private void editContextTheme() {
        Color color = getContextThemeColor();
        if (color == null) {
            closeContextMenu();
            return;
        }

        updatePickerFromColor(color);
        ThemeManager.setThemeColor(color);
        editingCustomTheme = contextThemeIndex >= 0 ? contextThemeIndex : ThemeManager.addCustomTheme(color);
        openPicker();
        selectedPulse = 1f;
        closeContextMenu();
    }

    private Color getContextThemeColor() {
        if (contextThemeIndex == -1) {
            return ThemeManager.PASTEL_RED;
        }
        if (contextThemeIndex == -2) {
            return ThemeManager.PASTEL_BLUE;
        }
        if (contextThemeIndex >= 0 && contextThemeIndex < ThemeManager.CUSTOM_THEMES.size()) {
            return ThemeManager.CUSTOM_THEMES.get(contextThemeIndex);
        }
        return null;
    }

    private void openContextMenu(float mouseX, float mouseY, int index) {
        contextThemeIndex = index;
        contextMenuX = mouseX;
        contextMenuY = mouseY;
        closePicker();
        draggingPickerField = false;
        draggingPickerHue = false;
        draggingPickerField2 = false;
        draggingPickerHue2 = false;
    }

    private void closeContextMenu() {
        contextThemeIndex = Integer.MIN_VALUE;
    }

    private boolean isContextMenuOpen() {
        return contextThemeIndex != Integer.MIN_VALUE;
    }

    private void openPicker() {
        setPickerOpen(true);
    }

    private void closePicker() {
        setPickerOpen(false);
    }

    private void setPickerOpen(boolean open) {
        if (pickerOpen == open) {
            return;
        }

        pickerProgress = getPickerAnimation();
        plusMenuProgress = getPlusMenuAnimation();
        pickerOpen = open;
        long offset = (long) ((open ? pickerProgress : 1f - pickerProgress) * PICKER_ANIMATION_DURATION);
        pickerAnimationStart = System.currentTimeMillis() - offset;
    }

    private float getPickerAnimation() {
        float progress = (System.currentTimeMillis() - pickerAnimationStart) / PICKER_ANIMATION_DURATION;
        progress = Math.clamp(progress, 0f, 1f);
        return pickerOpen ? progress : 1f - progress;
    }

    private void setPlusMenuOpen(boolean open) {
        if (plusMenuOpen == open) {
            return;
        }

        plusMenuProgress = getPlusMenuAnimation();
        plusMenuOpen = open;
        long offset = (long) ((open ? plusMenuProgress : 1f - plusMenuProgress) * ANIMATION_DURATION);
        plusMenuAnimationStart = System.currentTimeMillis() - offset;
    }

    private float getPlusMenuAnimation() {
        if (plusMenuAnimationStart == 0) return 0f;
        float progress = (System.currentTimeMillis() - plusMenuAnimationStart) / ANIMATION_DURATION;
        progress = Math.clamp(progress, 0f, 1f);
        return plusMenuOpen ? progress : 1f - progress;
    }

    private float animate(float current, float target, float speed, float delta) {
        float factor = 1f - (float) Math.exp(-speed * delta);
        float next = current + (target - current) * Math.clamp(factor, 0f, 1f);
        return Math.abs(next - target) < 0.001f ? target : next;
    }

    private Color mix(Color first, Color second, float progress) {
        float clamped = Math.clamp(progress, 0f, 1f);
        int red = (int) (first.getRed() + (second.getRed() - first.getRed()) * clamped);
        int green = (int) (first.getGreen() + (second.getGreen() - first.getGreen()) * clamped);
        int blue = (int) (first.getBlue() + (second.getBlue() - first.getBlue()) * clamped);
        int alpha = (int) (first.getAlpha() + (second.getAlpha() - first.getAlpha()) * clamped);
        return new Color(red, green, blue, alpha);
    }

    private boolean sameColor(Color first, Color second) {
        return first.getRed() == second.getRed() && first.getGreen() == second.getGreen() && first.getBlue() == second.getBlue();
    }

    private boolean isInside(float mouseX, float mouseY, float x, float y, float width, float height) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

    private float scaleX(float x, float centerX, float scale) {
        return centerX + (x - centerX) * scale;
    }

    private float scaleY(float y, float centerY, float scale) {
        return centerY + (y - centerY) * scale;
    }

    private Color withOpacity(Color color, float opacity) {
        int alpha = (int) (color.getAlpha() * Math.clamp(opacity, 0f, 1f));
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
    }

    private void renderTooltipAnim(DrawContext context, float mouseX, float mouseY, float scale, float opacity) {
        long now = System.currentTimeMillis();
        float animProgress = 1f;
        if (tooltipChangeStart > 0) {
            animProgress = Math.min(1f, (now - tooltipChangeStart) / 400f);
        }

        if (animProgress >= 1f && displayedTooltip.isEmpty()) {
            return;
        }

        float inAnim = Easings.OutBack(animProgress);
        float outAnim = Easings.OutCubic(animProgress);

        float padding = 5f * scale;
        float textSize = 8.5f * scale;
        float height = Msdf.height(textSize) + padding * 2f;

        float oldWidth = prevTooltip.isEmpty() ? 0 : Msdf.width(prevTooltip, textSize) + padding * 2f;
        float newWidth = displayedTooltip.isEmpty() ? 0 : Msdf.width(displayedTooltip, textSize) + padding * 2f;
        float currentMaxWidth = Math.max(oldWidth, newWidth);

        float targetX = mouseX + 12f * scale;
        float targetY = mouseY + 12f * scale;

        Scissor.push(targetX - 20f * scale, targetY - 20f * scale, currentMaxWidth + 40f * scale, height + 20f * scale);

        if (!prevTooltip.isEmpty() && animProgress < 1f) {
            float outYOffset = (height + 15f * scale) * outAnim;
            float outAlpha = opacity * (1f - animProgress);
            if (outAlpha > 0.02f) {
                drawTooltipBox(context, prevTooltip, targetX, targetY + outYOffset, oldWidth, height, textSize, padding, scale, outAlpha);
            }
        }

        if (!displayedTooltip.isEmpty()) {
            float inYOffset = (height + 15f * scale) * (1f - inAnim);
            float inAlpha = opacity * Math.min(1f, animProgress * 2f);
            if (inAlpha > 0.02f) {
                drawTooltipBox(context, displayedTooltip, targetX, targetY + inYOffset, newWidth, height, textSize, padding, scale, inAlpha);
            }
        }

        Scissor.pop();
    }

    private void drawTooltipBox(DrawContext context, String text, float x, float y, float width, float height, float textSize, float padding, float scale, float opacity) {
        RenderUtil.rect(x, y, width, height, 4f * scale, withOpacity(new Color(0, 0, 0, 170), opacity));
        RenderUtil.rect(x, y, width, height, 4f * scale, withOpacity(new Color(255, 255, 255, 15), opacity));
        RenderUtil.text(context, x + padding, y + padding + 1f * scale, text, textSize, withOpacity(new Color(230, 230, 235), opacity));
    }
}