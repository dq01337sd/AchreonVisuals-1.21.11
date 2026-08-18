package ez.minar.system.features.render;

import ez.minar.system.api.Category;
import ez.minar.system.api.Function;
import ez.minar.system.api.NewFunction;
import ez.minar.system.events.EventHandler;
import ez.minar.system.events.impl.AttackEntityEvent;
import ez.minar.system.events.impl.EntityStatusEvent;
import ez.minar.system.menu.ThemeManager;
import ez.minar.system.settings.impl.BooleanSetting;
import ez.minar.system.settings.impl.ColorSetting;
import ez.minar.system.settings.impl.MultiListSetting;
import ez.minar.system.settings.impl.ModeSetting;
import ez.minar.system.settings.impl.NumberSetting;
import net.minecraft.entity.player.PlayerEntity;
import ez.minar.utils.render.Render3DUtils;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.entity.Entity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.awt.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

@NewFunction(name = "Particles", desc = "Картинки-партиклы при ударе по сущности", category = Category.RENDER)
public class Particles extends Function {
    private static final Identifier GLOW_TEXTURE = Identifier.of("atheryx", "images/particles/glow.png");
    private static final Identifier FIREFLY_TEXTURE = Identifier.of("atheryx", "images/particles/firefly.png");
    private static final Identifier STAR_TEXTURE = Identifier.of("atheryx", "images/particles/star.png");
    private static final long LIFE_NANOS = 1_350_000_000L;
    private static final double GRAVITY = -9.8;
    private static final double FLOOR_BOUNCE = 0.32;
    private static final double FLOOR_FRICTION = 0.72;
    private static final float OUTER_GLOW_SCALE = 2.35f;
    private static final float OUTER_GLOW_ALPHA = 0.28f;
    private static final float INNER_GLOW_SCALE = 1.18f;
    private static final float INNER_GLOW_ALPHA = 0.72f;
    private static final byte TOTEM_POP_STATUS = 35;
    private static final int TOTEM_PARTICLE_COUNT = 30;
    private static final List<HitParticle> PARTICLES = new ArrayList<>();
    private static final Random RANDOM = new Random();

    public static Particles Instance;
    public final ModeSetting texture = new ModeSetting("Текстура", "Glow", "Firefly", "Star");

    public final MultiListSetting triggers = new MultiListSetting("Триггеры", "При ударе", "При тотеме");
    public final NumberSetting amount = new NumberSetting("Количество", 12, 1, 60, 1);
    public final NumberSetting speed = new NumberSetting("Скорость", 1.0, 0.1, 4.0, 0.1);
    public final NumberSetting spread = new NumberSetting("Сила разлёта", 1.0, 0.1, 4.0, 0.1);
    public final NumberSetting size = new NumberSetting("Размер", 1.0, 0.3, 3.0, 0.1);
    public final BooleanSetting themeColor = new BooleanSetting("Цвет от темы", true);
    public final ColorSetting color = new ColorSetting("Цвет", new Color(255, 255, 255));

    public Particles() {
        Instance = this;
        addSettings(triggers, amount, speed, spread, size, texture, themeColor, color);
        themeColor.runnable(this::updateVisibility);
        updateVisibility();
    }

    @Override
    public void onDisable() {
        synchronized (PARTICLES) {
            PARTICLES.clear();
        }
    }

    @EventHandler
    private void onAttack(AttackEntityEvent event) {
        if (!triggers.isEnabled("При ударе")) return;
        if (mc.player == null || event.getPlayer() != mc.player) return;
        spawn(event.getTarget(), false);
    }

    @EventHandler
    private void onEntityStatus(EntityStatusEvent event) {
        if (!triggers.isEnabled("При тотеме")) return;
        if (event.getPacket().getStatus() != TOTEM_POP_STATUS) return;
        if (!(event.getEntity() instanceof PlayerEntity)) return;
        spawn(event.getEntity(), true);
    }

