package ez.minar.system.features.render;

import ez.minar.system.api.Category;
import ez.minar.system.api.Function;
import ez.minar.system.api.FunctionManager;
import ez.minar.system.api.NewFunction;
import ez.minar.system.events.EventHandler;
import ez.minar.system.events.impl.Render2DEvent;
import ez.minar.system.events.impl.UpdateEvent;
import ez.minar.system.menu.ClickGui;
import ez.minar.system.menu.ThemeManager;
import ez.minar.system.settings.impl.BooleanSetting;
import ez.minar.system.settings.impl.ListSetting;
import ez.minar.utils.helpers.KeyHelper;
import ez.minar.utils.render.RenderUtil;
import ez.minar.utils.render.msdf.Msdf;
import ez.minar.utils.render.msdf.MsdfFont;
import ez.minar.utils.render.pipeline.TexturePipeline;
import ez.minar.utils.render.scissor.Scissor;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.gui.screen.ChatScreen;
import ez.minar.system.settings.impl.ListSetting;
import ez.minar.utils.helpers.KeyHelper;
import ez.minar.utils.render.RenderUtil;
import ez.minar.utils.render.msdf.Msdf;
import ez.minar.utils.render.pipeline.TexturePipeline;
import ez.minar.utils.render.scissor.Scissor;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.util.DefaultSkinHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.SkinTextures;
import net.minecraft.item.ItemStack;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.world.GameMode;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.scoreboard.ScoreboardDisplaySlot;
import net.minecraft.scoreboard.ScoreboardEntry;
import org.lwjgl.glfw.GLFW;

import java.awt.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static com.ibm.icu.impl.ValidIdentifiers.Datatype.x;

@NewFunction(name = "HUD", desc = "Минималистичный HUD с ватермаркой и активными биндами", category = Category.RENDER)
public class HUD extends Function {
    public static HUD Instance;
    private static final float PADDING = 7f;
    private static final float GAP = 5f;
    private static final float WATERMARK_HEIGHT = 20f;
    private static final float WATERMARK_PANEL_GAP = 2f;
    private static final float KEYBINDS_HEADER_HEIGHT = 22f;
    private static final float KEYBINDS_ROW_HEIGHT = 17f;
    private static final float WATERMARK_TEXT_SIZE = 9.2f;
    private static final float INFO_TEXT_SIZE = 7.8f;
    private static final float WATERMARK_ICON_SIZE = 10f;
    private static final float WATERMARK_ICON_GAP = 4f;
    private static final float KEYBINDS_TEXT_SIZE = 8f;
    private static final float KEYBINDS_ANIMATION_SPEED = 0.0075f;
    private static final float KEYBINDS_ROW_ANIMATION_SPEED = 0.018f;
    private static final float KEYBINDS_SIZE_ANIMATION_SPEED = 0.018f;
    private static final float KEYBINDS_TEXT_SLIDE = 14f;
    private static final float KEYBINDS_ROW_DELAY_PROGRESS = 0.45f;
    private static final float POTIONS_HEADER_HEIGHT = 22f;
    private static final float POTIONS_ROW_HEIGHT = 17f;
    private static final float POTIONS_TEXT_SIZE = 8f;
    private static final float POTIONS_ANIMATION_SPEED = 0.0075f;
    private static final float POTIONS_ROW_ANIMATION_SPEED = 0.018f;
    private static final float POTIONS_SIZE_ANIMATION_SPEED = 0.018f;
    private static final float POTIONS_TEXT_SLIDE = 14f;
    private static final float POTIONS_ROW_DELAY_PROGRESS = 0.45f;
    private static final float TARGET_HUD_WIDTH = 124f;
    private static final float TARGET_HUD_HEIGHT = 47f;
    private static final float ROUND_TARGET_HUD_WIDTH = 112f;
    private static final float ROUND_TARGET_HUD_HEIGHT = 39f;
    private static final float TARGET_HUD_STYLE_MENU_WIDTH = 96f;
    private static final float TARGET_HUD_STYLE_MENU_ROW = 17f;
    private static final float TARGET_HUD_STYLE_MENU_ANIMATION_SPEED = 0.018f;
    private static final float HUD_STYLE_MENU_WIDTH = 118f;
    private static final float TARGET_HUD_STYLE_ANIMATION_SPEED = 0.014f;
    private static final String GLASS_STYLE_DEFAULT = "Default";
    private static final String GLASS_STYLE_ICE = "\u041b\u0451\u0434";
    private static final String GLASS_STYLE_LIQUID = "\u0416\u0438\u0434\u043a\u043e\u0435 \u0441\u0442\u0435\u043a\u043b\u043e";

    private static final float TARGET_HUD_HEAD_SIZE = 34f;
    private static final float TARGET_HUD_HEAD_RADIUS = 5f;
    private static final float ROUND_TARGET_HUD_HEAD_SIZE = 25f;

    private static final float TARGET_HUD_TEXT_SIZE = 7.6f;
    private static final float TARGET_HUD_HEALTH_SIZE = 7.4f;
    private static final Color TARGET_HUD_ABSORPTION_COLOR = new Color(245, 204, 96);

    private static final float TARGET_HUD_ARMOR_SCALE = 0.58f;

    private static final float TARGET_HUD_ANIMATION_SPEED = 0.012f;

    private final BooleanSetting watermark = new BooleanSetting("Watermark", true);
    private final ListSetting hudGlassStyle = new ListSetting("\u0421\u0442\u0438\u043b\u044c HUD", GLASS_STYLE_DEFAULT, GLASS_STYLE_ICE, GLASS_STYLE_LIQUID);
    private final ListSetting watermarkStyle = new ListSetting("Watermark Style", "Default", "LiquidGlass Watermark");
    private final BooleanSetting watermarkLiquidGlass = new BooleanSetting("Watermark \u041b\u0451\u0434", false);
    private final BooleanSetting watermarkFluidGlass = new BooleanSetting("Watermark \u0416\u0438\u0434\u043a\u043e\u0435 \u0441\u0442\u0435\u043a\u043b\u043e", false);
    private final BooleanSetting keybindsLiquidGlass = new BooleanSetting("Keybinds \u041b\u0451\u0434", false);
    private final BooleanSetting keybindsFluidGlass = new BooleanSetting("Keybinds \u0416\u0438\u0434\u043a\u043e\u0435 \u0441\u0442\u0435\u043a\u043b\u043e", false);
    private final BooleanSetting potionsLiquidGlass = new BooleanSetting("Potions \u041b\u0451\u0434", false);
    private final BooleanSetting potionsFluidGlass = new BooleanSetting("Potions \u0416\u0438\u0434\u043a\u043e\u0435 \u0441\u0442\u0435\u043a\u043b\u043e", false);
    private final BooleanSetting targetHudLiquidGlass = new BooleanSetting("TargetHUD \u041b\u0451\u0434", false);
    private final BooleanSetting targetHudFluidGlass = new BooleanSetting("TargetHUD \u0416\u0438\u0434\u043a\u043e\u0435 \u0441\u0442\u0435\u043a\u043b\u043e", false);
    private final BooleanSetting hotbarLiquidGlass = new BooleanSetting("Hotbar \u041b\u0451\u0434", false);
    private final BooleanSetting hotbarFluidGlass = new BooleanSetting("Hotbar \u0416\u0438\u0434\u043a\u043e\u0435 \u0441\u0442\u0435\u043a\u043b\u043e", false);
    private final BooleanSetting keybinds = new BooleanSetting("Keybinds", true);
    private final BooleanSetting targetHud = new BooleanSetting("TargetHUD", true);
    private final ListSetting targetHudStyle = new ListSetting("Стиль TargetHUD", "Тандерхак", "Круглый");
    private final BooleanSetting potions = new BooleanSetting("Potions", true);
    private final BooleanSetting hotbar = new BooleanSetting("Hotbar", true);
    private final BooleanSetting scoreboard = new BooleanSetting("Scoreboard", true);
    private final ez.minar.system.settings.impl.NumberSetting blurStrength = new ez.minar.system.settings.impl.NumberSetting("Сила блюра", 25.0, 0.0, 100.0, 1.0);
    private final BooleanSetting shader = new BooleanSetting("Шейдер", false);
    private DragTarget dragging = DragTarget.NONE;
    private boolean previousMouseDown;
    private boolean previousRightMouseDown;
    private float dragOffsetX;
    private float dragOffsetY;
    private float watermarkX = 8f;
    private float watermarkY = 8f;
    private float watermarkWidth;
    private float watermarkHeight = WATERMARK_HEIGHT;
    private float watermarkScale = 1.0f;
    private boolean watermarkFps = true;
    private boolean watermarkLogo = true;
    private boolean watermarkPing = true;
    private boolean watermarkServer = true;
    private boolean watermarkTps = true;
    private long lastTickTime = -1L;
    private float currentTps = 20f;
    private float keybindsX = 8f;
    private float keybindsY = 37f;
    private float keybindsWidth;
    private float keybindsHeight = KEYBINDS_HEADER_HEIGHT + KEYBINDS_ROW_HEIGHT;
    private float keybindsDisplayWidth = 96f;
    private float keybindsDisplayHeight;
    private float keybindsProgress;
    private float targetHudX = 8f;
    private float targetHudY = 63f;
    private float potionsX = 8f;
    private float potionsY = 116f;
    private float potionsWidth;
    private float potionsHeight = POTIONS_HEADER_HEIGHT + POTIONS_ROW_HEIGHT;
    private float potionsDisplayWidth = 96f;
    private float potionsDisplayHeight;
    private float potionsProgress;
    private float targetHudProgress;
    private float scoreboardX = 8f;
    private float scoreboardY = 160f;
    private float scoreboardWidth = 120f;
    private float scoreboardHeight = 40f;
    private float lastTargetScoreboardWidth = -1f;
    private float currentScoreboardWidth = 120f;
    private float startScoreboardWidth = 120f;
    private long scoreboardAnimationStart = 0L;
    private boolean targetHudStyleMenuOpen;
    private float targetHudStyleMenuProgress;
    private float targetHudStyleProgress;
    private float targetHudStyleMenuX;
    private float targetHudStyleMenuY;
    private HudElement styleMenuTarget = HudElement.WATERMARK;
    private float currentMouseX;
    private float currentMouseY;
    private LivingEntity displayedTarget;
    private long lastAnimationFrame;
    private long lastHotbarFrame;
    private boolean hotbarAnimationInitialized;
    private float hotbarSelectorX;
    private String selectedItemName = "";
    private String selectedItemKey = "";
    private long selectedItemNameStarted;
    private boolean hotbarValuesInitialized;
    private float displayedHealth;
    private float displayedMaxHealth;
    private float displayedAbsorption;
    private float displayedFood;
    private float displayedArmor;
    private float displayedExperienceProgress;
    private float displayedExperienceLevel;
    private final float[] hotbarSlotProgress = new float[9];
    private final Map<Function, Float> keybindsRowProgress = new HashMap<>();
    private final Map<String, Float> potionsRowProgress = new HashMap<>();
    private final Map<String, SmoothTextState> smoothTextStates = new HashMap<>();

    public HUD() {
        Instance = this;
        watermark.runnable(() -> {
            watermarkStyle.setVisible(false);
            watermarkLiquidGlass.setVisible(false);
            watermarkFluidGlass.setVisible(false);
        });
        keybinds.runnable(() -> {
            keybindsLiquidGlass.setVisible(false);
            keybindsFluidGlass.setVisible(false);
        });
        potions.runnable(() -> {
            potionsLiquidGlass.setVisible(false);
            potionsFluidGlass.setVisible(false);
        });
        targetHud.runnable(() -> {
            targetHudStyle.setVisible(targetHud.isEnabled());
            targetHudLiquidGlass.setVisible(false);
            targetHudFluidGlass.setVisible(false);
        });
        hotbar.runnable(() -> {
            hotbarLiquidGlass.setVisible(false);
            hotbarFluidGlass.setVisible(false);
        });
        addSettings(watermark, targetHud, hotbar, keybinds, potions, scoreboard);
    }

