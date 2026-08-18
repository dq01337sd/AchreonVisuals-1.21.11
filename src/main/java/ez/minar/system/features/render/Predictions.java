package ez.minar.system.features.render;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import ez.minar.system.api.Category;
import ez.minar.system.api.Function;
import ez.minar.system.api.NewFunction;
import ez.minar.system.menu.ThemeManager;
import ez.minar.system.settings.impl.BooleanSetting;
import ez.minar.system.settings.impl.ColorSetting;
import ez.minar.system.settings.impl.NumberSetting;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gl.UniformType;
import net.minecraft.client.render.LayeringTransform;
import net.minecraft.client.render.OutputTarget;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderSetup;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.projectile.AbstractFireballEntity;
import net.minecraft.entity.projectile.AbstractWindChargeEntity;
import net.minecraft.entity.projectile.FireworkRocketEntity;
import net.minecraft.entity.projectile.FishingBobberEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.entity.projectile.TridentEntity;
import net.minecraft.entity.projectile.thrown.ThrownEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.item.Items;
import net.minecraft.util.math.MathHelper;
import net.minecraft.client.render.LightmapTextureManager;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

@NewFunction(name = "Predictions", desc = "Показывает шейдерную траекторию полета снарядов", category = Category.RENDER)
public class Predictions extends Function {
    private static final int MIN_POINTS = 2;
    private static final double MIN_VELOCITY_SQ = 0.0004;