    private void spawn(Entity target, boolean isTotem) {
        Vec3d center = target.getEntityPos().add(0.0, target.getHeight() * 0.55, 0.0);
        int count = isTotem ? TOTEM_PARTICLE_COUNT : (int) amount.getValue();
        float speedValue = (float) speed.getValue();
        float spreadValue = (float) spread.getValue();
        float sizeValue = (float) size.getValue();
        long now = System.nanoTime();

        synchronized (PARTICLES) {
            for (int i = 0; i < count; i++) {
                Vec3d direction = randomDirection();
                double impulse = (1.15 + RANDOM.nextDouble() * 1.55) * speedValue * spreadValue;
                Vec3d velocity = direction.multiply(impulse).add(0.0, RANDOM.nextDouble() * 0.25 * speedValue, 0.0);

                float size = (0.13f + RANDOM.nextFloat() * 0.08f) * sizeValue;
                float spinSpeed = -220f + RANDOM.nextFloat() * 440f;
                boolean useGreen = isTotem && RANDOM.nextBoolean();
                PARTICLES.add(new HitParticle(center, velocity, size, RANDOM.nextFloat() * 360f, spinSpeed, now, useGreen));
            }
        }
    }

    private Vec3d randomDirection() {
        double x = RANDOM.nextDouble() * 2.0 - 1.0;
        double y = RANDOM.nextDouble() * 1.8 - 0.75;
        double z = RANDOM.nextDouble() * 2.0 - 1.0;
        Vec3d direction = new Vec3d(x, y, z);

        if (direction.lengthSquared() < 0.001) {
            return new Vec3d(1.0, 0.0, 0.0);
        }

        return direction.normalize();
    }

    public static void renderWorld(WorldRenderContext context) {
        if (Instance == null || !Instance.isEnabled()) return;

        long now = System.nanoTime();
        List<HitParticle> particles;

        synchronized (PARTICLES) {
            Iterator<HitParticle> iterator = PARTICLES.iterator();
            while (iterator.hasNext()) {
                HitParticle particle = iterator.next();
                float age = (now - particle.spawnedAt) / (float) LIFE_NANOS;

                if (age >= 1f) {
                    iterator.remove();
                    continue;
                }

                particle.update(now);
            }

            if (PARTICLES.isEmpty()) return;
            particles = new ArrayList<>(PARTICLES);
        }

        Color selectedColor = Instance.themeColor.isEnabled() ? ThemeManager.getThemeColor() : Instance.color.getColor();
        boolean useTheme = Instance.themeColor.isEnabled();
        int red = useTheme ? brighten(selectedColor.getRed()) : selectedColor.getRed();
        int green = useTheme ? brighten(selectedColor.getGreen()) : selectedColor.getGreen();
        int blue = useTheme ? brighten(selectedColor.getBlue()) : selectedColor.getBlue();


        if (isGlowTextureSelected()) {
            Identifier particleTexture = getSelectedTexture();
            Render3DUtils.TexturedBillboardBatch outerGlowBatch = Render3DUtils.additiveTexturedBillboardBatch(context, particleTexture);
            for (HitParticle particle : particles) {
                float age = (now - particle.spawnedAt) / (float) LIFE_NANOS;
                int[] colors = particle.useGreen ? getGreenColor() : getParticleColor(red, green, blue);
                renderParticleOuterGlow(outerGlowBatch, particle, age, colors[0], colors[1], colors[2]);
            }

            Render3DUtils.TexturedBillboardBatch innerGlowBatch = Render3DUtils.additiveTexturedBillboardBatch(context, particleTexture);
            for (HitParticle particle : particles) {
                float age = (now - particle.spawnedAt) / (float) LIFE_NANOS;
                int[] colors = particle.useGreen ? getGreenColor() : getParticleColor(red, green, blue);
                renderParticleInnerGlow(innerGlowBatch, particle, age, colors[0], colors[1], colors[2]);
            }
            return;
        }

        Identifier particleTexture = getSelectedTexture();
        Render3DUtils.TexturedBillboardBatch batch = Render3DUtils.additiveTexturedBillboardBatch(context, particleTexture);
        for (HitParticle particle : particles) {
            float age = (now - particle.spawnedAt) / (float) LIFE_NANOS;
            int[] colors = particle.useGreen ? getGreenColor() : getParticleColor(red, green, blue);
            renderParticleCompact(batch, particle, age, colors[0], colors[1], colors[2]);
        }
    }

    private static int[] getParticleColor(int red, int green, int blue) {
        return new int[]{red, green, blue};
    }

    private static int[] getGreenColor() {
        boolean useYellow = RANDOM.nextBoolean();
        if (useYellow) {
            return new int[]{255, 255, 0};
        } else {
            return new int[]{0, 255, 0};
        }
    }

