package ez.minar.system.features.render;

import ez.minar.system.api.Category;
import ez.minar.system.api.Function;
import ez.minar.system.api.NewFunction;
import ez.minar.system.events.EventHandler;
import ez.minar.system.events.impl.Render2DEvent;
import ez.minar.system.menu.ThemeManager;
import ez.minar.system.settings.impl.NumberSetting;
import ez.minar.utils.render.RenderUtil;
import ez.minar.utils.render.pipeline.TexturePipeline;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.opengl.GlConst;
import org.joml.Matrix4f;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

@NewFunction(name = "Arrows", desc = "Показывает стрелочки, указывающие на игроков", category = Category.RENDER)
public class Arrows extends Function {
    private static final Identifier ARROW_TEXTURE = Identifier.of("atheryx", "images/world/arrows.png");
    private static final Identifier GLOW_TEXTURE = Identifier.of("atheryx", "images/particles/glow.png");

    private final NumberSetting radius = new NumberSetting("Radius", 75.0, 30.0, 160.0, 1.0);
    private final NumberSetting size = new NumberSetting("Size", 22.0, 8.0, 50.0, 1.0);

    private final Map<UUID, ArrowState> arrows = new HashMap<>();
    private long lastFrameTime;

    public Arrows() {
        addSettings(radius, size);
    }

    @Override
    public void onDisable() {
        arrows.clear();
        lastFrameTime = 0L;
    }

    @EventHandler
    public void onRender2D(Render2DEvent event) {
        DrawContext context = event.getContext();

        if (context == null || mc.player == null || mc.world == null) return;
        if (mc.options.hudHidden || mc.getDebugHud().shouldShowDebugHud()) return;

        long now = System.currentTimeMillis();
        float delta = lastFrameTime == 0L ? 0f : Math.min(50f, now - lastFrameTime);
        lastFrameTime = now;

        float centerX = RenderUtil.getFixedScaledWidth() / 2f;
        float centerY = RenderUtil.getFixedScaledHeight() / 2f;
        float tickDelta = mc.getRenderTickCounter().getTickProgress(false);

        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player == mc.player || !player.isAlive()) continue;

            UUID uuid = player.getUuid();
            ArrowState state = arrows.computeIfAbsent(uuid, ignored -> new ArrowState());
            state.target = player;
            state.visible = true;
        }

        Iterator<Map.Entry<UUID, ArrowState>> iterator = arrows.entrySet().iterator();

        while (iterator.hasNext()) {
            ArrowState state = iterator.next().getValue();

            state.progress = animate(state.progress, state.visible ? 1f : 0f, 0.018f, delta);

            if (state.target != null && state.progress > 0.01f) {
                renderArrow(state.target, tickDelta, centerX, centerY, easeOutBack(state.progress), state.progress);
            }

            state.visible = false;

            if (state.progress <= 0.001f) {
                iterator.remove();
            }
        }
    }

    private void renderArrow(PlayerEntity target, float tickDelta, float centerX, float centerY, float scaleProgress, float alphaProgress) {
        Vec3d targetPos = target.getLerpedPos(tickDelta);
        Vec3d selfPos = mc.player.getLerpedPos(tickDelta);

        double dx = targetPos.x - selfPos.x;
        double dz = targetPos.z - selfPos.z;

        if (dx * dx + dz * dz < 0.01) return;

        float worldAngle = (float) Math.atan2(dz, dx);
        float playerAngle = (float) Math.toRadians(mc.player.getYaw()) + (float) Math.PI / 2f;
        float angle = worldAngle - playerAngle;

        float r = (float) radius.getValue();
        float s = (float) size.getValue() * scaleProgress;

        float x = centerX + (float) Math.sin(angle) * r;
        float y = centerY - (float) Math.cos(angle) * r;

        drawArrowTexture(x, y, angle, s, alphaProgress);
    }

    private void drawArrowTexture(float centerX, float centerY, float angle, float size, float alpha) {
        var texture = mc.getTextureManager().getTexture(ARROW_TEXTURE).getGlTextureView();
        var glowTexture = mc.getTextureManager().getTexture(GLOW_TEXTURE).getGlTextureView();

        GlStateManager._enableBlend();

// SRC_ALPHA, ONE
        GlStateManager._blendFuncSeparate(
                GlConst.GL_SRC_ALPHA,
                GlConst.GL_ONE,
                GlConst.GL_ONE,
                GlConst.GL_ZERO
        );

        int red = brightenLikeParticles(ThemeManager.getThemeColor().getRed());
        int green = brightenLikeParticles(ThemeManager.getThemeColor().getGreen());
        int blue = brightenLikeParticles(ThemeManager.getThemeColor().getBlue());

        Matrix4f glowMatrix = RenderUtil.createProjection();
        glowMatrix.translate(centerX, centerY, 0f);
        glowMatrix.rotateZ(-angle * 0.85f);
        float glowSize = size * 1.85f;
        glowMatrix.translate(-glowSize / 2f, -glowSize / 2f, 0f);

        Matrix4f matrix = RenderUtil.createProjection();
        matrix.translate(centerX, centerY, 0f);
        matrix.rotateZ(angle);
        matrix.translate(-size / 2f, -size / 2f, 0f);

        TexturePipeline.draw(
                matrix,
                0f,
                0f,
                size,
                texture,
                argb(
                        255f * alpha,
                        red,
                        green,
                        blue
                ),
                0f,
                0f
        );

// вернуть обычный режим майна
        GlStateManager._blendFuncSeparate(
                GlConst.GL_SRC_ALPHA,
                GlConst.GL_ONE_MINUS_SRC_ALPHA,
                GlConst.GL_ONE,
                GlConst.GL_ZERO
        );
    }

    private float animate(float current, float target, float speed, float delta) {
        float factor = 1f - (float) Math.exp(-speed * delta);
        float next = current + (target - current) * Math.clamp(factor, 0f, 1f);
        return Math.abs(next - target) < 0.001f ? target : next;
    }

    private float easeOutBack(float progress) {
        float c1 = 1.70158f;
        float c3 = c1 + 1f;
        float t = Math.clamp(progress, 0f, 1f) - 1f;
        return 1f + c3 * t * t * t + c1 * t * t;
    }

    private int argb(float alpha, int red, int green, int blue) {
        int a = Math.clamp((int) alpha, 0, 255);
        return a << 24 | red << 16 | green << 8 | blue;
    }

    private int brightenLikeParticles(int value) {
        return Math.clamp((int) (value + (255 - value) * 0.45f), 0, 255);
    }

    private static class ArrowState {
        private PlayerEntity target;
        private boolean visible;
        private float progress;
    }
}