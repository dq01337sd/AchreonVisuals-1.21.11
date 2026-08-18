package ez.minar.system.features.render;

import ez.minar.system.api.Category;
import ez.minar.system.api.Function;
import ez.minar.system.api.NewFunction;
import ez.minar.system.events.EventHandler;
import ez.minar.system.events.impl.UpdateEvent;
import ez.minar.system.menu.ThemeManager;
import ez.minar.system.settings.impl.BooleanSetting;
import ez.minar.system.settings.impl.ColorSetting;
import ez.minar.system.settings.impl.ModeSetting;
import ez.minar.system.settings.impl.NumberSetting;
import ez.minar.utils.render.Render3DUtils;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

@NewFunction(name = "Fire Fly", desc = "Летающие снежные частицы вокруг игрока", category = Category.RENDER)
public class FireFly extends Function {
    private static final Identifier GLOW_TEXTURE = Identifier.of("atheryx", "images/particles/glow.png");
    private static final Identifier FIREFLY_TEXTURE = Identifier.of("atheryx", "images/particles/firefly.png");
    private static final Identifier STAR_TEXTURE = Identifier.of("atheryx", "images/particles/star.png");
    private static final double MIN_SPAWN_RADIUS = 3.0;
    private static final double MAX_SPAWN_RADIUS = 28.0;
    private static final double SPAWN_HEIGHT = 11.0;
    private static final long MAX_LIFETIME_MILLIS = 8_000L;
    private static final float OUTER_GLOW_SCALE = 2.35f;
    private static final float OUTER_GLOW_ALPHA = 0.28f;
    private static final float INNER_GLOW_SCALE = 1.18f;
    private static final float INNER_GLOW_ALPHA = 0.72f;

    public static FireFly Instance;
    public final ModeSetting texture = new ModeSetting("Текстура", "Glow", "Firefly", "Star");

    public final NumberSetting count = new NumberSetting("Количество", 55, 10, 120, 1);
    public final NumberSetting size = new NumberSetting("Размер", 1.0, 0.4, 3.0, 0.1);
    public final BooleanSetting themeColor = new BooleanSetting("Цвет от темы", true);
    public final ColorSetting color = new ColorSetting("Цвет", new Color(245, 248, 255));

    private final List<SnowParticle> particles = new ArrayList<>();
    private final Random random = new Random();

    public FireFly() {
        Instance = this;
        addSettings(count, size, texture, themeColor, color);
        themeColor.runnable(this::updateVisibility);
        updateVisibility();
    }

    @Override
    public void onDisable() {
        particles.clear();
    }

    @EventHandler
    public void onUpdate(UpdateEvent event) {
        if (mc.player == null || mc.world == null) return;

        Vec3d playerPos = mc.player.getEntityPos();

        Iterator<SnowParticle> iterator = particles.iterator();
        while (iterator.hasNext()) {
            SnowParticle particle = iterator.next();
            particle.update();

            if (particle.isDead(playerPos)) {
                iterator.remove();
            }
        }

        int targetCount = (int) count.getValue();
        while (particles.size() > targetCount) {
            particles.remove(particles.size() - 1);
        }

        while (particles.size() < targetCount) {
            spawnParticle(playerPos);
        }
    }

    public static void renderWorld(WorldRenderContext context) {
        if (Instance == null || !Instance.isEnabled()) return;
        Instance.render(context);
    }

    private void spawnParticle(Vec3d playerPos) {
        double angle = Math.toRadians(random.nextDouble() * 360.0);
        double distance = MIN_SPAWN_RADIUS + random.nextDouble() * (MAX_SPAWN_RADIUS - MIN_SPAWN_RADIUS);
        double height = 1.0 + random.nextDouble() * SPAWN_HEIGHT;

        Vec3d position = playerPos
                .add(Math.cos(angle) * distance, 0.0, Math.sin(angle) * distance)
                .add(0.0, height, 0.0);

        Vec3d velocity = new Vec3d(
                (random.nextDouble() - 0.5) * 0.055,
                -0.035 - random.nextDouble() * 0.055,
                (random.nextDouble() - 0.5) * 0.055
        );

        particles.add(new SnowParticle(
                position,
                velocity,
                0.055f + random.nextFloat() * 0.075f,
                random.nextFloat() * 360f,
                -1.4f + random.nextFloat() * 2.8f,
                0.55f + random.nextFloat() * 0.35f
        ));
    }

