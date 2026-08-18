package ez.minar.system.features.render;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import ez.minar.system.api.Category;
import ez.minar.system.api.Function;
import ez.minar.system.api.NewFunction;
import ez.minar.system.events.EventHandler;
import ez.minar.system.events.impl.UpdateEvent;
import ez.minar.system.menu.ThemeManager;
import ez.minar.system.settings.impl.BooleanSetting;
import ez.minar.system.settings.impl.ColorSetting;
import ez.minar.system.settings.impl.NumberSetting;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.render.LayeringTransform;
import net.minecraft.client.render.OutputTarget;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderSetup;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;

import java.awt.Color;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.Random;

@NewFunction(name = "LineGlyphs", desc = "Длинные случайно поворачивающиеся линии", category = Category.RENDER)
public class LineGlyphs extends Function {
    private static final RenderPipeline LINE_PIPELINE = RenderPipelines.register(
            RenderPipeline.builder(RenderPipelines.RENDERTYPE_LINES_SNIPPET)
                    .withLocation(Identifier.of("minar", "line_glyphs"))
                    .withBlend(BlendFunction.TRANSLUCENT)
                    .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                    .withDepthWrite(false)
                    .build()
    );
    private static final RenderLayer LINE_LAYER = RenderLayer.of("minar_line_glyphs",
            RenderSetup.builder(LINE_PIPELINE)
                    .layeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING)
                    .outputTarget(OutputTarget.ITEM_ENTITY_TARGET)
                    .build());

    public static LineGlyphs Instance;
    private static final float LIFETIME = 20.0f;
    private static final float FADE_DURATION = 2.0f;

    public final NumberSetting count = new NumberSetting("Количество", 40, 5, 120, 1);
    public final NumberSetting speed = new NumberSetting("Скорость", 0.6, 0.2, 5.0, 0.1);
    public final NumberSetting width = new NumberSetting("Толщина", 1.2, 0.8, 6.0, 0.1);
    public final BooleanSetting themeColor = new BooleanSetting("Цвет от темы", true);
    public final ColorSetting color = new ColorSetting("Цвет", new Color(160, 220, 255));

    private final Deque<Glyph> glyphs = new ArrayDeque<>();
    private final Random random = new Random();

    public LineGlyphs() {
        Instance = this;
        addSettings(count, speed, width, themeColor, color);
        themeColor.runnable(this::updateVisibility);
        updateVisibility();
    }

    @Override
    public void onEnable() {
        glyphs.clear();
    }

    @Override
    public void onDisable() {
        glyphs.clear();
    }

    @EventHandler
    public void onUpdate(UpdateEvent event) {
        if (mc.player == null || mc.world == null) return;

        float now = System.currentTimeMillis();
        float step = (float) (speed.getValue() * 0.14);

        Iterator<Glyph> iterator = glyphs.iterator();
        while (iterator.hasNext()) {
            Glyph glyph = iterator.next();
            if ((now - glyph.birthTime) / 1000f >= LIFETIME) {
                iterator.remove();
                continue;
            }
            glyph.update(step);
        }

        int targetCount = (int) count.getValue();
        while (glyphs.size() < targetCount) {
            glyphs.addLast(spawnGlyph(now));
        }
    }

    private Glyph spawnGlyph(float now) {
        Vec3d playerPos = mc.player.getEntityPos();
        double angle = random.nextDouble() * Math.PI * 2.0;
        double distance = 3.0 + random.nextDouble() * 6.0;
        Vec3d origin = playerPos
                .add(Math.cos(angle) * distance, 0.0, Math.sin(angle) * distance)
                .add(0.0, 1.0 + random.nextDouble() * 4.0, 0.0);
        double cardinal = Math.round(random.nextDouble() * 4.0) % 4.0 * (Math.PI / 2.0);
        return new Glyph(origin, cardinal, (long) now);
    }

    public static void renderWorld(WorldRenderContext context) {
        if (Instance == null || !Instance.isEnabled()) return;
        Instance.render(context);
    }

    private void render(WorldRenderContext context) {
        if (mc.player == null || mc.world == null) return;

        Color selected = themeColor.isEnabled() ? ThemeManager.getThemeColor() : color.getColor();
        Vec3d camera = context.worldState().cameraRenderState.pos;
        float lineWidth = (float) width.getValue();
        VertexConsumer buffer = context.consumers().getBuffer(LINE_LAYER);
        MatrixStack.Entry entry = context.matrices().peek();

        for (Glyph glyph : glyphs) {
            float age = (System.currentTimeMillis() - glyph.birthTime) / 1000f;
            float remaining = LIFETIME - age;
            float fade = remaining < FADE_DURATION ? Math.max(0f, remaining / FADE_DURATION) : 1f;
            glyph.drawPath(buffer, entry, camera, selected, lineWidth, fade);
        }
    }

    private void updateVisibility() {
        color.setVisible(!themeColor.isEnabled());
    }

    private class Glyph {
        private Vec3d pos;
        private double yaw;
        private int turnTimer;
        private boolean rising;
        private double targetY = -1.0;
        private final long birthTime;
        private final Deque<Vec3d> path = new ArrayDeque<>();
        private static final int MAX_PATH = 36;
        private static final float MAX_RADIUS = 14f;

        private Glyph(Vec3d pos, double yaw, long birthTime) {
            this.pos = pos;
            this.yaw = yaw;
            this.turnTimer = 0;
            this.birthTime = birthTime;
            this.path.addLast(pos);
        }

        private void update(float step) {
            if (rising) {
                double groundY = groundTop(pos.x, pos.z);
                double nextY = pos.y + step * 0.5;
                if (nextY >= targetY) {
                    nextY = targetY;
                    rising = false;
                }
                if (nextY < groundY) {
                    nextY = groundY;
                }
                pos = new Vec3d(pos.x, nextY, pos.z);
                path.addLast(pos);
                if (path.size() > MAX_PATH) {
                    path.removeFirst();
                }
                return;
            }

            if (turnTimer <= 0) {
                turnTimer = 60;
                chooseTurn();
                if (random.nextDouble() < 0.3) {
                    rising = true;
                    targetY = pos.y + 0.3 + random.nextDouble() * 0.7;
                }
            }
            turnTimer--;

            Vec3d playerPos = mc.player.getEntityPos();
            double dist = Math.hypot(pos.x - playerPos.x, pos.z - playerPos.z);
            if (dist > MAX_RADIUS) {
                yaw = snapCardinal(Math.atan2(playerPos.z - pos.z, playerPos.x - pos.x));
                rising = false;
                turnTimer = 60;
            }

            pos = new Vec3d(pos.x + Math.cos(yaw) * step, pos.y, pos.z + Math.sin(yaw) * step);
            path.addLast(pos);
            if (path.size() > MAX_PATH) {
                path.removeFirst();
            }
        }

        private void chooseTurn() {
            if (random.nextBoolean()) {
                yaw += Math.PI / 2.0;
            } else {
                yaw -= Math.PI / 2.0;
            }
        }

        private double snapCardinal(double angle) {
            return Math.round(angle / (Math.PI / 2.0)) * (Math.PI / 2.0);
        }

        private double groundTop(double x, double z) {
            if (mc.world == null) {
                return pos.y;
            }
            int top = mc.world.getTopY(net.minecraft.world.Heightmap.Type.WORLD_SURFACE, (int) Math.floor(x), (int) Math.floor(z));
            return top + 0.05;
        }

        private void drawPath(VertexConsumer buffer, MatrixStack.Entry entry, Vec3d camera, Color color, float width, float fade) {
            Vec3d prev = null;
            Iterator<Vec3d> iterator = path.iterator();
            while (iterator.hasNext()) {
                Vec3d point = iterator.next();
                if (prev != null) {
                    Vec3d a = prev.subtract(camera);
                    Vec3d b = point.subtract(camera);
                    line(buffer, entry, color, a.x, a.y, a.z, b.x, b.y, b.z, width, fade);
                }
                prev = point;
            }
        }

        private void line(VertexConsumer buffer, MatrixStack.Entry entry, Color color,
                          double x1, double y1, double z1, double x2, double y2, double z2, float width, float fade) {
            float dx = (float) (x2 - x1);
            float dy = (float) (y2 - y1);
            float dz = (float) (z2 - z1);
            float length = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (length <= 0.0001F) return;

            int alpha = (int) (255 * Math.clamp(fade, 0f, 1f));
            buffer.vertex(entry, (float) x1, (float) y1, (float) z1)
                    .color(color.getRed(), color.getGreen(), color.getBlue(), alpha)
                    .normal(entry, dx / length, dy / length, dz / length)
                    .lineWidth(width);
            buffer.vertex(entry, (float) x2, (float) y2, (float) z2)
                    .color(color.getRed(), color.getGreen(), color.getBlue(), alpha)
                    .normal(entry, dx / length, dy / length, dz / length)
                    .lineWidth(width);
        }
    }
}