    private static Identifier getSelectedTexture() {
        if (Instance != null && Instance.texture.isEnabled("Firefly")) return FIREFLY_TEXTURE;
        if (Instance != null && Instance.texture.isEnabled("Star")) return STAR_TEXTURE;
        return GLOW_TEXTURE;
    }

    private static boolean isGlowTextureSelected() {
        return Instance == null || Instance.texture.isEnabled("Glow");
    }

    private static void renderParticleOuterGlow(Render3DUtils.TexturedBillboardBatch batch, HitParticle particle, float age,
                                                int red, int green, int blue) {
        float alpha = particleAlpha(age);
        if (alpha <= 0.01f) return;

        batch.render(particle.position, particleSize(particle, age) * OUTER_GLOW_SCALE, particle.rotation,
                red, green, blue, alpha * OUTER_GLOW_ALPHA);
    }

    private static void renderParticleInnerGlow(Render3DUtils.TexturedBillboardBatch batch, HitParticle particle, float age,
                                                int red, int green, int blue) {
        float alpha = particleAlpha(age);
        if (alpha <= 0.01f) return;

        float size = particleSize(particle, age);
        batch.render(particle.position, size * INNER_GLOW_SCALE, particle.rotation, red, green, blue,
                Math.clamp(alpha * INNER_GLOW_ALPHA, 0f, 1f));
        batch.render(particle.position, size, particle.rotation, core(red), core(green), core(blue),
                Math.clamp(alpha * 1.25f, 0f, 1f));
    }

    private static void renderParticleCompact(Render3DUtils.TexturedBillboardBatch batch, HitParticle particle, float age,
                                              int red, int green, int blue) {
        float alpha = particleAlpha(age);
        if (alpha <= 0.01f) return;

        float size = particleSize(particle, age);
        batch.render(particle.position, size * INNER_GLOW_SCALE, particle.rotation, red, green, blue,
                Math.clamp(alpha * INNER_GLOW_ALPHA, 0f, 1f));
        batch.render(particle.position, size, particle.rotation, core(red), core(green), core(blue),
                Math.clamp(alpha * 1.25f, 0f, 1f));
    }

    private static float particleSize(HitParticle particle, float age) {
        float eased = 1f - (1f - age) * (1f - age);
        float alpha = particleAlpha(age);

        return particle.size * (0.65f + eased * 0.55f) * (0.18f + alpha * 0.82f);
    }

    private static float particleAlpha(float age) {
        float disappear = Math.clamp((1f - age) / 0.28f, 0f, 1f);
        return smooth(disappear);
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

    private static class HitParticle {
        private Vec3d position;
        private Vec3d velocity;
        private final float size;
        private float rotation;
        private final float spinSpeed;
        private final long spawnedAt;
        private long lastUpdate;
        private final boolean useGreen;

        private HitParticle(Vec3d position, Vec3d velocity, float size, float rotation, float spinSpeed, long spawnedAt, boolean useGreen) {
            this.position = position;
            this.velocity = velocity;
            this.size = size;
            this.rotation = rotation;
            this.spinSpeed = spinSpeed;
            this.spawnedAt = spawnedAt;
            this.lastUpdate = spawnedAt;
            this.useGreen = useGreen;
        }

        private void update(long now) {
            if (Instance == null || Instance.mc.world == null) return;

            double delta = Math.min((now - lastUpdate) / 1_000_000_000.0, 0.05);
            lastUpdate = now;

            velocity = velocity.add(0.0, GRAVITY * delta, 0.0);
            Vec3d next = position.add(velocity.multiply(delta));
            BlockPos floorPos = BlockPos.ofFloored(next.x, next.y - size * 0.45f, next.z);

            if (!Instance.mc.world.getBlockState(floorPos).isAir() && velocity.y < 0.0) {
                next = new Vec3d(next.x, floorPos.getY() + 1.0 + size * 0.45f, next.z);
                velocity = new Vec3d(
                        velocity.x * FLOOR_FRICTION,
                        Math.abs(velocity.y) < 0.55 ? 0.0 : -velocity.y * FLOOR_BOUNCE,
                        velocity.z * FLOOR_FRICTION
                );
            }

            if (Math.abs(velocity.y) < 0.02) {
                velocity = new Vec3d(velocity.x * 0.985, velocity.y, velocity.z * 0.985);
            }

            position = next;
            rotation += spinSpeed * (float) delta;
        }
    }
}