    private void render(WorldRenderContext context) {
        if (mc.player == null || mc.world == null || particles.isEmpty()) return;

        Color selectedColor = themeColor.isEnabled() ? ThemeManager.getThemeColor() : color.getColor();
        boolean useTheme = themeColor.isEnabled();
        int red = useTheme ? brighten(selectedColor.getRed()) : selectedColor.getRed();
        int green = useTheme ? brighten(selectedColor.getGreen()) : selectedColor.getGreen();
        int blue = useTheme ? brighten(selectedColor.getBlue()) : selectedColor.getBlue();
        float sizeMultiplier = (float) size.getValue();
        float tickDelta = mc.getRenderTickCounter().getTickProgress(false);
        Identifier particleTexture = getSelectedTexture();
        Render3DUtils.TexturedBillboardBatch outerGlowBatch = Render3DUtils.additiveTexturedBillboardBatch(context, particleTexture);

        for (SnowParticle particle : particles) {
            float alpha = particle.getAlpha();
            if (alpha <= 0.01f) continue;

            Vec3d position = particle.getInterpolatedPosition(tickDelta);
            float particleSize = particle.size * sizeMultiplier;
            outerGlowBatch.render(position, particleSize * OUTER_GLOW_SCALE, particle.rotation,
                    red, green, blue, alpha * OUTER_GLOW_ALPHA);
        }

        Render3DUtils.TexturedBillboardBatch innerGlowBatch = Render3DUtils.additiveTexturedBillboardBatch(context, particleTexture);
        for (SnowParticle particle : particles) {
            float alpha = particle.getAlpha();
            if (alpha <= 0.01f) continue;

            Vec3d position = particle.getInterpolatedPosition(tickDelta);
            float particleSize = particle.size * sizeMultiplier;
            innerGlowBatch.render(position, particleSize * INNER_GLOW_SCALE, particle.rotation,
                    red, green, blue, alpha * INNER_GLOW_ALPHA);
            innerGlowBatch.render(position, particleSize, particle.rotation, 255, 255, 255, alpha);
        }
    }

    private Identifier getSelectedTexture() {
        if (texture.isEnabled("Firefly")) return FIREFLY_TEXTURE;
        if (texture.isEnabled("Star")) return STAR_TEXTURE;
        return GLOW_TEXTURE;
    }

    private static int brighten(int value) {
        return Math.clamp((int) (value + (255 - value) * 0.58f), 0, 255);
    }

    private void updateVisibility() {
        color.setVisible(!themeColor.isEnabled());
    }

    private static class SnowParticle {
        private double x;
        private double y;
        private double z;
        private double prevX;
        private double prevY;
        private double prevZ;
        private final Vec3d velocity;
        private final float size;
        private float rotation;
        private final float spinSpeed;
        private final float maxAlpha;
        private final float wobbleOffset;
        private final long spawnTime;

        private SnowParticle(Vec3d position, Vec3d velocity, float size, float rotation, float spinSpeed, float maxAlpha) {
            this.x = position.x;
            this.y = position.y;
            this.z = position.z;
            this.prevX = position.x;
            this.prevY = position.y;
            this.prevZ = position.z;
            this.velocity = velocity;
            this.size = size;
            this.rotation = rotation;
            this.spinSpeed = spinSpeed;
            this.maxAlpha = maxAlpha;
            this.wobbleOffset = (float) (Math.random() * Math.PI * 2.0);
            this.spawnTime = System.currentTimeMillis();
        }

        private void update() {
            prevX = x;
            prevY = y;
            prevZ = z;

            long age = System.currentTimeMillis() - spawnTime;
            double wobble = Math.sin(age / 420.0 + wobbleOffset) * 0.018;

            x += velocity.x + wobble;
            y += velocity.y;
            z += velocity.z + Math.cos(age / 510.0 + wobbleOffset) * 0.018;
            rotation += spinSpeed;
        }

        private boolean isDead(Vec3d playerPos) {
            long age = System.currentTimeMillis() - spawnTime;
            if (age > MAX_LIFETIME_MILLIS || y < playerPos.y - 3.5) {
                return true;
            }

            double dx = x - playerPos.x;
            double dy = y - playerPos.y;
            double dz = z - playerPos.z;
            return dx * dx + dy * dy + dz * dz > 45.0 * 45.0;
        }

        private Vec3d getInterpolatedPosition(float tickDelta) {
            return new Vec3d(
                    MathHelper.lerp(tickDelta, prevX, x),
                    MathHelper.lerp(tickDelta, prevY, y),
                    MathHelper.lerp(tickDelta, prevZ, z)
            );
        }

        private float getAlpha() {
            long age = System.currentTimeMillis() - spawnTime;
            float fadeIn = Math.clamp(age / 650f, 0f, 1f);
            float fadeOut = Math.clamp((MAX_LIFETIME_MILLIS - age) / 1300f, 0f, 1f);
            return smooth(Math.min(fadeIn, fadeOut)) * maxAlpha;
        }

        private float smooth(float value) {
            value = Math.clamp(value, 0f, 1f);
            return value * value * (3f - 2f * value);
        }
    }
}