    @EventHandler
    public void onRender2D(Render2DEvent event) {
        DrawContext context = event.getContext();
        if (context == null || mc.options.hudHidden) {
            return;
        }

        if (!isHudVisibleScreen()) {
            return;
        }

        boolean chatOpen = mc.currentScreen instanceof ChatScreen;
        List<Function> activeBinds = getActiveBinds();
        List<StatusEffectInstance> activePotions = getActivePotions();
        boolean keybindsVisible = keybinds.isEnabled() && (!activeBinds.isEmpty() || chatOpen);
        boolean potionsVisible = potions.isEnabled() && (!activePotions.isEmpty() || chatOpen);
        LivingEntity auraTarget = getAuraTarget();
        LivingEntity targetHudEntity = auraTarget != null ? auraTarget : isHudEditScreen() ? mc.player : null;
        boolean targetHudVisible = targetHud.isEnabled() && targetHudEntity != null;
        updateAnimations(keybindsVisible, targetHudVisible, potionsVisible, activeBinds, activePotions);
        updateDragging();

        if (watermark.isEnabled()) {
            renderWatermark(context, watermarkX, watermarkY);
        }

        if (targetHud.isEnabled() && targetHudProgress > 0.02f) {
            renderTargetHud(context, targetHudX, targetHudY, targetHudEntity, targetHudProgress);
        }

        if (hotbar.isEnabled()) {
            renderHotbarElement(context);
        }

        if (scoreboard.isEnabled()) {
            renderCustomScoreboard(context, scoreboardX, scoreboardY);
        }

        if (targetHudStyleMenuOpen && targetHudStyleMenuProgress > 0.01f) {
            renderHudStyleMenu(context, targetHudStyleMenuX, targetHudStyleMenuY, targetHudStyleMenuProgress);
        }
    }

    @EventHandler
    public void onUpdate(UpdateEvent event) {
        if (lastTickTime < 0L) {
            lastTickTime = System.currentTimeMillis();
            return;
        }
        long now = System.currentTimeMillis();
        long delta = now - lastTickTime;
        lastTickTime = now;
        if (delta > 0L) {
            currentTps = currentTps * 0.9f + (1000f / delta) * 0.1f;
        }
    }

    private String getServerName() {
        if (mc.getCurrentServerEntry() != null && mc.getCurrentServerEntry().address != null) {
            return mc.getCurrentServerEntry().address;
        }
        return "\u041b\u043e\u043a\u0430\u043b\u044c\u043d\u0430\u044f";
    }

    private void renderWatermark(DrawContext context, float x, float y) {
        MsdfFont font = Msdf.SF_REGULAR;

        float s = watermarkScale;
        float textSize = 7.5f * s;
        float elemHeight = 8f * s + 2f * s;
        float elemPadX = 3f * s;
        float elemRadius = 2f * s;
        float gapX = 2.5f * s;
        float gapY = 2.5f * s;

        String fps = "Fps: " + mc.getCurrentFps();
        String logo = "Achrone";
        String ping = "Ping: " + getPing() + "ms";
        String server = "Server: " + getServerName();
        String tps = "Tps: " + String.format(Locale.US, "%.1f", currentTps);

        Color themeColor = ThemeManager.getThemeColor();
        Color textColor = new Color(238, 238, 242);
        Color bgColor = new Color(18, 18, 22, 200);

        List<String> row1 = new ArrayList<>();
        if (watermarkFps) row1.add(fps);
        if (watermarkLogo) row1.add(logo);
        if (watermarkPing) row1.add(ping);

        List<String> row2 = new ArrayList<>();
        if (watermarkServer) row2.add(server);
        if (watermarkTps) row2.add(tps);

        if (row1.isEmpty() && row2.isEmpty()) {
            return;
        }

        float row1W = 0f;
        for (String e : row1) {
            row1W += elemPadX * 2f + Msdf.width(font, e, textSize);
        }
        row1W += Math.max(0, row1.size() - 1) * gapX;

        float row2W = 0f;
        for (String e : row2) {
            row2W += elemPadX * 2f + Msdf.width(font, e, textSize);
        }
        row2W += Math.max(0, row2.size() - 1) * gapX;

        boolean hasRow2 = !row2.isEmpty();
        float width = Math.max(row1W, row2W);
        float height = elemHeight * (hasRow2 ? 2 : 1) + (hasRow2 ? gapY : 0f);
        watermarkWidth = width;
        watermarkHeight = height;

        float row2Offset = hasRow2 ? elemHeight + gapY : 0f;
        float textY1 = y + (elemHeight - Msdf.height(font, textSize)) / 2f;
        float textY2 = textY1 + row2Offset;

        float cx1 = x;
        for (String e : row1) {
            float w = elemPadX * 2f + Msdf.width(font, e, textSize);
            RenderUtil.rect(cx1, y, w, elemHeight, elemRadius, bgColor);
            RenderUtil.text(context, font, cx1 + elemPadX, textY1, e, textSize, themeColor);
            cx1 += w + gapX;
        }

        float cx2 = x;
        for (String e : row2) {
            float w = elemPadX * 2f + Msdf.width(font, e, textSize);
            RenderUtil.rect(cx2, y + row2Offset, w, elemHeight, elemRadius, bgColor);
            RenderUtil.text(context, font, cx2 + elemPadX, textY2, e, textSize, textColor);
            cx2 += w + gapX;
        }
    }

    private float renderWatermarkDivider(DrawContext context, float cursorX, float y, float sepPad, float sepWidth, float height) {
        cursorX += sepPad;
        RenderUtil.rect(cursorX, y + height * 0.28f, sepWidth, height * 0.44f, 0f,
                new Color(120, 126, 136, 110));
        cursorX += sepWidth + sepPad;
        return cursorX;
    }

    private Color[] getAnimatedGradient(String text, Color start, Color end) {
        Color[] colors = new Color[text.length()];
        long time = System.currentTimeMillis();
        for (int i = 0; i < text.length(); i++) {
            float offset = (time - i * 120L) % 2000L / 2000f;
            if (offset < 0) offset += 1.0f;
            float t = (float) (Math.sin(offset * Math.PI * 2) * 0.5 + 0.5);
            colors[i] = new Color(ez.minar.utils.render.utils.ColorHelper.lerpColor(start.getRGB(), end.getRGB(), t), true);
        }
        return colors;
    }

    private void renderGlassWatermarkBackground(float x, float y, float width, float height) {
        float radius = 8.5f;
        RenderUtil.shadow(x + 0.5f, y + 1.2f, width - 1f, height - 0.4f, radius,
                10f, 0.34f, 2.4f, new Color(0, 0, 0, 170));
        renderGlassFill(x, y, width, height, radius, 0.9f);
        RenderUtil.outline(x + 0.45f, y + 0.45f, width - 0.9f, height - 0.9f, radius,
                0.85f, new Color(255, 255, 255, 82), new Color(255, 255, 255, 44),
                withAlpha(ThemeManager.getThemeColor(), 52), new Color(255, 255, 255, 24));
        RenderUtil.rect(x + 6f, y + 2.1f, width - 12f, 1.05f, 0.5f,
                new Color(255, 255, 255, 54), new Color(255, 255, 255, 6));
        renderShaderEffect("WatermarkGlass", x, y, width, height, radius, 1f);
    }

    private void renderKeybinds(DrawContext context, float x, float y, List<Function> activeBinds, float progress) {
        float eased = easeOutCubic(progress);
        float width = keybindsDisplayWidth;
        float height = keybindsDisplayHeight;

        keybindsWidth = width;
        keybindsHeight = height;

        // Светлый фон панели кейбиндов вручную
        renderElementPanel(HudElement.KEYBINDS, x, y, width, height, 7f, eased);

        if (height <= 0.5f) {
            return;
        }

        Scissor.push(x, y, width, height);

        // Темный заголовок
        float headerProgress = clamp(height / KEYBINDS_HEADER_HEIGHT, 0f, 1f);
        float headerOpacity = eased * easeOutCubic(headerProgress);

        if (headerOpacity > 0.01f) {
            renderHudHeader(HudElement.KEYBINDS, x, y, width, KEYBINDS_HEADER_HEIGHT * headerProgress, 0f, 0f, 6f, 6f, headerOpacity);

            RenderUtil.text(
                    context,
                    x + 24,
                    centeredTextY(y, KEYBINDS_HEADER_HEIGHT, KEYBINDS_TEXT_SIZE),
                    "Keybinds",
                    KEYBINDS_TEXT_SIZE,
                    withOpacity(new Color(238, 238, 242), headerOpacity),
                    "center"
            );

            RenderUtil.text(
                    context,
                    Msdf.WTMICO,
                    x + width - 8f,
                    y + 7f,
                    "I",
                    KEYBINDS_TEXT_SIZE,
                    withOpacity(ThemeManager.getThemeColor(), headerOpacity),
                    "right"
            );
        }

        float rowY = y + KEYBINDS_HEADER_HEIGHT;
        float rowsOpenProgress = clamp(
                (progress - KEYBINDS_ROW_DELAY_PROGRESS) / (1f - KEYBINDS_ROW_DELAY_PROGRESS),
                0f,
                1f
        );

        for (Function function : activeBinds) {
            float rowProgress = easeOutCubic(keybindsRowProgress.getOrDefault(function, 0f))
                    * easeOutCubic(rowsOpenProgress);

            if (rowProgress <= 0.02f) {
                continue;
            }

            float rowHeight = KEYBINDS_ROW_HEIGHT * rowProgress;
            float visibleByPanel = clamp((y + height - rowY) / Math.max(1f, rowHeight), 0f, 1f);

            if (visibleByPanel <= 0.02f) {
                break;
            }

            float rowOpacity = eased * rowProgress * easeOutCubic(visibleByPanel);
            float slide = KEYBINDS_TEXT_SLIDE * (1f - rowProgress);
            String key = KeyHelper.getKeyName(function.getKeybind());

            RenderUtil.text(
                    context,
                    x + PADDING - slide,
                    centeredTextY(rowY, rowHeight, KEYBINDS_TEXT_SIZE),
                    function.getName(),
                    KEYBINDS_TEXT_SIZE,
                    withOpacity(new Color(230, 230, 235), rowOpacity) // светлее текст строк
            );

            RenderUtil.text(
                    context,
                    x + width - PADDING + slide - 1.5f,
                    centeredTextY(rowY, rowHeight, KEYBINDS_TEXT_SIZE),
                    key,
                    KEYBINDS_TEXT_SIZE,
                    withOpacity(ThemeManager.getThemeColor(), rowOpacity),
                    "right"
            );

            rowY += rowHeight;
        }

        Scissor.pop();
    }


    private void renderPotions(DrawContext context, float x, float y, List<StatusEffectInstance> effects, float progress) {
        float eased = easeOutCubic(progress);
        float width = potionsDisplayWidth;
        float height = potionsDisplayHeight;

        potionsWidth = width;
        potionsHeight = height;

        renderElementPanel(HudElement.POTIONS, x, y, width, height, 7f, eased);

        if (height <= 0.5f) {
            return;
        }

        Scissor.push(x, y, width, height);

        float headerProgress = clamp(height / POTIONS_HEADER_HEIGHT, 0f, 1f);
        float headerOpacity = eased * easeOutCubic(headerProgress);

        if (headerOpacity > 0.01f) {
            renderHudHeader(HudElement.POTIONS, x, y, width, POTIONS_HEADER_HEIGHT * headerProgress, 0f, 0f, 6f, 6f, headerOpacity);

            RenderUtil.text(
                    context,
                    x + 22,
                    centeredTextY(y, POTIONS_HEADER_HEIGHT, POTIONS_TEXT_SIZE),
                    "Potions",
                    POTIONS_TEXT_SIZE,
                    withOpacity(new Color(238, 238, 242), headerOpacity),
                    "center"
            );
            RenderUtil.text(
                    context,
                    Msdf.WTMICO,
                    x + width - 8f,
                    y + 7f,
                    "J",
                    POTIONS_TEXT_SIZE,
                    withOpacity(ThemeManager.getThemeColor(), headerOpacity),
                    "right"
            );

        }

        float rowY = y + POTIONS_HEADER_HEIGHT;
        float rowsOpenProgress = clamp(
                (progress - POTIONS_ROW_DELAY_PROGRESS) / (1f - POTIONS_ROW_DELAY_PROGRESS),
                0f,
                1f
        );

        for (StatusEffectInstance effect : effects) {
            String id = getPotionId(effect);

            float rowProgress = easeOutCubic(potionsRowProgress.getOrDefault(id, 0f))
                    * easeOutCubic(rowsOpenProgress);

            if (rowProgress <= 0.02f) {
                continue;
            }

            float rowHeight = POTIONS_ROW_HEIGHT * rowProgress;
            float visibleByPanel = clamp((y + height - rowY) / Math.max(1f, rowHeight), 0f, 1f);

            if (visibleByPanel <= 0.02f) {
                break;
            }

            float rowOpacity = eased * rowProgress * easeOutCubic(visibleByPanel);
            float slide = POTIONS_TEXT_SLIDE * (1f - rowProgress);

            String name = getPotionName(effect);
            String duration = formatPotionDuration(effect);

            float iconSize = 9f;
            float iconGap = 4f;
            net.minecraft.util.Identifier spriteId = net.minecraft.client.gui.hud.InGameHud.getEffectTexture(effect.getEffectType());

            float textX = x + PADDING - slide + (spriteId != null ? iconSize + iconGap : 0);
            float textMaxWidth = width - PADDING * 2f - 39f - (spriteId != null ? iconSize + iconGap : 0);
            float textY = centeredTextY(rowY, rowHeight, POTIONS_TEXT_SIZE);
            float textWidth = ez.minar.utils.render.msdf.Msdf.width(name, POTIONS_TEXT_SIZE);
            Color textColor = withOpacity(new Color(230, 230, 235), rowOpacity);

            if (spriteId != null) {
                int color = new java.awt.Color(255, 255, 255, (int) (rowOpacity * 255)).getRGB();
                context.drawGuiTexture(net.minecraft.client.gl.RenderPipelines.GUI_TEXTURED, spriteId, (int) (x + PADDING - slide), (int) (centeredTextY(rowY, rowHeight, iconSize) - 1f) + 2, 9, 9, color);
            }

            if (textWidth > textMaxWidth) {
                float overflow = textWidth - textMaxWidth;
                float cycleDuration = Math.max(1.8f, overflow * 2f / 26f);
                float phase = (System.currentTimeMillis() % (long) (cycleDuration * 1000f)) / (cycleDuration * 1000f);
                float offset = overflow * (0.5f - 0.5f * (float) Math.cos(phase * Math.PI * 2f));

                ez.minar.utils.render.scissor.Scissor.push(textX, textY - 2f, textMaxWidth, POTIONS_TEXT_SIZE + 4f);
                RenderUtil.text(context, textX - offset, textY, name, POTIONS_TEXT_SIZE, textColor);
                ez.minar.utils.render.scissor.Scissor.pop();
            } else {
                RenderUtil.text(context, textX, textY, name, POTIONS_TEXT_SIZE, textColor);
            }

            renderAnimatedValue(
                    context,
                    "potion.duration." + id,
                    duration,
                    x + width - PADDING + slide,
                    centeredTextY(rowY, rowHeight, POTIONS_TEXT_SIZE),
                    POTIONS_TEXT_SIZE,
                    withOpacity(ThemeManager.getThemeColor(), rowOpacity),
                    "right"
            );

            rowY += rowHeight;
        }

        Scissor.pop();
    }

