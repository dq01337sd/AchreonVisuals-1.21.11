package ez.minar.system.managers;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;

public class RotationManager {
    private static final MinecraftClient mc = MinecraftClient.getInstance();
    private static final long IDLE_TIMEOUT_MS = 120L;
    private static final float MAX_SERVER_ROTATION_STEP = 30.0F;

    private static Rotation currentRotation = Rotation.ZERO;
    private static Rotation previousRotation = Rotation.ZERO;
    private static Rotation renderRotation = Rotation.ZERO;
    private static Rotation targetRotation;
    private static float yawSpeed;
    private static float pitchSpeed;
    private static float returnSpeed;
    private static MoveCorrection moveCorrection = MoveCorrection.NONE;
    private static long lastRotateTime;
    private static State state = State.IDLE;
    private static boolean skipGcd;

    public static void update() {
        previousRotation = currentRotation;

        if (mc.player == null) {
            reset();
            return;
        }

        if (targetRotation == null) {
            currentRotation = getPlayerRotation();
            state = State.IDLE;
            return;
        }

        if (System.currentTimeMillis() - lastRotateTime > IDLE_TIMEOUT_MS) {
            Rotation playerRotation = getPlayerRotation();
            if (currentRotation.differenceValue(playerRotation) < 1.0F) {
                targetRotation = null;
                currentRotation = playerRotation;
                state = State.IDLE;
                return;
            }

            state = State.ROTATING_BACK;

            float yawDiff = getAngleDifference(currentRotation.yaw(), playerRotation.yaw());
            float pitchDiff = getAngleDifference(currentRotation.pitch(), playerRotation.pitch());

            float yawStep = Math.max(4.0F, Math.abs(yawDiff) * 0.55F);
            float pitchStep = Math.max(4.0F, Math.abs(pitchDiff) * 0.55F);

            yawStep = Math.min(yawStep, returnSpeed);
            pitchStep = Math.min(pitchStep, returnSpeed);

            Rotation backStep = new Rotation(
                    moveTowardsAngle(currentRotation.yaw(), playerRotation.yaw(), yawStep),
                    moveTowardsAngle(currentRotation.pitch(), playerRotation.pitch(), pitchStep)
            );
            currentRotation = skipGcd ? backStep : correctRotation(backStep);
            return;
        }

        state = State.ROTATING;
        Rotation fwdStep = new Rotation(
                moveTowardsAngle(currentRotation.yaw(), targetRotation.yaw(), yawSpeed),
                moveTowardsAngle(currentRotation.pitch(), targetRotation.pitch(), pitchSpeed)
        );
        currentRotation = skipGcd ? fwdStep : correctRotation(fwdStep);
    }

    public static void updateRender(float tickDelta) {
        float yaw = interpolateAngle(previousRotation.yaw(), currentRotation.yaw(), tickDelta);
        float pitch = MathHelper.lerp(tickDelta, previousRotation.pitch(), currentRotation.pitch());
        renderRotation = new Rotation(yaw, pitch <= -85.0F ? 0.0F : pitch);
    }

    public static void rotate(Rotation rotation, MoveCorrection moveCorrection, float yawSpeed, float pitchSpeed, float returnSpeed) {
        rotate(rotation, moveCorrection, yawSpeed, pitchSpeed, returnSpeed, false);
    }

    public static void rotate(Rotation rotation, MoveCorrection moveCorrection, float yawSpeed, float pitchSpeed, float returnSpeed, boolean skipGcd) {
        rotate(rotation, moveCorrection, yawSpeed, pitchSpeed, returnSpeed, skipGcd, MAX_SERVER_ROTATION_STEP);
    }