    private static final RenderPipeline LINE_PIPELINE = RenderPipelines.register(
            RenderPipeline.builder(RenderPipelines.RENDERTYPE_LINES_SNIPPET)
                    .withLocation(Identifier.of("atheryx", "predictions_lines"))
                    .withBlend(BlendFunction.TRANSLUCENT)
                    .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                    .withDepthWrite(false)
                    .build()
    );
    private static final RenderLayer LINE_LAYER = RenderLayer.of("atheryx_predictions_lines",
            RenderSetup.builder(LINE_PIPELINE)
                    .layeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING)
                    .outputTarget(OutputTarget.ITEM_ENTITY_TARGET)
                    .build());

    public static Predictions Instance;
    private static final double ANIMATION_SPEED = 14.0;
    private java.util.List<net.minecraft.util.math.Box> animatedBoxes = new java.util.ArrayList<>();
    private long lastFrameTime;

    private final NumberSetting lineWidth = new NumberSetting("Line Width", 2.2, 0.8, 6.0, 0.1);
    private final NumberSetting maxTicks = new NumberSetting("Prediction Ticks", 80.0, 10.0, 180.0, 5.0);
    private final BooleanSetting onlyMine = new BooleanSetting("Only Mine", true);
    private final BooleanSetting themeColor = new BooleanSetting("Theme Color", true);
    private final ColorSetting color = new ColorSetting("Color", new Color(120, 210, 255));

    public Predictions() {
        Instance = this;
        addSettings(lineWidth, maxTicks, onlyMine, themeColor, color);
        themeColor.runnable(this::updateVisibility);
    }

    @Override
    public void onDisable() {
        animatedBoxes.clear();
        lastFrameTime = 0L;
    }

    public static void renderWorld(WorldRenderContext context) {
        if (Instance == null || !Instance.isEnabled()) return;
        Instance.render(context);
    }

    private void render(WorldRenderContext context) {
        if (mc.world == null || mc.player == null) return;

        Color selectedColor = themeColor.isEnabled() ? ThemeManager.getThemeColor() : color.getColor();
        Vec3d camera = context.worldState().cameraRenderState.pos;
        float tickDelta = mc.getRenderTickCounter().getTickProgress(false);

        labelsToRender.clear();

        boolean holdingPearl = mc.player.getMainHandStack().getItem() == net.minecraft.item.Items.ENDER_PEARL || mc.player.getOffHandStack().getItem() == net.minecraft.item.Items.ENDER_PEARL;
        boolean holdingBow = mc.player.getMainHandStack().getItem() instanceof net.minecraft.item.BowItem || mc.player.getOffHandStack().getItem() instanceof net.minecraft.item.BowItem;
        boolean holdingCrossbow = mc.player.getMainHandStack().getItem() instanceof net.minecraft.item.CrossbowItem || mc.player.getOffHandStack().getItem() instanceof net.minecraft.item.CrossbowItem;
        
        java.util.List<net.minecraft.util.math.Box> targetBoxes = new java.util.ArrayList<>();

        if (holdingPearl || holdingBow || holdingCrossbow) {
            float pitch = mc.player.getPitch();
            float yaw = mc.player.getYaw();

            float power = 1.5f;
            double gravity = 0.03;
            boolean multishot = false;

            if (holdingBow) {
                gravity = 0.05;
                if (mc.player.isUsingItem() && mc.player.getActiveItem().getItem() instanceof net.minecraft.item.BowItem) {
                    int useTime = mc.player.getItemUseTime();
                    float pull = net.minecraft.item.BowItem.getPullProgress(useTime);
                    power = pull * 3.0f;
                    if (power < 0.15f) power = 0.15f;
                } else {
                    power = 3.0f;
                }
            } else if (holdingCrossbow) {
                gravity = 0.05;
                power = 3.15f;
                net.minecraft.item.ItemStack holdingStack = mc.player.getMainHandStack().getItem() instanceof net.minecraft.item.CrossbowItem ? mc.player.getMainHandStack() : mc.player.getOffHandStack();
                if (holdingStack != null && holdingStack.getEnchantments().toString().contains("multishot")) {
                    multishot = true;
                }
            }

            java.util.List<Vec3d> velocities = new java.util.ArrayList<>();
            if (multishot) {
                velocities.add(getVelocityVector(yaw - 10, pitch).multiply(power));
                velocities.add(getVelocityVector(yaw, pitch).multiply(power));
                velocities.add(getVelocityVector(yaw + 10, pitch).multiply(power));
            } else {
                velocities.add(getVelocityVector(yaw, pitch).multiply(power));
            }

            double eyeHeightOffset = mc.player.getEyePos().y - mc.player.getY();
            Vec3d realPosition = mc.player.getLerpedPos(tickDelta).add(0, eyeHeightOffset - 0.1, 0);

            for (Vec3d velocity : velocities) {
                SimulationResult result = simulatePath(realPosition, velocity, null, 0.99, gravity, mc.player);
                if (result.points.size() >= MIN_POINTS && result.hitResult != null) {
                    net.minecraft.util.math.Box box = getHitBox(result.hitResult);
                    if (box != null) targetBoxes.add(box);
                }
            }
        }

        for (Entity entity : mc.world.getEntities()) {
            if (!(entity instanceof ProjectileEntity projectile) || !shouldPredict(projectile)) continue;

            SimulationResult result = simulate(projectile, tickDelta);
            List<Vec3d> points = result.points;
            if (points.size() < MIN_POINTS) continue;

            if (result.hitResult != null) {
                net.minecraft.util.math.Box box = getHitBox(result.hitResult);
                if (box != null) targetBoxes.add(box);
            }

            Vec3d endPos = points.get(points.size() - 1);
            int ticksToLand = points.size() - 1;
            double seconds = ticksToLand / 20.0;
            String timeStr = String.format("%.1f s", seconds);
            labelsToRender.add(new LabelData(endPos, getProjectileStack(projectile), timeStr));
        }

        updateAnimatedBoxes(targetBoxes);
        for (net.minecraft.util.math.Box renderBox : animatedBoxes) {
            net.minecraft.util.math.Box cameraBox = renderBox.offset(-camera.x, -camera.y, -camera.z);
            drawBoxOutline(context.consumers().getBuffer(LINE_LAYER), context.matrices().peek(), cameraBox, selectedColor);
        }
    }

    private Vec3d getVelocityVector(float yaw, float pitch) {
        float f = -net.minecraft.util.math.MathHelper.sin(yaw * 0.017453292F) * net.minecraft.util.math.MathHelper.cos(pitch * 0.017453292F);
        float g = -net.minecraft.util.math.MathHelper.sin(pitch * 0.017453292F);
        float h = net.minecraft.util.math.MathHelper.cos(yaw * 0.017453292F) * net.minecraft.util.math.MathHelper.cos(pitch * 0.017453292F);
        return new Vec3d(f, g, h).normalize();
    }

    private void renderPath(List<Vec3d> points, WorldRenderContext context, Vec3d camera, Color selectedColor) {
        drawLinePath(context.consumers().getBuffer(LINE_LAYER), context.matrices().peek(), points, camera, selectedColor);
    }

    private static class LabelData {
        Vec3d pos;
        net.minecraft.item.ItemStack stack;
        String timeStr;
        LabelData(Vec3d pos, net.minecraft.item.ItemStack stack, String timeStr) {
            this.pos = pos;
            this.stack = stack;
            this.timeStr = timeStr;
        }
    }

    private final java.util.List<LabelData> labelsToRender = new java.util.ArrayList<>();

    @ez.minar.system.events.EventHandler
    public void onRender2D(ez.minar.system.events.impl.Render2DEvent event) {
        if (mc.world == null || mc.player == null) return;
        net.minecraft.client.gui.DrawContext context = event.getContext();

        for (LabelData label : labelsToRender) {
            float[] screen = ez.minar.utils.render.WorldToScreen.project(label.pos);
            if (screen == null) continue;

            String text = label.stack.getName().getString() + " " + label.timeStr;
            float textW = ez.minar.utils.render.msdf.Msdf.width(ez.minar.utils.render.msdf.Msdf.SF_BOLD, text, 7.5f);
            float textH = ez.minar.utils.render.msdf.Msdf.height(ez.minar.utils.render.msdf.Msdf.SF_BOLD, 7.5f);

            float paddingX = 5f;
            float paddingY = 4f;

            float totalW = textW + paddingX * 2;
            float totalH = textH + paddingY * 2;

            float startX = screen[0] - totalW / 2f;
            float startY = screen[1] - totalH / 2f;

            ez.minar.utils.render.RenderUtil.hudBlur(startX, startY, totalW, totalH, 3f, 25f, 1f, new java.awt.Color(4, 4, 6, 180));

            float textX = startX + paddingX;
            float textY = startY + paddingY - 0.5f;

            ez.minar.utils.render.RenderUtil.text(context, ez.minar.utils.render.msdf.Msdf.SF_BOLD, textX, textY, text, 7.5f, new java.awt.Color(230, 230, 235));
        }
    }

    private net.minecraft.util.math.Box getHitBox(net.minecraft.util.hit.HitResult hitResult) {
        if (hitResult instanceof net.minecraft.util.hit.BlockHitResult blockHit) {
            net.minecraft.util.math.BlockPos pos = blockHit.getBlockPos();
            if (mc.world != null) {
                net.minecraft.util.shape.VoxelShape shape = mc.world.getBlockState(pos).getOutlineShape(mc.world, pos);
                if (!shape.isEmpty()) {
                    return shape.getBoundingBox().offset(pos);
                }
            }
        } else if (hitResult instanceof net.minecraft.util.hit.EntityHitResult entityHit) {
            Entity entity = entityHit.getEntity();
            float tickDelta = mc.getRenderTickCounter().getTickProgress(false);
            Vec3d lerped = entity.getLerpedPos(tickDelta);
            double w = entity.getWidth() / 2.0;
            return new net.minecraft.util.math.Box(lerped.x - w, lerped.y, lerped.z - w, lerped.x + w, lerped.y + entity.getHeight(), lerped.z + w);
        }
        return null;
    }

    private void updateAnimatedBoxes(java.util.List<net.minecraft.util.math.Box> targetBoxes) {
        long now = System.nanoTime();
        double delta = lastFrameTime == 0L ? 1.0 : Math.min((now - lastFrameTime) / 1_000_000_000.0, 0.05);
        lastFrameTime = now;

        double progress = 1.0 - Math.exp(-ANIMATION_SPEED * delta);

        if (targetBoxes.isEmpty()) {
            for (int i = 0; i < animatedBoxes.size(); i++) {
                net.minecraft.util.math.Box b = animatedBoxes.get(i);
                Vec3d center = b.getCenter();
                net.minecraft.util.math.Box zeroBox = new net.minecraft.util.math.Box(center, center);
                net.minecraft.util.math.Box newB = new net.minecraft.util.math.Box(
                        lerp(b.minX, zeroBox.minX, progress),
                        lerp(b.minY, zeroBox.minY, progress),
                        lerp(b.minZ, zeroBox.minZ, progress),
                        lerp(b.maxX, zeroBox.maxX, progress),
                        lerp(b.maxY, zeroBox.maxY, progress),
                        lerp(b.maxZ, zeroBox.maxZ, progress)
                );
                animatedBoxes.set(i, newB);
            }
            animatedBoxes.removeIf(b -> b.maxX - b.minX < 0.01);
            if (animatedBoxes.isEmpty()) lastFrameTime = 0L;
            return;
        }

        while (animatedBoxes.size() < targetBoxes.size()) {
            Vec3d center = targetBoxes.get(animatedBoxes.size()).getCenter();
            animatedBoxes.add(new net.minecraft.util.math.Box(center, center));
        }
        while (animatedBoxes.size() > targetBoxes.size()) {
            animatedBoxes.remove(animatedBoxes.size() - 1);
        }

        for (int i = 0; i < targetBoxes.size(); i++) {
            net.minecraft.util.math.Box targetBox = targetBoxes.get(i);
            net.minecraft.util.math.Box animatedBox = animatedBoxes.get(i);
            net.minecraft.util.math.Box newB = new net.minecraft.util.math.Box(
                    lerp(animatedBox.minX, targetBox.minX, progress),
                    lerp(animatedBox.minY, targetBox.minY, progress),
                    lerp(animatedBox.minZ, targetBox.minZ, progress),
                    lerp(animatedBox.maxX, targetBox.maxX, progress),
                    lerp(animatedBox.maxY, targetBox.maxY, progress),
                    lerp(animatedBox.maxZ, targetBox.maxZ, progress)
            );
            animatedBoxes.set(i, newB);
        }
    }

    private double lerp(double from, double to, double progress) {
        return from + (to - from) * progress;
    }

    private net.minecraft.item.ItemStack getProjectileStack(ProjectileEntity projectile) {
        if (projectile instanceof net.minecraft.entity.projectile.thrown.ThrownItemEntity thrownItem) {
            return thrownItem.getStack();
        } else if (projectile instanceof TridentEntity) {
            return new net.minecraft.item.ItemStack(Items.TRIDENT);
        } else if (projectile instanceof PersistentProjectileEntity) {
            return new net.minecraft.item.ItemStack(Items.ARROW);
        }
        return new net.minecraft.item.ItemStack(Items.ENDER_PEARL);
    }

    private boolean shouldPredict(ProjectileEntity projectile) {
        if (!projectile.isAlive() || projectile.getVelocity().lengthSquared() < MIN_VELOCITY_SQ) return false;
        if (onlyMine.isEnabled() && projectile.getOwner() != mc.player) return false;
        if (projectile instanceof FishingBobberEntity) return false;
        if (projectile instanceof PersistentProjectileEntity persistent && !persistent.canHit()) return false;
        return projectile instanceof ThrownEntity
                || projectile instanceof PersistentProjectileEntity
                || projectile instanceof TridentEntity
                || projectile instanceof FireworkRocketEntity
                || projectile instanceof AbstractFireballEntity
                || projectile instanceof AbstractWindChargeEntity;
    }

    private SimulationResult simulatePath(Vec3d position, Vec3d velocity, ProjectileEntity projectile, double defaultDrag, double gravity, Entity entityForRaycast) {
        List<Vec3d> points = new ArrayList<>();
        points.add(position);
        HitResult finalHit = null;

        int steps = (int) maxTicks.getValue();
        for (int i = 0; i < steps; i++) {
            if (velocity.lengthSquared() < MIN_VELOCITY_SQ) break;

            Vec3d next = position.add(velocity);
            HitResult hit = mc.world.getCollisionsIncludingWorldBorder(new RaycastContext(

                    position,
                    next,
                    RaycastContext.ShapeType.COLLIDER,
                    RaycastContext.FluidHandling.NONE,
                    entityForRaycast
            ));

            double minDistanceSq = hit.getType() != HitResult.Type.MISS ? position.squaredDistanceTo(hit.getPos()) : Double.MAX_VALUE;
            net.minecraft.util.hit.EntityHitResult entityHit = null;
            Box box = new Box(position, next).expand(1.0);
            for (Entity entity : mc.world.getOtherEntities(entityForRaycast, box)) {
                if (!entity.canHit() && !(entity instanceof net.minecraft.entity.LivingEntity)) continue;
                Box entityBox = entity.getBoundingBox().expand(0.3);
                java.util.Optional<Vec3d> entityHitPos = entityBox.raycast(position, next);
                if (entityHitPos.isPresent()) {
                    double distSq = position.squaredDistanceTo(entityHitPos.get());
                    if (distSq < minDistanceSq) {
                        minDistanceSq = distSq;
                        entityHit = new net.minecraft.util.hit.EntityHitResult(entity, entityHitPos.get());
                    }
                }
            }
            if (entityHit != null) {
                hit = entityHit;
            }

            if (hit != null && hit.getType() != HitResult.Type.MISS) {
                points.add(hit.getPos());
                finalHit = hit;
                break;
            }

            points.add(next);
            position = next;

            double drag;
            if (projectile != null) {
                drag = getDrag(projectile, position);
            } else {
                boolean inFluid = !mc.world.getFluidState(BlockPos.ofFloored(position)).isEmpty();
                drag = inFluid ? 0.8 : defaultDrag;
            }

            velocity = velocity.multiply(drag);
            if (gravity > 0.0) {
                velocity = velocity.add(0.0, -gravity, 0.0);
            }
        }

        return new SimulationResult(points, finalHit);
    }

    private SimulationResult simulate(ProjectileEntity projectile, float tickDelta) {
        double gravity = projectile.hasNoGravity() ? 0.0 : getGravity(projectile);
        SimulationResult result = simulatePath(projectile.getLerpedPos(1.0f), projectile.getVelocity(), projectile, 0.99, gravity, projectile);
        if (!result.points.isEmpty()) {
            result.points.set(0, projectile.getLerpedPos(tickDelta));
        }
        return result;
    }

    private double getDrag(ProjectileEntity projectile, Vec3d position) {
        boolean inFluid = !mc.world.getFluidState(BlockPos.ofFloored(position)).isEmpty();
        if (projectile instanceof TridentEntity) return inFluid ? 0.99 : 0.99;
        if (projectile instanceof PersistentProjectileEntity) return inFluid ? 0.6 : 0.99;
        if (projectile instanceof ThrownEntity) return inFluid ? 0.8 : 0.99;
        if (projectile instanceof AbstractFireballEntity || projectile instanceof AbstractWindChargeEntity) return 0.95;
        return 0.99;
    }

    private double getGravity(ProjectileEntity projectile) {
        EntityType<?> type = projectile.getType();
        if (type == EntityType.FIREBALL || type == EntityType.SMALL_FIREBALL || type == EntityType.DRAGON_FIREBALL || type == EntityType.WITHER_SKULL) {
            return 0.0;
        }
        if (projectile instanceof PersistentProjectileEntity) return 0.05;
        if (projectile instanceof ThrownEntity) return 0.03;
        if (projectile instanceof AbstractWindChargeEntity) return 0.0;
        if (projectile instanceof FireworkRocketEntity) return 0.0;
        return 0.03;
    }

    private void drawLinePath(VertexConsumer buffer, MatrixStack.Entry entry, List<Vec3d> points, Vec3d camera, Color color) {
        float width = (float) lineWidth.getValue();
        int segCount = points.size() - 1;
        for (int i = 0; i < segCount; i++) {
            Vec3d start = points.get(i).subtract(camera);
            Vec3d end = points.get(i + 1).subtract(camera);
            float t = segCount <= 1 ? 1f : (float) i / segCount;
            int alpha = (int) (255 * (0.12f + 0.88f * t));
            line(buffer, entry, color, start.x, start.y, start.z, end.x, end.y, end.z, width, alpha);
        }
    }



    private void drawBoxOutline(VertexConsumer buffer, MatrixStack.Entry entry, Box box, Color color) {
        float width = (float) lineWidth.getValue();
        double minX = box.minX, minY = box.minY, minZ = box.minZ;
        double maxX = box.maxX, maxY = box.maxY, maxZ = box.maxZ;

        line(buffer, entry, color, minX, minY, minZ, maxX, minY, minZ, width, 255);
        line(buffer, entry, color, maxX, minY, minZ, maxX, minY, maxZ, width, 255);
        line(buffer, entry, color, maxX, minY, maxZ, minX, minY, maxZ, width, 255);
        line(buffer, entry, color, minX, minY, maxZ, minX, minY, minZ, width, 255);

        line(buffer, entry, color, minX, maxY, minZ, maxX, maxY, minZ, width, 255);
        line(buffer, entry, color, maxX, maxY, minZ, maxX, maxY, maxZ, width, 255);
        line(buffer, entry, color, maxX, maxY, maxZ, minX, maxY, maxZ, width, 255);
        line(buffer, entry, color, minX, maxY, maxZ, minX, maxY, minZ, width, 255);

        line(buffer, entry, color, minX, minY, minZ, minX, maxY, minZ, width, 255);
        line(buffer, entry, color, maxX, minY, minZ, maxX, maxY, minZ, width, 255);
        line(buffer, entry, color, maxX, minY, maxZ, maxX, maxY, maxZ, width, 255);
        line(buffer, entry, color, minX, minY, maxZ, minX, maxY, maxZ, width, 255);
    }

    private void line(VertexConsumer buffer, MatrixStack.Entry entry, Color color,
                      double x1, double y1, double z1, double x2, double y2, double z2, float width, int alpha) {
        float dx = (float) (x2 - x1);
        float dy = (float) (y2 - y1);
        float dz = (float) (z2 - z1);
        float length = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (length <= 0.0001F) return;

        buffer.vertex(entry, (float) x1, (float) y1, (float) z1)
                .color(color.getRed(), color.getGreen(), color.getBlue(), alpha)
                .normal(entry, dx / length, dy / length, dz / length)
                .lineWidth(width);
        buffer.vertex(entry, (float) x2, (float) y2, (float) z2)
                .color(color.getRed(), color.getGreen(), color.getBlue(), alpha)
                .normal(entry, dx / length, dy / length, dz / length)
                .lineWidth(width);
    }

    private static class SimulationResult {
        public final List<Vec3d> points;
        public final HitResult hitResult;

        public SimulationResult(List<Vec3d> points, HitResult hitResult) {
            this.points = points;
            this.hitResult = hitResult;
        }
    }

    private void updateVisibility() {
        color.setVisible(!themeColor.isEnabled());
    }
}
