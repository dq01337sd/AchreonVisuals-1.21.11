package ez.minar.system.features.render;

import ez.minar.system.api.Category;
import ez.minar.system.api.Function;
import ez.minar.system.api.NewFunction;
import ez.minar.system.events.EventHandler;
import ez.minar.system.events.impl.UpdateEvent;
import ez.minar.system.menu.ThemeManager;
import ez.minar.system.settings.impl.BooleanSetting;
import ez.minar.system.settings.impl.ColorSetting;
import ez.minar.system.settings.impl.NumberSetting;
import ez.minar.utils.render.Render3DUtils;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@NewFunction(name = "JumpCircles", desc = "Эффект кругов под ногами при прыжке", category = Category.RENDER)
public class JumpCircles extends Function {
    private static final Identifier CIRCLE_TEXTURE = Identifier.of("atheryx", "images/world/circle.png");
    private static final List<JumpCircle> CIRCLES = new ArrayList<>();
    private static final long LIFE_NANOS = 850_000_000L;
    private static final double GROUND_OFFSET = 0.025;
    private static final float BASE_ALPHA = 0.5f;
    private static final float OUTER_GLOW_SCALE = 2.35f;
    private static final float OUTER_GLOW_ALPHA = 0.28f;
    private static final float INNER_GLOW_SCALE = 1.18f;
    private static final float INNER_GLOW_ALPHA = 0.72f;

    public static JumpCircles Instance;

    private final NumberSetting size = new NumberSetting("Size", 1.25, 0.35, 3.0, 0.05);
    private final BooleanSetting themeColor = new BooleanSetting("Theme color", true);
    private final ColorSetting color = new ColorSetting("Color", new Color(255, 255, 255));

    private boolean wasOnGround;
    private double lastGroundY;

    public JumpCircles() {
        Instance = this;
        addSettings(size, themeColor, color);
        themeColor.runnable(this::updateVisibility);
        updateVisibility();
    }

    @Override
    public void onEnable() {
        wasOnGround = mc.player != null && mc.player.isOnGround();
        lastGroundY = mc.player != null ? mc.player.getY() : 0.0;
    }

    @Override
    public void onDisable() {
        CIRCLES.clear();
        wasOnGround = false;
    }

    @EventHandler
    private void onUpdate(UpdateEvent event) {
        if (mc.player == null || mc.world == null) {
            wasOnGround = false;
            CIRCLES.clear();
            return;
        }

        boolean onGround = mc.player.isOnGround();
        if (onGround) {
            lastGroundY = mc.player.getY();
        } else if (wasOnGround && mc.player.getVelocity().y > 0.0) {
            spawnCircle();
        }

        wasOnGround = onGround;
    }

    private void spawnCircle() {
        Vec3d position = new Vec3d(mc.player.getX(), lastGroundY + GROUND_OFFSET, mc.player.getZ());
        CIRCLES.add(new JumpCircle(position, (float) (Math.random() * 360.0), System.nanoTime()));
    }

    public static void renderWorld(WorldRenderContext context) {
        if (Instance == null || !Instance.isEnabled() || CIRCLES.isEmpty()) return;
        Instance.render(context);
    }

    private void render(WorldRenderContext context) {
        long now = System.nanoTime();
        Color selectedColor = themeColor.isEnabled() ? ThemeManager.getThemeColor() : color.getColor();
        int red = themeColor.isEnabled() ? brighten(selectedColor.getRed()) : selectedColor.getRed();
        int green = themeColor.isEnabled() ? brighten(selectedColor.getGreen()) : selectedColor.getGreen();
        int blue = themeColor.isEnabled() ? brighten(selectedColor.getBlue()) : selectedColor.getBlue();
        float baseSize = (float) size.getValue();

        Iterator<JumpCircle> iterator = CIRCLES.iterator();
        Render3DUtils.TexturedPlaneBatch batch = Render3DUtils.additiveTexturedPlaneBatch(context, CIRCLE_TEXTURE);
        while (iterator.hasNext()) {
            JumpCircle circle = iterator.next();
            float age = (now - circle.spawnedAt) / (float) LIFE_NANOS;

            if (age >= 1f) {
                iterator.remove();
                continue;
            }

            float scale = smooth(Math.clamp(age / 0.48f, 0f, 1f));
            float fade = smooth(Math.clamp((1f - age) / 0.34f, 0f, 1f));
            float currentSize = baseSize * scale;
            float currentAlpha = BASE_ALPHA * fade;

            batch.render(circle.position, currentSize * INNER_GLOW_SCALE, circle.rotation, red, green, blue,
                    currentAlpha * INNER_GLOW_ALPHA);
            batch.render(circle.position, currentSize, circle.rotation, core(red), core(green), core(blue),
                    Math.clamp(currentAlpha * 1.25f, 0f, 1f));
        }
    }

    private static float smooth(float value) {
        return value * value * (3f - 2f * value);
    }

    private static int brighten(int value) {
        return Math.clamp((int) (value + (255 - value) * 0.45f), 0, 255);
    }

    private static int core(int value) {
        return Math.clamp((int) (value + (255 - value) * 0.72f), 0, 255);
    }

    private void updateVisibility() {
        color.setVisible(!themeColor.isEnabled());
    }

    private static class JumpCircle {
        private final Vec3d position;
        private final float rotation;
        private final long spawnedAt;

        private JumpCircle(Vec3d position, float rotation, long spawnedAt) {
            this.position = position;
            this.rotation = rotation;
            this.spawnedAt = spawnedAt;
        }
    }
}