    public static void rotate(Rotation rotation, MoveCorrection moveCorrection, float yawSpeed, float pitchSpeed, float returnSpeed, boolean skipGcd, float maxStep) {
        if (mc.player == null) return;

        float adjustedYaw = adjustAngle(targetRotation == null ? getPlayerRotation().yaw() : targetRotation.yaw(), rotation.yaw());
        targetRotation = new Rotation(adjustedYaw, MathHelper.clamp(rotation.pitch(), -90.0F, 90.0F));
        RotationManager.yawSpeed = clampServerStep(yawSpeed, maxStep);
        RotationManager.pitchSpeed = clampServerStep(pitchSpeed, maxStep);
        RotationManager.returnSpeed = clampServerStep(returnSpeed, maxStep);
        RotationManager.moveCorrection = moveCorrection;
        RotationManager.skipGcd = skipGcd;
        lastRotateTime = System.currentTimeMillis();
        state = State.ROTATING;

        Rotation stepped = new Rotation(
                moveTowardsAngle(currentRotation.yaw(), targetRotation.yaw(), RotationManager.yawSpeed),
                moveTowardsAngle(currentRotation.pitch(), targetRotation.pitch(), RotationManager.pitchSpeed)
        );
        currentRotation = skipGcd ? stepped : correctRotation(stepped);
    }

    public static void rotate(Rotation rotation, float yawSpeed, float pitchSpeed, float returnSpeed) {
        rotate(rotation, MoveCorrection.NONE, yawSpeed, pitchSpeed, returnSpeed, false);
    }

    public static void reset() {
        targetRotation = null;
        state = State.IDLE;
        moveCorrection = MoveCorrection.NONE;
        skipGcd = false;
        if (mc.player != null) {
            currentRotation = getPlayerRotation();
            previousRotation = currentRotation;
            renderRotation = currentRotation;
        }
    }

    public static void releaseSmooth(float speed) {
        if (mc.player == null || targetRotation == null) {
            reset();
            return;
        }
        returnSpeed = clampServerStep(speed);
        lastRotateTime = 0L;
    }

    public static boolean isActive() {
        return mc.player != null && state != State.IDLE;
    }

    public static float getYaw(float fallback) {
        return isActive() ? currentRotation.yaw() : fallback;
    }

    public static float getPitch(float fallback) {
        return isActive() ? currentRotation.pitch() : fallback;
    }

    public static float getRenderYaw(float fallback) {
        return isActive() ? renderRotation.yaw() : fallback;
    }

    public static float getRenderPitch(float fallback) {
        return isActive() ? renderRotation.pitch() : fallback;
    }

    public static Rotation getCurrentRotation() {
        return currentRotation;
    }

    public static boolean shouldCorrectMovement() {
        return isActive() && moveCorrection != MoveCorrection.NONE;
    }

    public static boolean shouldCorrectInputSilently() {
        return isActive() && moveCorrection == MoveCorrection.SILENT;
    }

    public static Vec2f correctMovementInput(float forward, float strafe) {
        if (mc.player == null || !shouldCorrectInputSilently() || (forward == 0.0F && strafe == 0.0F)) {
            return new Vec2f(strafe, forward);
        }

        double angle = MathHelper.wrapDegrees(Math.toDegrees(direction(mc.player.getYaw(), forward, strafe)));
        float closestForward = forward;
        float closestStrafe = strafe;
        float closestDifference = Float.MAX_VALUE;

        for (float predictedForward = -1.0F; predictedForward <= 1.0F; predictedForward++) {
            for (float predictedStrafe = -1.0F; predictedStrafe <= 1.0F; predictedStrafe++) {
                if (predictedForward == 0.0F && predictedStrafe == 0.0F) continue;

                double predictedAngle = MathHelper.wrapDegrees(Math.toDegrees(direction(currentRotation.yaw(), predictedForward, predictedStrafe)));
                float difference = (float) Math.abs(MathHelper.wrapDegrees(angle - predictedAngle));
                if (difference < closestDifference) {
                    closestDifference = difference;
                    closestForward = predictedForward;
                    closestStrafe = predictedStrafe;
                }
            }
        }

        return new Vec2f(closestStrafe, closestForward);
    }

    public static Rotation getRotationTo(Vec3d pos) {
        Vec3d eyes = mc.player.getEyePos();
        double deltaX = pos.x - eyes.x;
        double deltaY = pos.y - eyes.y;
        double deltaZ = pos.z - eyes.z;
        double horizontal = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
        float yaw = (float) Math.toDegrees(Math.atan2(deltaZ, deltaX)) - 90.0F;
        float pitch = (float) -Math.toDegrees(Math.atan2(deltaY, horizontal));
        return new Rotation(yaw, pitch);
    }