    private float animatedHealthProgress = 1.0f;
    private float armorAlpha = 0.0f;

    private void renderTargetHud(DrawContext context, float x, float y, LivingEntity auraTarget, float progress) {
        float roundProgress = 1f;
        float hudOpacity = easeOutCubic(progress);
        float thunderOpacity = hudOpacity * (1f - roundProgress);
        float roundOpacity = hudOpacity * roundProgress;

        if (thunderOpacity > 0.02f) {
            renderThunderTargetHud(context, x, y, auraTarget, thunderOpacity);
        }
        if (roundOpacity > 0.02f) {
            renderRoundTargetHud(context, x, y, auraTarget, roundOpacity);
        }
    }

    private void renderThunderTargetHud(DrawContext context, float x, float y, LivingEntity auraTarget, float progress) {
        LivingEntity target = auraTarget != null ? auraTarget : displayedTarget;

        // РЈРїСЂР°РІР»СЏРµРј alpha РґР»СЏ fade-in Р±СЂРѕРЅРё
        float targetArmorAlpha = auraTarget != null ? 1.0f : 0.0f;
        armorAlpha += (targetArmorAlpha - armorAlpha) * 0.12f;

        if (target == null) return;

        displayedTarget = auraTarget != null ? auraTarget : displayedTarget;

        float eased = clamp(progress, 0f, 1f);
        float posX = x;
        float posY = y;
        float width = TARGET_HUD_WIDTH;
        float height = TARGET_HUD_HEIGHT;
        float padding = 8f;

        // --- Р РµРЅРґРµСЂРёРј HUD ---
        renderElementPanel(HudElement.TARGET_HUD, posX, posY, width - 12, height - 5, 9f, eased);

        // --- Р‘СЂРѕРЅСЏ СЃРІРµСЂС…Сѓ HUD, РіРѕСЂРёР·РѕРЅС‚Р°Р»СЊРЅРѕ ---
        float armorX = posX + padding;
        float armorY = posY - 16f; // РЅР°Рґ РіРѕР»РѕРІРѕР№ Рё РЅРёРєРѕРј
        renderArmor(context, target, armorX, armorY, armorAlpha); // РїРµСЂРµРґР°РµРј alpha РґР»СЏ fade-in

        Scissor.push(posX, posY, width, height);

        // Р“РѕР»РѕРІР°
        float headX = posX + padding;
        float headY = posY + (height - TARGET_HUD_HEAD_SIZE) / 2f;
        renderTargetHead(context, target, headX - 4, headY - 2.5f, eased);

        // РРјСЏ Рё HP
        float infoX = headX + TARGET_HUD_HEAD_SIZE;
        float infoRight = posX + width - padding;
        float infoWidth = infoRight - infoX;
        String name = target.getName().getString();

        float health = Math.max(0f, target.getHealth());
        float maxHealth = Math.max(1f, target.getMaxHealth());
        float absorption = Math.max(0f, target.getAbsorptionAmount());
        float targetHealthProgress = clamp(health / maxHealth, 0f, 1f);
        float absorptionProgress = clamp(absorption / maxHealth, 0f, 1f);

        // РџР»Р°РІРЅР°СЏ Р°РЅРёРјР°С†РёСЏ РїРѕР»РѕСЃРєРё HP
        animatedHealthProgress += (targetHealthProgress - animatedHealthProgress) * 0.12f;

        // HP РІ РїСЂРѕС†РµРЅС‚Р°С…
        float healthPercent = (health / maxHealth) * 100f;
        String healthText = Math.round(healthPercent) + "%";

        float contentY = posY + (height - 28f) / 2f;
        float textY = contentY;
        float barY = contentY + 23f;

        // РРјСЏ
        RenderUtil.text(context, infoX, textY - 3,
                trimToWidth(name, infoWidth - 25f, TARGET_HUD_TEXT_SIZE),
                TARGET_HUD_TEXT_SIZE,
                withOpacity(new Color(245, 245, 248), eased));


        // Р¤РѕРЅ РїРѕР»РѕСЃРєРё HP
        float barWidth = infoWidth - 7f;
        RenderUtil.rect(infoX, barY - 5, barWidth, 9f, 3.5f,
                withOpacity(new Color(42, 42, 46), eased));

        // РџРѕР»РѕСЃРєР° HP
        RenderUtil.rect(infoX, barY - 5, Math.max(5f, barWidth * animatedHealthProgress), 9f, 3.5f,
                withOpacity(brighten(ThemeManager.getThemeColor(), 0.08f), eased));

        if (absorptionProgress > 0.001f) {
            RenderUtil.rect(infoX, barY + 0.7f, Math.max(4f, barWidth * absorptionProgress), 3.1f, 1.55f,
                    withOpacity(TARGET_HUD_ABSORPTION_COLOR, eased));
        }

        // HP С‚РµРєСЃС‚ РІ РїСЂРѕС†РµРЅС‚Р°С… СЃРїСЂР°РІР°
        renderAnimatedValue(context, "target.thunder.health", healthText, infoRight - 40, textY + 18,
                TARGET_HUD_HEALTH_SIZE, withOpacity(new Color(236, 236, 240), eased), "center");

        Scissor.pop();
    }

    private void renderRoundTargetHud(DrawContext context, float x, float y, LivingEntity auraTarget, float progress) {
        LivingEntity target = auraTarget != null ? auraTarget : displayedTarget;
        float targetArmorAlpha = auraTarget != null ? 1.0f : 0.0f;
        armorAlpha += (targetArmorAlpha - armorAlpha) * 0.12f;

        if (target == null) return;

        displayedTarget = auraTarget != null ? auraTarget : displayedTarget;

        float eased = clamp(progress, 0f, 1f);
        float width = ROUND_TARGET_HUD_WIDTH + 10;
        float height = ROUND_TARGET_HUD_HEIGHT;
        float padding = 7f;

        renderElementPanel(HudElement.TARGET_HUD, x, y, width, height, 8f, eased);

        float health = Math.max(0f, target.getHealth());
        float maxHealth = Math.max(1f, target.getMaxHealth());
        float absorption = Math.max(0f, target.getAbsorptionAmount());
        float targetHealthProgress = clamp(health / maxHealth, 0f, 1f);
        float absorptionProgress = clamp(absorption / maxHealth, 0f, 1f);
        animatedHealthProgress += (targetHealthProgress - animatedHealthProgress) * 0.12f;

        Scissor.push(x, y, width, height);

        float headX = x + padding;
        float headY = y + (height - ROUND_TARGET_HUD_HEAD_SIZE) / 2f;
        renderTargetHead(context, target, headX, headY, eased, ROUND_TARGET_HUD_HEAD_SIZE, 4f);

        float infoX = headX + ROUND_TARGET_HUD_HEAD_SIZE + 6f;
        String name = target.getName().getString();
        String distance = mc.player == null ? "Distance: 0.0" : String.format(Locale.US, "Distance: %.1f", mc.player.distanceTo(target));

        RenderUtil.text(context, infoX, y + 9f,
                trimToWidth(name, 48f, 8.2f),
                8.2f,
                withOpacity(new Color(245, 245, 248), eased));
        renderAnimatedValue(context, "target.round.distance", distance, infoX, y + 21.5f,
                6.5f, withOpacity(new Color(235, 235, 238), eased), "left");

        float circleX = x + width - 18f;
        float circleY = y + height / 2f;
        RenderUtil.circleOutline(circleX, circleY, 12f, 3.2f, withOpacity(new Color(86, 86, 90), eased));
        RenderUtil.circleOutline(circleX, circleY, 12f, 3.2f, -90f, animatedHealthProgress,
                withOpacity(ThemeManager.getThemeColor(), eased));
        if (absorptionProgress > 0.001f) {
            RenderUtil.circleOutline(circleX, circleY, 15f, 2.4f, -90f, absorptionProgress,
                    withOpacity(TARGET_HUD_ABSORPTION_COLOR, eased));
        }

        String healthText = formatHealth(health);
        renderAnimatedValue(context, "target.round.health", healthText, circleX + 0.1f, circleY - 4.4f,
                7f, withOpacity(new Color(246, 246, 248), eased), "center");

        Scissor.pop();

        renderArmor(context, target, x + 41f, y + height + 4f, armorAlpha * eased);
    }

    private void renderHudStyleMenu(DrawContext context, float x, float y, float progress) {
        float eased = easeOutCubic(progress);
        float height = getHudStyleMenuHeight();
        float slide = (1f - eased) * 7f;
        float drawY = y - slide;
        renderPanel("HudStyleMenu", x, drawY, HUD_STYLE_MENU_WIDTH, height * eased, 6f, eased);

        if (eased <= 0.08f) {
            return;
        }

        Scissor.push(x, drawY, HUD_STYLE_MENU_WIDTH, height * eased);
        RenderUtil.text(context, x + 7f, drawY + 5.4f, "HUD", 7.4f,
                withOpacity(new Color(225, 225, 230), eased));

        if (styleMenuTarget == HudElement.TARGET_HUD) {
            RenderUtil.text(context, x + 7f, drawY + TARGET_HUD_STYLE_MENU_ROW + 5.4f, "TargetHUD Shape", 7.4f,
                    withOpacity(new Color(225, 225, 230), eased));
            renderTargetHudStyleOption(context, x, drawY + TARGET_HUD_STYLE_MENU_ROW * 2f, "\u0422\u0430\u043d\u0434\u0435\u0440\u0445\u0430\u043a", eased);
            renderTargetHudStyleOption(context, x, drawY + TARGET_HUD_STYLE_MENU_ROW * 3f, "\u041a\u0440\u0443\u0433\u043b\u044b\u0439", eased);
        }
        if (styleMenuTarget == HudElement.WATERMARK) {
            renderWatermarkToggle(context, x, drawY + TARGET_HUD_STYLE_MENU_ROW, "Fps", watermarkFps, eased);
            renderWatermarkToggle(context, x, drawY + TARGET_HUD_STYLE_MENU_ROW * 2f, "Logo", watermarkLogo, eased);
            renderWatermarkToggle(context, x, drawY + TARGET_HUD_STYLE_MENU_ROW * 3f, "Ping", watermarkPing, eased);
            renderWatermarkToggle(context, x, drawY + TARGET_HUD_STYLE_MENU_ROW * 4f, "Server", watermarkServer, eased);
            renderWatermarkToggle(context, x, drawY + TARGET_HUD_STYLE_MENU_ROW * 5f, "Tps", watermarkTps, eased);
            float sliderY = drawY + TARGET_HUD_STYLE_MENU_ROW * 6f;
            RenderUtil.text(context, x + 8f, sliderY + 4f, "Size " + Math.round(watermarkScale * 100) + "%", 7f,
                    withOpacity(new Color(225, 225, 230), eased));
            float barX = x + 10f;
            float barY = sliderY + TARGET_HUD_STYLE_MENU_ROW - 9f;
            float barW = HUD_STYLE_MENU_WIDTH - 20f;
            RenderUtil.rect(barX, barY, barW, 2f, 1f, withOpacity(new Color(80, 80, 85), eased));
            float t = (watermarkScale - 0.5f) / 1.0f;
            float knobX = barX + t * barW;
            RenderUtil.rect(knobX - 2f, barY - 3f, 4f, 8f, 1f, withOpacity(ThemeManager.getThemeColor(), eased));
        }
        if (styleMenuTarget == HudElement.SCOREBOARD) {
            renderHudStyleToggle(context, x, drawY + TARGET_HUD_STYLE_MENU_ROW, "Scoreboard", scoreboard.isEnabled(), eased);
        }
        Scissor.pop();
    }