    public static Vec3d getNearestPoint(net.minecraft.entity.LivingEntity entity) {
        Vec3d eyes = mc.player.getEyePos();
        return new Vec3d(
                MathHelper.clamp(eyes.x, entity.getBoundingBox().minX, entity.getBoundingBox().maxX),
                MathHelper.clamp(eyes.y, entity.getBoundingBox().minY, entity.getBoundingBox().maxY),
                MathHelper.clamp(eyes.z, entity.getBoundingBox().minZ, entity.getBoundingBox().maxZ)
        );
    }

    private static Rotation getPlayerRotation() {
        return mc.player == null ? Rotation.ZERO : new Rotation(mc.player.getYaw(), mc.player.getPitch());
    }

    private static Rotation correctRotation(Rotation rotation) {
        double gcd = getGcd();
        float yaw = (float) (rotation.yaw() - rotation.yaw() % gcd);
        float pitch = (float) (rotation.pitch() - rotation.pitch() % gcd);
        return new Rotation(yaw, pitch);
    }

    private static double getGcd() {
        double sensitivity = mc.options.getMouseSensitivity().getValue() * 0.6F + 0.2F;
        return sensitivity * sensitivity * sensitivity * 8.0 * 0.15F;
    }

    private static float clampServerStep(float speed) {
        return clampServerStep(speed, MAX_SERVER_ROTATION_STEP);
    }

    private static float clampServerStep(float speed, float maxStep) {
        return MathHelper.clamp(speed, 0.0F, Math.max(0.0F, maxStep));
    }

    private static float moveTowardsAngle(float current, float target, float speed) {
        float difference = getAngleDifference(current, target);
        return Math.abs(difference) <= speed ? target : current + Math.signum(difference) * speed;
    }

    private static float getAngleDifference(float current, float target) {
        return MathHelper.wrapDegrees(target - current);
    }

    private static float adjustAngle(float currentAngle, float targetAngle) {
        float normalizedCurrent = currentAngle % 360.0F;
        if (normalizedCurrent < 0.0F) normalizedCurrent += 360.0F;

        float normalizedTarget = targetAngle % 360.0F;
        if (normalizedTarget < 0.0F) normalizedTarget += 360.0F;

        int revolutions = (int) (currentAngle / 360.0F);
        if (currentAngle < 0.0F && currentAngle % 360.0F != 0.0F) revolutions--;

        float adjustedTarget = normalizedTarget + revolutions * 360.0F;
        float difference = adjustedTarget - currentAngle;
        if (difference > 180.0F) adjustedTarget -= 360.0F;
        if (difference < -180.0F) adjustedTarget += 360.0F;

        return adjustedTarget;
    }

    private static float interpolateAngle(float start, float end, float delta) {
        return start + MathHelper.wrapDegrees(end - start) * delta;
    }

    private static double direction(float yaw, float forward, float strafe) {
        if (forward < 0.0F) {
            yaw += 180.0F;
        }

        float factor = 1.0F;
        if (forward < 0.0F) {
            factor = -0.5F;
        } else if (forward > 0.0F) {
            factor = 0.5F;
        }

        if (strafe > 0.0F) {
            yaw -= 90.0F * factor;
        }
        if (strafe < 0.0F) {
            yaw += 90.0F * factor;
        }

        return Math.toRadians(yaw);
    }

    private enum State {
        IDLE,
        ROTATING,
        ROTATING_BACK
    }

    public enum MoveCorrection {
        NONE,
        DIRECT,
        SILENT
    }

    public record Rotation(float yaw, float pitch) {
        public static final Rotation ZERO = new Rotation(0.0F, 0.0F);

        public float differenceValue(Rotation other) {
            return Math.abs(MathHelper.wrapDegrees(other.yaw - yaw)) + Math.abs(MathHelper.wrapDegrees(other.pitch - pitch));
        }
    }
}