    private void renderHudStyleToggle(DrawContext context, float x, float y, String label, boolean selected, float opacity) {
        boolean hovered = isInside(currentMouseX, currentMouseY, x, y, HUD_STYLE_MENU_WIDTH, TARGET_HUD_STYLE_MENU_ROW);
        if (hovered || selected) {
            RenderUtil.rect(x + 3f, y + 2f, HUD_STYLE_MENU_WIDTH - 6f, TARGET_HUD_STYLE_MENU_ROW - 4f, 4f,
                    selected ? withOpacity(ThemeManager.getThemeColor(), 0.55f * opacity) : withOpacity(new Color(38, 38, 42, 210), opacity));
        }
        RenderUtil.text(context, x + 8f, y + 5.2f, label, 7f,
                withOpacity(selected ? new Color(255, 255, 255) : new Color(210, 210, 215), opacity));
    }

    private void renderWatermarkToggle(DrawContext context, float x, float y, String label, boolean enabled, float opacity) {
        boolean hovered = isInside(currentMouseX, currentMouseY, x, y, HUD_STYLE_MENU_WIDTH, TARGET_HUD_STYLE_MENU_ROW);
        if (hovered || enabled) {
            RenderUtil.rect(x + 3f, y + 2f, HUD_STYLE_MENU_WIDTH - 6f, TARGET_HUD_STYLE_MENU_ROW - 4f, 4f,
                    enabled ? withOpacity(ThemeManager.getThemeColor(), 0.55f * opacity) : withOpacity(new Color(38, 38, 42, 210), opacity));
        }
        RenderUtil.text(context, x + 8f, y + 5.2f, label, 7f,
                withOpacity(enabled ? new Color(255, 255, 255) : new Color(160, 160, 165), opacity));
    }

    private void renderTargetHudStyleOption(DrawContext context, float x, float y, String style, float opacity) {
        boolean selected = targetHudStyle.isEnabled(style);
        boolean hovered = isInside(currentMouseX, currentMouseY, x, y, HUD_STYLE_MENU_WIDTH, TARGET_HUD_STYLE_MENU_ROW);
        if (hovered || selected) {
            RenderUtil.rect(x + 3f, y + 2f, HUD_STYLE_MENU_WIDTH - 6f, TARGET_HUD_STYLE_MENU_ROW - 4f, 4f,
                    selected ? withOpacity(ThemeManager.getThemeColor(), 0.55f * opacity) : withOpacity(new Color(38, 38, 42, 210), opacity));
        }
        RenderUtil.text(context, x + 8f, y + 5.2f, style, 7f,
                withOpacity(selected ? new Color(255, 255, 255) : new Color(210, 210, 215), opacity));
    }

    private void renderTargetHead(DrawContext context, LivingEntity target, float x, float y, float opacity) {
        renderTargetHead(context, target, x, y, opacity, TARGET_HUD_HEAD_SIZE, TARGET_HUD_HEAD_RADIUS);
    }

    private void renderTargetHead(DrawContext context, LivingEntity target, float x, float y, float opacity, float size, float radius) {
        if (target instanceof PlayerEntity player) {
            SkinTextures skin = player instanceof AbstractClientPlayerEntity clientPlayer
                    ? clientPlayer.getSkin()
                    : DefaultSkinHelper.getSkinTextures(player.getUuid());
            Identifier texture = skin.body().texturePath();
            renderRoundedSkinPart(texture, x, y, size, radius,
                    8f / 64f, 8f / 64f, 8f / 64f, 8f / 64f, opacity);
            renderRoundedSkinPart(texture, x, y, size, radius,
                    40f / 64f, 8f / 64f, 8f / 64f, 8f / 64f, opacity);
            return;
        }
        RenderUtil.rect(x, y, size, size, radius,
                withOpacity(new Color(34, 34, 38), opacity));
        String letter = target.getName().getString().isEmpty() ? "?" : target.getName().getString().substring(0, 1).toUpperCase(Locale.ROOT);
        RenderUtil.text(context, x + size / 2f, y + size * 0.31f, letter, size * 0.35f,
                withOpacity(new Color(250, 250, 252), opacity), "center");
    }

    private void renderRoundedSkinPart(Identifier texture, float x, float y, float size, float radius,
                                       float u, float v, float uvWidth, float uvHeight, float opacity) {
        var view = mc.getTextureManager().getTexture(texture).getGlTextureView();
        TexturePipeline.draw(RenderUtil.createProjection(), x, y, size, view,
                argb(255f * opacity, 255, 255, 255), radius, 0f, u, v, uvWidth, uvHeight, false);
    }

    private void renderArmor(DrawContext context, LivingEntity target, float x, float y, float opacity) {
        EquipmentSlot[] slots = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
        float step = 13f;
        for (int i = 0; i < slots.length; i++) {
            float iconX = x + i * step;
            ItemStack stack = target.getEquippedStack(slots[i]);

            context.getMatrices().pushMatrix();
            context.getMatrices().translate(iconX, y);
            context.getMatrices().scale(TARGET_HUD_ARMOR_SCALE, TARGET_HUD_ARMOR_SCALE);
            context.drawItem(stack, 0, 0);
            context.drawStackOverlay(mc.textRenderer, stack, 0, 0);
            context.getMatrices().popMatrix();
        }
    }


    private List<StatusEffectInstance> getActivePotions() {
        List<StatusEffectInstance> result = new ArrayList<>();
        if (mc.player == null) {
            return result;
        }

        result.addAll(mc.player.getStatusEffects());
        result.sort(Comparator.comparing(this::getPotionName));
        return result;
    }

    private List<StatusEffectInstance> getRenderablePotions() {
        List<StatusEffectInstance> result = getActivePotions();

        if (mc.player != null) {
            for (StatusEffectInstance effect : mc.player.getStatusEffects()) {
                potionsRowProgress.putIfAbsent(getPotionId(effect), 0f);
            }
        }

        result.sort(Comparator.comparing(this::getPotionName));
        return result;
    }

    private boolean hasPotion(List<StatusEffectInstance> effects, String id) {
        for (StatusEffectInstance effect : effects) {
            if (getPotionId(effect).equals(id)) {
                return true;
            }
        }
        return false;
    }

    private String getPotionId(StatusEffectInstance effect) {
        return effect.getEffectType().value().getTranslationKey();
    }

    private String getPotionName(StatusEffectInstance effect) {
        String name = effect.getEffectType().value().getName().getString();
        int amplifier = effect.getAmplifier();

        if (amplifier > 0) {
            name += " " + toRoman(amplifier + 1);
        }

        return name;
    }

    private String formatPotionDuration(StatusEffectInstance effect) {
        if (effect.isInfinite()) {
            return "в€ћ";
        }

        int totalSeconds = Math.max(0, effect.getDuration() / 20);
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        return minutes + ":" + (seconds < 10 ? "0" : "") + seconds;
    }

    private String toRoman(int value) {
        return switch (value) {
            case 2 -> "II";
            case 3 -> "III";
            case 4 -> "IV";
            case 5 -> "V";
            default -> String.valueOf(value);
        };
    }

    private List<Function> getActiveBinds() {
        List<Function> result = new ArrayList<>();
        for (Function function : FunctionManager.getFunctions()) {
            if (function != this && function.isEnabled() && function.getKeybind() > 0) {
                result.add(function);
            }
        }
        result.sort(Comparator.comparing(Function::getName));
        return result;
    }

    private List<Function> getRenderableBinds() {
        List<Function> result = new ArrayList<>();
        for (Function function : FunctionManager.getFunctions()) {
            if (function != this && function.getKeybind() > 0
                    && (function.isEnabled() || keybindsRowProgress.getOrDefault(function, 0f) > 0.02f)) {
                result.add(function);
            }
        }
        result.sort(Comparator.comparing(Function::getName));
        return result;
    }

    private int getPing() {
        if (mc.player == null || mc.getNetworkHandler() == null) {
            return 0;
        }

        PlayerListEntry entry = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
        return entry == null ? 0 : entry.getLatency();
    }

    public static boolean shouldDisableDefaultHotbar() {
        return Instance != null && Instance.isEnabled() && Instance.hotbar.isEnabled();
    }

    public static boolean shouldHideDefaultStatusEffectIcons() {
        return Instance != null && Instance.isEnabled();
    }

    private static final float HOTBAR_SLOT_SIZE = 18f;
    private static final float HOTBAR_GAP = 2.5f;
    private static final float HOTBAR_BORDER = 4f;
    private static final float HOTBAR_RADIUS = 6f;
    private static final float HOTBAR_TEXT_SIZE = 7f;
    private static final float HOTBAR_BELOW_GAP = 5f;
    private static final float HOTBAR_SIDE_BAR_WIDTH = 64f;
    private static final float HOTBAR_XP_BAR_HEIGHT = 7f;
    private static final float HOTBAR_BAR_HEIGHT = 9f;
    private static final float HOTBAR_BAR_RADIUS = 3f;
    private static final float HOTBAR_BAR_TEXT_SIZE = 6.2f;
    private static final float HOTBAR_BAR_ICON_SIZE = 6.4f;
    private static final float HOTBAR_EMPTY_SLOT_TEXT_SIZE = 7.2f;
    private static final float HOTBAR_ITEM_NAME_TEXT_SIZE = 7.6f;
    private static final long HOTBAR_ITEM_NAME_DURATION = 1200L;
    private static final long HOTBAR_ITEM_NAME_FADE_OUT = 260L;
    private static final float HOTBAR_SELECTOR_SPEED = 0.024f;
    private static final float HOTBAR_ITEM_SPEED = 0.018f;
    private static final float HOTBAR_VALUE_SPEED = 0.014f;
    private static final float HOTBAR_SELECTED_ITEM_SCALE = 1.30f;
    private static final float HOTBAR_IDLE_ITEM_SCALE = 0.92f;

    private void renderHotbarElement(DrawContext context) {
        if (mc.player == null || mc.world == null) return;

        float screenWidth = RenderUtil.getFixedScaledWidth();
        float screenHeight = RenderUtil.getFixedScaledHeight();

        float totalWidth = 9 * HOTBAR_SLOT_SIZE + 8 * HOTBAR_GAP + 2 * HOTBAR_BORDER;
        float x = (screenWidth - totalWidth) / 2f;
        float y = screenHeight - 29f;

        int selectedSlot = mc.player.getInventory().getSelectedSlot();
        updateHotbarAnimations(x + HOTBAR_BORDER, selectedSlot);

        float height = HOTBAR_SLOT_SIZE + 2 * HOTBAR_BORDER;
        renderElementPanel(HudElement.HOTBAR, x, y, totalWidth, height, HOTBAR_RADIUS, 1f);

        float selectorY = y + HOTBAR_BORDER;

        float slotX = x + HOTBAR_BORDER;
        float slotY = y + HOTBAR_BORDER;
        for (int i = 0; i < 9; i++) {
            float progress = easeOutCubic(hotbarSlotProgress[i]);

            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.isEmpty()) {
                renderEmptySlotNumber(context, slotX, slotY, i, progress);
            } else {
                float scale = lerp(HOTBAR_IDLE_ITEM_SCALE, HOTBAR_SELECTED_ITEM_SCALE, progress);
                float centerX = slotX + HOTBAR_SLOT_SIZE / 2f;
                float centerY = slotY + HOTBAR_SLOT_SIZE / 2f - progress * 1.3f;

                context.getMatrices().pushMatrix();
                context.getMatrices().translate(centerX, centerY);
                context.getMatrices().scale(scale, scale);
                context.drawItem(stack, -8, -8);
                context.drawStackOverlay(mc.textRenderer, stack, -8, -8);
                context.getMatrices().popMatrix();
            }

            slotX += HOTBAR_SLOT_SIZE + HOTBAR_GAP;
        }

        boolean survivalHud = shouldRenderSurvivalHotbarStats();
        float itemNameY = y - 16f;
        if (survivalHud) {
            float experienceY = y - HOTBAR_XP_BAR_HEIGHT - 2f;
            renderExperience(context, x, experienceY, totalWidth);

            float barsY = experienceY - HOTBAR_BAR_HEIGHT - 4f;
            renderHealth(context, x, barsY);
            renderFood(context, x + totalWidth, barsY);
            float armorY = barsY - HOTBAR_BAR_HEIGHT - 2f;
            renderArmor(context, x, armorY);
            itemNameY = armorY - 16f;
        }
        renderSelectedItemName(context, x + totalWidth / 2f, itemNameY);
    }

    private boolean shouldRenderSurvivalHotbarStats() {
        return mc.interactionManager == null || mc.interactionManager.getCurrentGameMode() != GameMode.CREATIVE;
    }

    private void updateHotbarAnimations(float firstSlotX, int selectedSlot) {
        long now = System.currentTimeMillis();
        if (lastHotbarFrame == 0L) {
            lastHotbarFrame = now;
        }

        float delta = Math.min(50f, now - lastHotbarFrame);
        lastHotbarFrame = now;
        updateHotbarValueAnimations(delta);

        float targetSelectorX = firstSlotX + selectedSlot * (HOTBAR_SLOT_SIZE + HOTBAR_GAP);
        if (!hotbarAnimationInitialized) {
            hotbarSelectorX = targetSelectorX;
            for (int i = 0; i < hotbarSlotProgress.length; i++) {
                hotbarSlotProgress[i] = i == selectedSlot ? 1f : 0f;
            }
            hotbarAnimationInitialized = true;
            return;
        }

        hotbarSelectorX = animate(hotbarSelectorX, targetSelectorX, HOTBAR_SELECTOR_SPEED, delta);
        for (int i = 0; i < hotbarSlotProgress.length; i++) {
            hotbarSlotProgress[i] = animate(hotbarSlotProgress[i], i == selectedSlot ? 1f : 0f, HOTBAR_ITEM_SPEED, delta);
        }
    }

    private void updateHotbarValueAnimations(float delta) {
        if (mc.player == null) return;

        float health = mc.player.getHealth();
        float maxHealth = mc.player.getMaxHealth();
        float absorption = mc.player.getAbsorptionAmount();
        float food = mc.player.getHungerManager().getFoodLevel();
        float armor = mc.player.getArmor();
        float experienceProgress = clamp(mc.player.experienceProgress, 0f, 1f);
        float experienceLevel = mc.player.experienceLevel;

        if (!hotbarValuesInitialized) {
            displayedHealth = health;
            displayedMaxHealth = maxHealth;
            displayedAbsorption = absorption;
            displayedFood = food;
            displayedArmor = armor;
            displayedExperienceProgress = experienceProgress;
            displayedExperienceLevel = experienceLevel;
            hotbarValuesInitialized = true;
            return;
        }

        displayedHealth = animate(displayedHealth, health, HOTBAR_VALUE_SPEED, delta);
        displayedMaxHealth = animate(displayedMaxHealth, maxHealth, HOTBAR_VALUE_SPEED, delta);
        displayedAbsorption = animate(displayedAbsorption, absorption, HOTBAR_VALUE_SPEED, delta);
        displayedFood = animate(displayedFood, food, HOTBAR_VALUE_SPEED, delta);
        displayedArmor = animate(displayedArmor, armor, HOTBAR_VALUE_SPEED, delta);
        displayedExperienceProgress = animate(displayedExperienceProgress, experienceProgress, HOTBAR_VALUE_SPEED, delta);
        displayedExperienceLevel = animate(displayedExperienceLevel, experienceLevel, HOTBAR_VALUE_SPEED, delta);
    }

    private void renderEmptySlotNumber(DrawContext context, float slotX, float slotY, int slot, float selectedProgress) {
        float selected = clamp(selectedProgress, 0f, 1f);
        float eased = smoothStep(selected);
        float scale = lerp(1f, 1.8f, eased);
        float size = HOTBAR_EMPTY_SLOT_TEXT_SIZE * scale;
        String text = String.valueOf(slot + 1);

        RenderUtil.text(context, slotX + HOTBAR_SLOT_SIZE / 2f,
                slotY + (HOTBAR_SLOT_SIZE - Msdf.height(size)) / 2f - 0.4f,
                text, size, new Color(225, 225, 230), "center");
    }

    private void renderHealth(DrawContext context, float x, float y) {
        if (mc.player == null) return;

        float health = hotbarValuesInitialized ? displayedHealth : mc.player.getHealth();
        float absorption = hotbarValuesInitialized ? displayedAbsorption : mc.player.getAbsorptionAmount();

        renderIconRow(context, x, y, health, "K", new Color(38, 34, 36, 210), new Color(244, 111, 118));

        if (absorption > 0.05f) {
            renderIconRow(context, x, y, absorption, "K", new Color(0, 0, 0, 0), new Color(245, 204, 96));
        }
    }

    private void renderExperience(DrawContext context, float x, float y, float barWidth) {
        if (mc.player == null) return;

        float experienceFill = hotbarValuesInitialized ? displayedExperienceProgress : clamp(mc.player.experienceProgress, 0f, 1f);

        RenderUtil.rect(x, y, barWidth, HOTBAR_XP_BAR_HEIGHT, HOTBAR_BAR_RADIUS, new Color(33, 39, 31, 210));
        if (experienceFill > 0f) {
            RenderUtil.rect(x, y, barWidth * experienceFill, HOTBAR_XP_BAR_HEIGHT, HOTBAR_BAR_RADIUS, new Color(153, 199, 116));
        }

        String text = mc.player.experienceLevel > 0
                ? String.valueOf(Math.round(hotbarValuesInitialized ? displayedExperienceLevel : mc.player.experienceLevel))
                : Math.round(experienceFill * 100f) + "%";
        renderAnimatedBarText(context, "hotbar.experience", x, y, barWidth, text);
    }

    private void renderFood(DrawContext context, float x, float y) {
        if (mc.player == null) return;

        float food = hotbarValuesInitialized ? displayedFood : mc.player.getHungerManager().getFoodLevel();

        // Right-aligned icons
        float iconSize = HOTBAR_BAR_ICON_SIZE + 2f;
        float spacing = 8f;
        float startX = x - (10 * spacing);

        renderIconRow(context, startX, y, food, "L", new Color(39, 35, 31, 210), new Color(191, 143, 103));
    }

    private void renderBarText(DrawContext context, float x, float y, float width, String text) {
        RenderUtil.text(context, x + width / 2f, y + (HOTBAR_BAR_HEIGHT - Msdf.height(HOTBAR_BAR_TEXT_SIZE)) / 2f - 0.3f,
                text, HOTBAR_BAR_TEXT_SIZE, new Color(255, 255, 255), "center");
    }

    private void renderBarIcon(DrawContext context, float x, float y, String icon) {
        RenderUtil.text(context, Msdf.WTMICO, x + 4f,
                y + (HOTBAR_BAR_HEIGHT - Msdf.height(Msdf.WTMICO, HOTBAR_BAR_ICON_SIZE)) / 2f - 0.15f,
                icon, HOTBAR_BAR_ICON_SIZE, new Color(255, 255, 255));
    }

    private void renderArmor(DrawContext context, float x, float y) {
        if (mc.player == null) return;

        float armor = hotbarValuesInitialized ? displayedArmor : mc.player.getArmor();
        if (armor <= 0.05f && mc.player.getArmor() <= 0) return;

        renderIconRow(context, x, y, armor, "M", new Color(35, 36, 38, 210), new Color(184, 188, 193));
    }

    private void renderIconRow(DrawContext context, float x, float y, float value, String icon, Color emptyColor, Color fullColor) {
        int maxIcons = 10;
        float spacing = 8f;
        float iconSize = HOTBAR_BAR_ICON_SIZE + 2f;
        float drawY = y + (HOTBAR_BAR_HEIGHT - Msdf.height(Msdf.WTMICO, iconSize)) / 2f - 0.15f;
        float iconWidth = Msdf.width(Msdf.WTMICO, icon, iconSize);

        for (int i = 0; i < maxIcons; i++) {
            float iconX = x + i * spacing;

            if (emptyColor.getAlpha() > 0) {
                RenderUtil.text(context, Msdf.WTMICO, iconX, drawY, icon, iconSize, emptyColor);
            }

            float targetValue = (i * 2f) + 2f;
            float currentValue = value;

            if (currentValue >= targetValue) {
                RenderUtil.text(context, Msdf.WTMICO, iconX, drawY, icon, iconSize, fullColor);
            } else if (currentValue > targetValue - 2f) {
                float fraction = (currentValue - (targetValue - 2f)) / 2f;
                Scissor.push(iconX - 1f, drawY - 2f, iconWidth * fraction + 1f, iconSize + 4f);
                RenderUtil.text(context, Msdf.WTMICO, iconX, drawY, icon, iconSize, fullColor);
                Scissor.pop();
            }
        }
    }

    private void renderSelectedItemName(DrawContext context, float centerX, float y) {
        if (mc.player == null) return;

        ItemStack stack = mc.player.getInventory().getSelectedStack();
        String key = stack.isEmpty() ? "" : stack.getItem() + "|" + stack.getName().getString();
        if (!key.equals(selectedItemKey)) {
            selectedItemKey = key;
            selectedItemName = stack.isEmpty() ? "" : stack.getName().getString();
            selectedItemNameStarted = selectedItemName.isEmpty() ? 0L : System.currentTimeMillis();
        }

        if (selectedItemName.isEmpty() || selectedItemNameStarted == 0L) return;

        long elapsed = System.currentTimeMillis() - selectedItemNameStarted;
        if (elapsed > HOTBAR_ITEM_NAME_DURATION) return;

        float enter = clamp(elapsed / 280f, 0f, 1f);
        float exit = elapsed <= HOTBAR_ITEM_NAME_DURATION - HOTBAR_ITEM_NAME_FADE_OUT
                ? 1f
                : 1f - clamp((elapsed - (HOTBAR_ITEM_NAME_DURATION - HOTBAR_ITEM_NAME_FADE_OUT)) / (float) HOTBAR_ITEM_NAME_FADE_OUT, 0f, 1f);
        float scale = easeOutBack(enter);
        float opacity = easeOutCubic(exit);
        float textSize = HOTBAR_ITEM_NAME_TEXT_SIZE * scale;
        float width = Msdf.width(selectedItemName, textSize) + 16f * scale;
        float height = 14f * scale;
        float drawX = centerX - width / 2f;
        float drawY = Math.max(2f, y - 2f * (1f - enter));

        if (getGlassStyle() != GlassStyle.DEFAULT) {
            renderGlassPanel(HudElement.HOTBAR, drawX, drawY, width, height, 5f * scale, opacity);
        } else {
            RenderUtil.rect(drawX, drawY, width, height, 5f * scale,
                    withOpacity(new Color(25, 25, 27, 225), opacity));
        }
        RenderUtil.text(context, centerX, drawY + (height - Msdf.height(textSize)) / 2f - 0.6f,
                selectedItemName, textSize, withOpacity(new Color(255, 255, 255), opacity), "center");
    }

    private void renderAnimatedBarText(DrawContext context, String key, float x, float y, float width, String text) {
        renderAnimatedValue(context, key, text, x + width / 2f,
                y + (HOTBAR_BAR_HEIGHT - Msdf.height(HOTBAR_BAR_TEXT_SIZE)) / 2f - 0.3f,
                HOTBAR_BAR_TEXT_SIZE, new Color(255, 255, 255), "center");
    }

    private void renderAnimatedValue(DrawContext context, String key, String value, float x, float y, float size,
                                     Color color, String align) {
        if (value == null || value.isEmpty()) {
            return;
        }

        SmoothTextState state = smoothTextStates.computeIfAbsent(key, ignored -> new SmoothTextState());
        String rendered = updateSmoothTextState(state, value);
        RenderUtil.text(context, x, y, rendered, size, color, align);
    }

    private String updateSmoothTextState(SmoothTextState state, String value) {
        NumericText parsed = parseNumericText(value);
        long now = System.currentTimeMillis();

        if (parsed == null) {
            state.numeric = false;
            state.lastUpdate = now;
            return value;
        }

        if (!state.numeric || !parsed.sameFormat(state)) {
            state.numeric = true;
            state.lastUpdate = now;
            state.prefix = parsed.prefix;
            state.suffix = parsed.suffix;
            state.plus = parsed.plus;
            state.decimalsA = parsed.decimalsA;
            state.decimalsB = parsed.decimalsB;
            state.displayedA = parsed.valueA;
            state.targetA = parsed.valueA;
            state.displayedB = parsed.valueB;
            state.targetB = parsed.valueB;
            return value;
        }

        state.targetA = parsed.valueA;
        state.targetB = parsed.valueB;

        float delta = state.lastUpdate == 0L ? 0f : Math.min(50f, now - state.lastUpdate);
        state.lastUpdate = now;

        state.displayedA = animate(state.displayedA, state.targetA, 0.018f, delta);
        state.displayedB = animate(state.displayedB, state.targetB, 0.018f, delta);

        if (state.plus) {
            return state.prefix + formatNumeric(state.displayedA, state.decimalsA) + "+" + formatNumeric(state.displayedB, state.decimalsB) + state.suffix;
        }
        return state.prefix + formatNumeric(state.displayedA, state.decimalsA) + state.suffix;
    }

    private void renderPanel(String id, float x, float y, float width, float height, float radius) {
        renderPanel(id, x, y, width, height, radius, 1f);
    }

    private void renderPanel(String id, float x, float y, float width, float height, float radius, float opacity) {
        if (width <= 0.5f || height <= 0.5f || opacity <= 0.01f) return;
        RenderUtil.hudBlur(x, y, width, height, radius, (float) blurStrength.getValue(), opacity, new Color(18, 18, 22, 120));
        renderShaderEffect(id, x, y, width, height, radius, opacity);
    }

    public void renderElementPanel(HudElement element, float x, float y, float width, float height, float radius, float opacity) {
        if (getGlassStyle() != GlassStyle.DEFAULT) {
            renderGlassPanel(element, x, y, width, height, radius, opacity);
            renderShaderEffect(element != null ? element.name() : "NONE", x, y, width, height, radius, opacity);
            return;
        }
        if (width <= 0.5f || height <= 0.5f || opacity <= 0.01f) return;
        RenderUtil.hudBlur(x, y, width, height, radius, (float) blurStrength.getValue(), opacity, new Color(18, 18, 22, 120));
        renderShaderEffect(element != null ? element.name() : "NONE", x, y, width, height, radius, opacity);
    }

    private void renderHudHeader(HudElement element, float x, float y, float width, float height, float tl, float tr, float br, float bl, float opacity) {
        if (getGlassStyle() != GlassStyle.DEFAULT) {
            RenderUtil.rect(x, y, width, height, tl, tr, br, bl,
                    withOpacity(new Color(255, 255, 255, 28), opacity),
                    withOpacity(new Color(255, 255, 255, 10), opacity));
            return;
        }
        RenderUtil.hudBlur(x, y, width, height, tl, tr, br, bl, (float) blurStrength.getValue(), opacity, new Color(18, 18, 22, 140));
    }

    private void renderGlassPanel(HudElement element, float x, float y, float width, float height, float radius, float opacity) {
        if (width <= 0.5f || height <= 0.5f || opacity <= 0.01f) return;
        RenderUtil.shadow(x + 0.5f, y + 1.2f, width - 1f, height - 0.4f, radius,
                10f, 0.28f * opacity, 2.4f, new Color(0, 0, 0, 170));
        renderGlassFill(x, y, width, height, radius, 0.88f * opacity);
        RenderUtil.outline(x + 0.45f, y + 0.45f, width - 0.9f, height - 0.9f, radius,
                0.85f, withOpacity(new Color(255, 255, 255, 82), opacity),
                withOpacity(new Color(255, 255, 255, 44), opacity),
                withOpacity(ThemeManager.getThemeColor(), 0.22f * opacity),
                withOpacity(new Color(255, 255, 255, 24), opacity));
    }

    private void renderGlassFill(float x, float y, float width, float height, float radius, float alpha) {
        Color tint = withAlpha(ThemeManager.getThemeColor(), (int) (72f * Math.clamp(alpha, 0f, 1f)));
        if (getGlassStyle() == GlassStyle.LIQUID) {
            RenderUtil.fluidGlass(x, y, width, height, radius, 1.0f, alpha, tint);
        } else {
            RenderUtil.liquidGlass(x, y, width, height, radius, 1.0f, alpha, tint);
        }
    }

    private GlassStyle getGlassStyle() {
        if (hudGlassStyle.isEnabled(GLASS_STYLE_LIQUID)) {
            return GlassStyle.LIQUID;
        }
        if (hudGlassStyle.isEnabled(GLASS_STYLE_ICE)) {
            return GlassStyle.ICE;
        }
        return getLegacyGlassStyle();
    }

    private GlassStyle getLegacyGlassStyle() {
        if (watermarkFluidGlass.isEnabled() || keybindsFluidGlass.isEnabled()
                || potionsFluidGlass.isEnabled() || targetHudFluidGlass.isEnabled()
                || hotbarFluidGlass.isEnabled()) {
            return GlassStyle.LIQUID;
        }
        if (watermarkLiquidGlass.isEnabled() || watermarkStyle.isEnabled("LiquidGlass Watermark")
                || keybindsLiquidGlass.isEnabled() || potionsLiquidGlass.isEnabled()
                || targetHudLiquidGlass.isEnabled() || hotbarLiquidGlass.isEnabled()) {
            return GlassStyle.ICE;
        }
        return GlassStyle.DEFAULT;
    }

    private void setGlassStyle(GlassStyle style) {
        hudGlassStyle.setMode(getGlassStyleName(style));
        watermarkLiquidGlass.setEnabled(false);
        watermarkFluidGlass.setEnabled(false);
        keybindsLiquidGlass.setEnabled(false);
        keybindsFluidGlass.setEnabled(false);
        potionsLiquidGlass.setEnabled(false);
        potionsFluidGlass.setEnabled(false);
        targetHudLiquidGlass.setEnabled(false);
        targetHudFluidGlass.setEnabled(false);
        hotbarLiquidGlass.setEnabled(false);
        hotbarFluidGlass.setEnabled(false);
        watermarkStyle.setMode("Default");
    }

    private String getGlassStyleName(GlassStyle style) {
        return switch (style) {
            case DEFAULT -> GLASS_STYLE_DEFAULT;
            case ICE -> GLASS_STYLE_ICE;
            case LIQUID -> GLASS_STYLE_LIQUID;
        };
    }

    private void renderShaderEffect(String id, float x, float y, float width, float height, float radius, float opacity) {
        if (!shader.isEnabled() || opacity < 0.01f) return;

        long time = System.currentTimeMillis();
        float cycleDuration = 2500f;
        
        int hash = id.hashCode();
        float offset = Math.abs(hash % 1000) / 1000f;
        float phase = ((time + (long)(offset * cycleDuration)) % (long) cycleDuration) / cycleDuration;

        int angleIndex = Math.abs(hash) % 4;
        float baseAngle = 45f + angleIndex * 90f;
        float angle = (float) Math.toRadians(baseAngle);

        ez.minar.utils.render.pipeline.SweepPipeline.draw(
                RenderUtil.createProjection(), 
                x, y, width, height, 
                radius, radius, radius, radius, 
                0f, phase, angle, opacity);
    }

    private void updateAnimations(boolean keybindsVisible, boolean targetHudVisible, boolean potionsVisible, List<Function> activeBinds, List<StatusEffectInstance> activePotions) {
        long now = System.currentTimeMillis();
        if (lastAnimationFrame == 0L) {
            lastAnimationFrame = now;
            return;
        }

        float delta = Math.min(50f, now - lastAnimationFrame);
        lastAnimationFrame = now;



        keybindsProgress = animate(keybindsProgress, keybindsVisible ? 1f : 0f, KEYBINDS_ANIMATION_SPEED, delta);
        targetHudProgress = animate(targetHudProgress, targetHudVisible ? 1f : 0f, TARGET_HUD_ANIMATION_SPEED, delta);
        potionsProgress = animate(potionsProgress, potionsVisible ? 1f : 0f, POTIONS_ANIMATION_SPEED, delta);
        targetHudStyleMenuProgress = animate(targetHudStyleMenuProgress, targetHudStyleMenuOpen && isHudEditScreen() ? 1f : 0f,
                TARGET_HUD_STYLE_MENU_ANIMATION_SPEED, delta);
        targetHudStyleProgress = animate(targetHudStyleProgress, targetHudStyle.isEnabled("Круглый") ? 1f : 0f,
                TARGET_HUD_STYLE_ANIMATION_SPEED, delta);
        if (targetHudProgress <= 0.001f && !targetHudVisible) {
            displayedTarget = null;
        }

        for (Function function : activeBinds) {
            keybindsRowProgress.putIfAbsent(function, 0f);
        }

        Iterator<Map.Entry<Function, Float>> iterator = keybindsRowProgress.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Function, Float> entry = iterator.next();
            Function function = entry.getKey();
            boolean rowVisible = keybindsVisible && function.isEnabled() && function.getKeybind() > 0;
            float next = animate(entry.getValue(), rowVisible ? 1f : 0f, KEYBINDS_ROW_ANIMATION_SPEED, delta);
            if (!rowVisible && next <= 0.001f) {
                iterator.remove();
            } else {
                entry.setValue(next);
            }
        }

        for (StatusEffectInstance effect : activePotions) {
            potionsRowProgress.putIfAbsent(getPotionId(effect), 0f);
        }

        Iterator<Map.Entry<String, Float>> potionsIterator = potionsRowProgress.entrySet().iterator();
        while (potionsIterator.hasNext()) {
            Map.Entry<String, Float> entry = potionsIterator.next();
            boolean rowVisible = potionsVisible && hasPotion(activePotions, entry.getKey());
            float next = animate(entry.getValue(), rowVisible ? 1f : 0f, POTIONS_ROW_ANIMATION_SPEED, delta);
            if (!rowVisible && next <= 0.001f) {
                potionsIterator.remove();
            } else {
                entry.setValue(next);
            }
        }

        List<Function> renderableBinds = getRenderableBinds();
        float targetWidth = !keybindsVisible && renderableBinds.isEmpty()
                ? keybindsDisplayWidth
                : calculateKeybindsWidth(renderableBinds);
        float targetHeight = calculateTargetKeybindsHeight(keybindsVisible, renderableBinds);
        keybindsDisplayWidth = animate(keybindsDisplayWidth, targetWidth, KEYBINDS_SIZE_ANIMATION_SPEED, delta);
        keybindsDisplayHeight = animate(keybindsDisplayHeight, targetHeight, KEYBINDS_SIZE_ANIMATION_SPEED, delta);

        List<StatusEffectInstance> renderablePotions = getRenderablePotions();
        float targetPotionsWidth = !potionsVisible && renderablePotions.isEmpty()
                ? potionsDisplayWidth
                : calculatePotionsWidth(renderablePotions);
        float targetPotionsHeight = calculateTargetPotionsHeight(potionsVisible, renderablePotions);
        potionsDisplayWidth = animate(potionsDisplayWidth, targetPotionsWidth, POTIONS_SIZE_ANIMATION_SPEED, delta);
        potionsDisplayHeight = animate(potionsDisplayHeight, targetPotionsHeight, POTIONS_SIZE_ANIMATION_SPEED, delta);
    }

    private float calculateKeybindsWidth(List<Function> binds) {
        float width = Math.max(96f, Msdf.width("Keybinds", KEYBINDS_TEXT_SIZE) + PADDING * 2f);
        for (Function function : binds) {
            String key = KeyHelper.getKeyName(function.getKeybind());
            width = Math.max(width, PADDING * 2f + Msdf.width(function.getName(), KEYBINDS_TEXT_SIZE) + 18f + Msdf.width(key, KEYBINDS_TEXT_SIZE));
        }
        return width;
    }

    private float calculatePotionsWidth(List<StatusEffectInstance> effects) {
        float width = Math.max(96f, Msdf.width("Potions", POTIONS_TEXT_SIZE) + PADDING * 2f);
        for (StatusEffectInstance effect : effects) {
            width = Math.max(width, PADDING * 2f + Msdf.width(getPotionName(effect), POTIONS_TEXT_SIZE) + 18f + Msdf.width(formatPotionDuration(effect), POTIONS_TEXT_SIZE));
        }
        return width;
    }

    private float calculatePotionsRowsHeight() {
        float height = 0f;
        for (float progress : potionsRowProgress.values()) {
            height += POTIONS_ROW_HEIGHT * easeOutCubic(progress);
        }
        return height;
    }

    private float calculateTargetPotionsHeight(boolean potionsVisible, List<StatusEffectInstance> renderablePotions) {
        if (!potionsVisible && renderablePotions.isEmpty()) {
            return 0f;
        }

        float headerHeight = POTIONS_HEADER_HEIGHT * easeOutCubic(potionsProgress);
        float rowsProgress = clamp((potionsProgress - POTIONS_ROW_DELAY_PROGRESS) / (1f - POTIONS_ROW_DELAY_PROGRESS), 0f, 1f);
        return headerHeight + calculatePotionsRowsHeight() * easeOutCubic(rowsProgress);
    }

    private float calculateRowsHeight() {
        float height = 0f;
        for (float progress : keybindsRowProgress.values()) {
            height += KEYBINDS_ROW_HEIGHT * easeOutCubic(progress);
        }
        return height;
    }

    private float calculateTargetKeybindsHeight(boolean keybindsVisible, List<Function> renderableBinds) {
        if (!keybindsVisible && renderableBinds.isEmpty()) {
            return 0f;
        }

        float headerHeight = KEYBINDS_HEADER_HEIGHT * easeOutCubic(keybindsProgress);
        float rowsProgress = clamp((keybindsProgress - KEYBINDS_ROW_DELAY_PROGRESS) / (1f - KEYBINDS_ROW_DELAY_PROGRESS), 0f, 1f);
        return headerHeight + calculateRowsHeight() * easeOutCubic(rowsProgress);
    }

    private void updateDragging() {
        if (!isHudEditScreen() || mc.getWindow() == null) {
            dragging = DragTarget.NONE;
            previousMouseDown = false;
            previousRightMouseDown = false;
            targetHudStyleMenuOpen = false;
            return;
        }

        long handle = mc.getWindow().getHandle();
        boolean mouseDown = GLFW.glfwGetMouseButton(handle, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
        boolean rightMouseDown = GLFW.glfwGetMouseButton(handle, GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS;
        double[] rawX = new double[1];
        double[] rawY = new double[1];
        GLFW.glfwGetCursorPos(handle, rawX, rawY);
        float mouseX = (float) (rawX[0] * RenderUtil.getFixedScaledWidth() / mc.getWindow().getWidth());
        float mouseY = (float) (rawY[0] * RenderUtil.getFixedScaledHeight() / mc.getWindow().getHeight());
        currentMouseX = mouseX;
        currentMouseY = mouseY;

        if (rightMouseDown && !previousRightMouseDown) {
            handleHudStyleRightClick(mouseX, mouseY);
        }

        if (mouseDown && !previousMouseDown) {
            if (handleHudStyleMenuClick(mouseX, mouseY)) {
                previousMouseDown = mouseDown;
                previousRightMouseDown = rightMouseDown;
                return;
            }
            startDragging(mouseX, mouseY);
        }

        if (!mouseDown) {
            dragging = DragTarget.NONE;
        } else if (dragging != DragTarget.NONE) {
            moveDragged(mouseX, mouseY);
        }

        previousMouseDown = mouseDown;
        previousRightMouseDown = rightMouseDown;
    }

    private void handleHudStyleRightClick(float mouseX, float mouseY) {
        HudElement hoveredElement = getHoveredHudElement(mouseX, mouseY);
        if (hoveredElement != null) {
            styleMenuTarget = hoveredElement;
            targetHudStyleMenuOpen = true;
            targetHudStyleMenuX = clamp(mouseX, 2f, RenderUtil.getFixedScaledWidth() - HUD_STYLE_MENU_WIDTH - 2f);
            targetHudStyleMenuY = clamp(mouseY, 2f, RenderUtil.getFixedScaledHeight() - getHudStyleMenuHeight() - 2f);
            dragging = DragTarget.NONE;
        } else if (!isInside(mouseX, mouseY, targetHudStyleMenuX, targetHudStyleMenuY,
                HUD_STYLE_MENU_WIDTH, getHudStyleMenuHeight())) {
            targetHudStyleMenuOpen = false;
        }
    }

    private boolean handleHudStyleMenuClick(float mouseX, float mouseY) {
        if (!targetHudStyleMenuOpen) {
            return false;
        }

        if (!isInside(mouseX, mouseY, targetHudStyleMenuX, targetHudStyleMenuY,
                HUD_STYLE_MENU_WIDTH, getHudStyleMenuHeight())) {
            targetHudStyleMenuOpen = false;
            return false;
        }

        float optionsY = targetHudStyleMenuY + TARGET_HUD_STYLE_MENU_ROW;
        if (styleMenuTarget == HudElement.TARGET_HUD && mouseY >= optionsY + TARGET_HUD_STYLE_MENU_ROW && mouseY < optionsY + TARGET_HUD_STYLE_MENU_ROW * 2f) {
            targetHudStyle.setMode("\u0422\u0430\u043d\u0434\u0435\u0440\u0445\u0430\u043a");
            targetHudStyleMenuOpen = false;
        } else if (styleMenuTarget == HudElement.TARGET_HUD && mouseY >= optionsY + TARGET_HUD_STYLE_MENU_ROW * 2f) {
            targetHudStyle.setMode("\u041a\u0440\u0443\u0433\u043b\u044b\u0439");
            targetHudStyleMenuOpen = false;
        } else if (styleMenuTarget == HudElement.WATERMARK && mouseY >= optionsY && mouseY < optionsY + TARGET_HUD_STYLE_MENU_ROW) {
            watermarkFps = !watermarkFps;
        } else if (styleMenuTarget == HudElement.WATERMARK && mouseY >= optionsY + TARGET_HUD_STYLE_MENU_ROW && mouseY < optionsY + TARGET_HUD_STYLE_MENU_ROW * 2f) {
            watermarkLogo = !watermarkLogo;
        } else if (styleMenuTarget == HudElement.WATERMARK && mouseY >= optionsY + TARGET_HUD_STYLE_MENU_ROW * 2f && mouseY < optionsY + TARGET_HUD_STYLE_MENU_ROW * 3f) {
            watermarkPing = !watermarkPing;
        } else if (styleMenuTarget == HudElement.WATERMARK && mouseY >= optionsY + TARGET_HUD_STYLE_MENU_ROW * 3f && mouseY < optionsY + TARGET_HUD_STYLE_MENU_ROW * 4f) {
            watermarkServer = !watermarkServer;
        } else if (styleMenuTarget == HudElement.WATERMARK && mouseY >= optionsY + TARGET_HUD_STYLE_MENU_ROW * 4f && mouseY < optionsY + TARGET_HUD_STYLE_MENU_ROW * 5f) {
            watermarkTps = !watermarkTps;
        } else if (styleMenuTarget == HudElement.WATERMARK && mouseY >= optionsY + TARGET_HUD_STYLE_MENU_ROW * 5f) {
            float t = (mouseX - (targetHudStyleMenuX + 10f)) / (HUD_STYLE_MENU_WIDTH - 20f);
            t = clamp(t, 0f, 1f);
            watermarkScale = 0.5f + t * 1.0f;
        } else if (styleMenuTarget == HudElement.SCOREBOARD && mouseY >= optionsY && mouseY < optionsY + TARGET_HUD_STYLE_MENU_ROW) {
            scoreboard.setEnabled(!scoreboard.isEnabled());
        }
        return true;
    }

    private HudElement getHoveredHudElement(float mouseX, float mouseY) {
        if (watermark.isEnabled() && isInside(mouseX, mouseY, watermarkX, watermarkY, watermarkWidth, watermarkHeight))
            return HudElement.WATERMARK;
        if (scoreboard.isEnabled() && isInside(mouseX, mouseY, scoreboardX, scoreboardY, scoreboardWidth, scoreboardHeight))
            return HudElement.SCOREBOARD;
        if (keybinds.isEnabled() && keybindsProgress > 0.2f && isInside(mouseX, mouseY, keybindsX, keybindsY, keybindsWidth, keybindsHeight))
            return HudElement.KEYBINDS;
        if (potions.isEnabled() && potionsProgress > 0.2f && isInside(mouseX, mouseY, potionsX, potionsY, potionsWidth, potionsHeight))
            return HudElement.POTIONS;
        if (targetHud.isEnabled() && targetHudProgress > 0.2f && isInside(mouseX, mouseY, targetHudX, targetHudY, getTargetHudWidth(), getTargetHudHeight()))
            return HudElement.TARGET_HUD;
        if (hotbar.isEnabled() && isInsideHotbar(mouseX, mouseY)) return HudElement.HOTBAR;
        return null;
    }

    private boolean isInsideHotbar(float mouseX, float mouseY) {
        float totalWidth = 9 * HOTBAR_SLOT_SIZE + 8 * HOTBAR_GAP + 2 * HOTBAR_BORDER;
        float x = (RenderUtil.getFixedScaledWidth() - totalWidth) / 2f;
        float y = RenderUtil.getFixedScaledHeight() - 29f;
        float height = HOTBAR_SLOT_SIZE + 2 * HOTBAR_BORDER;
        return isInside(mouseX, mouseY, x, y, totalWidth, height);
    }

    private float getHudStyleMenuHeight() {
        if (styleMenuTarget == HudElement.TARGET_HUD) return TARGET_HUD_STYLE_MENU_ROW * 4f;
        if (styleMenuTarget == HudElement.WATERMARK) return TARGET_HUD_STYLE_MENU_ROW * 7f;
        if (styleMenuTarget == HudElement.SCOREBOARD) return TARGET_HUD_STYLE_MENU_ROW * 2f;
        return TARGET_HUD_STYLE_MENU_ROW * 1f;
    }

    private void startDragging(float mouseX, float mouseY) {
        if (targetHudStyleMenuOpen) {
            return;
        }

        if (targetHud.isEnabled() && targetHudProgress > 0.2f && isInside(mouseX, mouseY, targetHudX, targetHudY, getTargetHudWidth(), getTargetHudHeight())) {
            dragging = DragTarget.TARGET_HUD;
            dragOffsetX = mouseX - targetHudX;
            dragOffsetY = mouseY - targetHudY;
            return;
        }

        if (keybinds.isEnabled() && keybindsProgress > 0.2f && isInside(mouseX, mouseY, keybindsX, keybindsY, keybindsWidth, keybindsHeight)) {
            dragging = DragTarget.KEYBINDS;
            dragOffsetX = mouseX - keybindsX;
            dragOffsetY = mouseY - keybindsY;
            return;
        }

        if (potions.isEnabled() && potionsProgress > 0.2f && isInside(mouseX, mouseY, potionsX, potionsY, potionsWidth, potionsHeight)) {
            dragging = DragTarget.POTIONS;
            dragOffsetX = mouseX - potionsX;
            dragOffsetY = mouseY - potionsY;
            return;
        }

        if (watermark.isEnabled() && isInside(mouseX, mouseY, watermarkX, watermarkY, watermarkWidth, watermarkHeight)) {
            dragging = DragTarget.WATERMARK;
            dragOffsetX = mouseX - watermarkX;
            dragOffsetY = mouseY - watermarkY;
            return;
        }

        if (scoreboard.isEnabled() && isInside(mouseX, mouseY, scoreboardX, scoreboardY, scoreboardWidth, scoreboardHeight)) {
            dragging = DragTarget.SCOREBOARD;
            dragOffsetX = mouseX - scoreboardX;
            dragOffsetY = mouseY - scoreboardY;
        }
    }

    private void moveDragged(float mouseX, float mouseY) {
        if (dragging == DragTarget.WATERMARK) {
            watermarkX = clamp(mouseX - dragOffsetX, 2f, RenderUtil.getFixedScaledWidth() - watermarkWidth - 2f);
            watermarkY = clamp(mouseY - dragOffsetY, 2f, RenderUtil.getFixedScaledHeight() - watermarkHeight - 2f);
        } else if (dragging == DragTarget.SCOREBOARD) {
            scoreboardX = clamp(mouseX - dragOffsetX, 2f, RenderUtil.getFixedScaledWidth() - scoreboardWidth - 2f);
            scoreboardY = clamp(mouseY - dragOffsetY, 2f, RenderUtil.getFixedScaledHeight() - scoreboardHeight - 2f);
        } else if (dragging == DragTarget.KEYBINDS) {
            keybindsX = clamp(mouseX - dragOffsetX, 2f, RenderUtil.getFixedScaledWidth() - keybindsWidth - 2f);
            keybindsY = clamp(mouseY - dragOffsetY, 2f, RenderUtil.getFixedScaledHeight() - keybindsHeight - 2f);
        } else if (dragging == DragTarget.POTIONS) {
            potionsX = clamp(mouseX - dragOffsetX, 2f, RenderUtil.getFixedScaledWidth() - potionsWidth - 2f);
            potionsY = clamp(mouseY - dragOffsetY, 2f, RenderUtil.getFixedScaledHeight() - potionsHeight - 2f);
        } else if (dragging == DragTarget.TARGET_HUD) {
            targetHudX = clamp(mouseX - dragOffsetX, 2f, RenderUtil.getFixedScaledWidth() - getTargetHudWidth() - 2f);
            targetHudY = clamp(mouseY - dragOffsetY, 2f, RenderUtil.getFixedScaledHeight() - getTargetHudHeight() - 2f);
        }
    }

    private float getTargetHudWidth() {
        return lerp(TARGET_HUD_WIDTH, ROUND_TARGET_HUD_WIDTH + 10f, easeOutCubic(targetHudStyleProgress));
    }

    private float getTargetHudHeight() {
        return lerp(TARGET_HUD_HEIGHT, ROUND_TARGET_HUD_HEIGHT, easeOutCubic(targetHudStyleProgress));
    }

    private LivingEntity getAuraTarget() {
        if (mc.player == null || mc.world == null) {
            return null;
        }
        net.minecraft.util.hit.HitResult hit = mc.crosshairTarget;
        if (hit instanceof net.minecraft.util.hit.EntityHitResult ehr
                && ehr.getEntity() instanceof LivingEntity living
                && living != mc.player && living.isAlive()) {
            return living;
        }
        return null;
    }

    private boolean isInside(float mouseX, float mouseY, float x, float y, float width, float height) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

    private boolean isHudVisibleScreen() {
        return mc.currentScreen == null || isHudEditScreen();
    }

    private boolean isHudEditScreen() {
        return mc.currentScreen instanceof ChatScreen || mc.currentScreen instanceof ClickGui;
    }

    private float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private float animate(float current, float target, float speed, float delta) {
        float factor = 1f - (float) Math.exp(-speed * delta);
        float next = current + (target - current) * clamp(factor, 0f, 1f);
        return Math.abs(next - target) < 0.001f ? target : next;
    }

    private double animate(double current, double target, float speed, float delta) {
        double factor = 1d - Math.exp(-speed * delta);
        double next = current + (target - current) * Math.max(0d, Math.min(1d, factor));
        return Math.abs(next - target) < 0.001d ? target : next;
    }

    private float lerp(float from, float to, float progress) {
        return from + (to - from) * clamp(progress, 0f, 1f);
    }

    private float easeOutCubic(float progress) {
        float t = clamp(progress, 0f, 1f) - 1f;
        return t * t * t + 1f;
    }

    private float easeOutBack(float progress) {
        float t = clamp(progress, 0f, 1f) - 1f;
        float c = 1.70158f;
        return 1f + (c + 1f) * t * t * t + c * t * t;
    }

    private float smoothStep(float progress) {
        float t = clamp(progress, 0f, 1f);
        return t * t * (3f - 2f * t);
    }

    private float centeredTextY(float y, float height, float size) {
        return y + (height - Msdf.height(size)) / 2f;
    }

    private float centeredIconY(float y, float height) {
        return y + (height - Msdf.height(Msdf.ICONS, WATERMARK_ICON_SIZE)) / 2f;
    }

    private Color withOpacity(Color color, float opacity) {
        int alpha = (int) (color.getAlpha() * clamp(opacity, 0f, 1f));
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
    }

    private Color withAlpha(Color color, int alpha) {
        int clamped = Math.max(0, Math.min(255, alpha));
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), clamped);
    }

    private Color blend(Color from, Color to, float progress) {
        float t = clamp(progress, 0f, 1f);
        return new Color(
                Math.round(from.getRed() + (to.getRed() - from.getRed()) * t),
                Math.round(from.getGreen() + (to.getGreen() - from.getGreen()) * t),
                Math.round(from.getBlue() + (to.getBlue() - from.getBlue()) * t),
                Math.round(from.getAlpha() + (to.getAlpha() - from.getAlpha()) * t)
        );
    }

    private Color brighten(Color color, float strength) {
        return new Color(
                Math.clamp((int) (color.getRed() + (255 - color.getRed()) * strength), 0, 255),
                Math.clamp((int) (color.getGreen() + (255 - color.getGreen()) * strength), 0, 255),
                Math.clamp((int) (color.getBlue() + (255 - color.getBlue()) * strength), 0, 255),
                color.getAlpha()
        );
    }

    private int argb(float alpha, int red, int green, int blue) {
        int a = Math.clamp((int) alpha, 0, 255);
        return a << 24 | red << 16 | green << 8 | blue;
    }

    private String formatHealth(float health) {
        if (health >= 10f) {
            return String.valueOf(Math.round(health));
        }
        return String.format(Locale.US, "%.1f", health);
    }

    private String trimToWidth(String text, float maxWidth, float size) {
        if (Msdf.width(text, size) <= maxWidth) {
            return text;
        }

        String suffix = "...";
        String trimmed = text;
        while (!trimmed.isEmpty() && Msdf.width(trimmed + suffix, size) > maxWidth) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed.isEmpty() ? suffix : trimmed + suffix;
    }

    private NumericText parseNumericText(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }

        java.util.regex.Matcher plusMatcher = java.util.regex.Pattern
                .compile("^(-?\\d+(?:[\\.,]\\d+)?)\\+(-?\\d+(?:[\\.,]\\d+)?)$")
                .matcher(value);
        if (plusMatcher.matches()) {
            return new NumericText(
                    "",
                    "",
                    true,
                    parseDouble(plusMatcher.group(1)),
                    parseDouble(plusMatcher.group(2)),
                    countDecimals(plusMatcher.group(1)),
                    countDecimals(plusMatcher.group(2))
            );
        }

        java.util.regex.Matcher singleMatcher = java.util.regex.Pattern
                .compile("^(.*?)(-?\\d+(?:[\\.,]\\d+)?)(.*?)$")
                .matcher(value);
        if (singleMatcher.matches()) {
            String prefix = singleMatcher.group(1);
            String number = singleMatcher.group(2);
            String suffix = singleMatcher.group(3);
            if (!containsDigit(prefix) && !containsDigit(suffix)) {
                return new NumericText(prefix, suffix, false, parseDouble(number), 0d, countDecimals(number), 0);
            }
        }

        return null;
    }

    private double parseDouble(String value) {
        return Double.parseDouble(value.replace(',', '.'));
    }

    private int countDecimals(String value) {
        int dot = value.indexOf('.');
        int comma = value.indexOf(',');
        int index = Math.max(dot, comma);
        return index < 0 ? 0 : value.length() - index - 1;
    }

    private boolean containsDigit(String value) {
        for (int i = 0; i < value.length(); i++) {
            if (Character.isDigit(value.charAt(i))) {
                return true;
            }
        }
        return false;
    }

    private String formatNumeric(double value, int decimals) {
        return decimals > 0
                ? String.format(Locale.US, "%." + decimals + "f", value)
                : String.valueOf(Math.round(value));
    }

    public static boolean shouldDisableDefaultScoreboard() {
        return Instance != null && Instance.isEnabled() && Instance.scoreboard.isEnabled();
    }

    private void renderCustomScoreboard(DrawContext context, float x, float y) {
        if (mc.world == null || mc.player == null) return;
        Scoreboard board = mc.world.getScoreboard();
        ScoreboardObjective objective = board.getObjectiveForSlot(ScoreboardDisplaySlot.SIDEBAR);
        if (objective == null) return;

        List<ScoreboardEntry> scores = new ArrayList<>(board.getScoreboardEntries(objective));
        scores.sort(Comparator.comparing(ScoreboardEntry::value).reversed());

        if (scores.size() > 15) {
            scores = scores.subList(0, 15);
        }

        float titleSize = 8.5f;
        float scoreSize = 7.5f;
        float padding = 6f;
        float headerHeight = titleSize + padding * 2f;

        ColoredTextRenderer.ColoredString titleColored = ColoredTextRenderer.ColoredString.fromText(objective.getDisplayName(), ThemeManager.getThemeColor().getRGB());
        float maxWidth = MixedTextRenderer.getWidth(Msdf.SF_BOLD, titleColored, titleSize);

        List<ColoredTextRenderer.ColoredString> entryNames = new ArrayList<>();

        for (ScoreboardEntry score : scores) {
            String owner = score.owner();
            net.minecraft.scoreboard.Team team = board.getScoreHolderTeam(owner);
            net.minecraft.text.Text text = score.display();
            if (text == null) {
                text = net.minecraft.text.Text.literal(owner);
            }
            net.minecraft.text.Text nameText = net.minecraft.scoreboard.Team.decorateName(team, text);
            
            ColoredTextRenderer.ColoredString nameColored = ColoredTextRenderer.ColoredString.fromText(nameText, new Color(245, 245, 245).getRGB());
            entryNames.add(nameColored);

            float width = MixedTextRenderer.getWidth(ez.minar.utils.render.msdf.MsdfManager.getDefault(), nameColored, scoreSize);
            if (width > maxWidth) maxWidth = width;
        }

        float targetWidth = Math.max(105f, maxWidth + padding * 2f);

        if (lastTargetScoreboardWidth == -1f) {
            lastTargetScoreboardWidth = targetWidth;
            currentScoreboardWidth = targetWidth;
            startScoreboardWidth = targetWidth;
        }

        if (Math.abs(targetWidth - lastTargetScoreboardWidth) > 0.1f) {
            startScoreboardWidth = currentScoreboardWidth;
            lastTargetScoreboardWidth = targetWidth;
            scoreboardAnimationStart = System.currentTimeMillis();
        }

        float animProgress = Math.min(1f, (System.currentTimeMillis() - scoreboardAnimationStart) / 400f);
        float eased = easeOutBack(animProgress);
        currentScoreboardWidth = startScoreboardWidth + (targetWidth - startScoreboardWidth) * eased;

        scoreboardWidth = currentScoreboardWidth;
        scoreboardHeight = headerHeight + scores.size() * (scoreSize + 3f) + padding;

        float drawX = x + (targetWidth - currentScoreboardWidth) / 2f;

        // Render main background panel
        renderElementPanel(HudElement.SCOREBOARD, drawX, y, scoreboardWidth, scoreboardHeight, 7f, 1f);

        // Render header background
        renderHudHeader(HudElement.SCOREBOARD, drawX, y, scoreboardWidth, headerHeight, 0f, 0f, 6f, 6f, 1f);

        // Render title
        MixedTextRenderer.drawText(context, Msdf.SF_BOLD, titleColored, drawX + scoreboardWidth / 2f - MixedTextRenderer.getWidth(Msdf.SF_BOLD, titleColored, titleSize) / 2f, centeredTextY(y, headerHeight, titleSize) +2, titleSize);

        float currentY = y + headerHeight + padding;

        // Render entries
        for (int i = 0; i < scores.size(); i++) {
            ColoredTextRenderer.ColoredString nameColored = entryNames.get(i);
            
            MixedTextRenderer.drawText(context, ez.minar.utils.render.msdf.MsdfManager.getDefault(), nameColored, drawX + padding, currentY, scoreSize);

            currentY += scoreSize + 3f;
        }
    }

    private static final class SmoothTextState {
        private boolean numeric;
        private boolean plus;
        private String prefix = "";
        private String suffix = "";
        private double displayedA;
        private double targetA;
        private double displayedB;
        private double targetB;
        private int decimalsA;
        private int decimalsB;
        private long lastUpdate;
    }

    private static final class NumericText {
        private final String prefix;
        private final String suffix;
        private final boolean plus;
        private final double valueA;
        private final double valueB;
        private final int decimalsA;
        private final int decimalsB;

        private NumericText(String prefix, String suffix, boolean plus, double valueA, double valueB, int decimalsA, int decimalsB) {
            this.prefix = prefix;
            this.suffix = suffix;
            this.plus = plus;
            this.valueA = valueA;
            this.valueB = valueB;
            this.decimalsA = decimalsA;
            this.decimalsB = decimalsB;
        }

        private boolean sameFormat(SmoothTextState state) {
            return plus == state.plus
                    && prefix.equals(state.prefix)
                    && suffix.equals(state.suffix)
                    && decimalsA == state.decimalsA
                    && decimalsB == state.decimalsB;
        }
    }

    private enum HudElement {
        WATERMARK,
        SCOREBOARD,
        KEYBINDS,
        POTIONS,
        TARGET_HUD,
        HOTBAR
    }

    private enum GlassStyle {
        DEFAULT,
        ICE,
        LIQUID
    }

    private enum DragTarget {
        NONE,
        WATERMARK,
        SCOREBOARD,
        KEYBINDS,
        POTIONS,
        TARGET_HUD
    }
}


