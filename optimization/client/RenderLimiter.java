/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.MinecraftClient
 *  org.lwjgl.glfw.GLFW
 *  org.lwjgl.glfw.GLFWVidMode
 */
package org.framesync.client;

import net.minecraft.client.MinecraftClient;
import org.framesync.FrameSync;
import org.framesync.config.ModConfig;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWVidMode;

public class RenderLimiter {
    private int monitorHz;
    private long lastRenderTime;
    private boolean enabled;
    private long frameInterval;
    private long[] frameTimeHistory;
    private int frameTimeIndex;
    private static final int FRAME_HISTORY_SIZE = 10;
    private long accumulatedTime;
    private boolean lowEndMode;
    private static final long MIN_FRAME_TIME = 1000000L;

    public RenderLimiter(int monitorHz) {
        this.monitorHz = monitorHz;
        this.enabled = true;
        this.lastRenderTime = System.nanoTime();
        this.frameInterval = 1000000000L / (long)monitorHz;
        this.frameTimeHistory = new long[10];
        this.frameTimeIndex = 0;
        this.accumulatedTime = 0L;
        this.lowEndMode = ModConfig.getInstance().lowEndMode;
        FrameSync.LOGGER.info("RenderLimiter initialized with {} Hz", (Object)monitorHz);
    }

    public boolean canRender() {
        if (!this.enabled) {
            return true;
        }
        ModConfig config = ModConfig.getInstance();
        return config.lowEndMode ? this.canRenderSmooth() : this.canRenderAggressive();
    }

    private boolean canRenderSmooth() {
        long now = System.nanoTime();
        long deltaTime = now - this.lastRenderTime;
        this.accumulatedTime += deltaTime;
        if (this.accumulatedTime >= this.frameInterval) {
            this.frameTimeHistory[this.frameTimeIndex] = deltaTime;
            this.frameTimeIndex = (this.frameTimeIndex + 1) % 10;
            this.accumulatedTime -= this.frameInterval;
            if (this.accumulatedTime > this.frameInterval * 2L) {
                this.accumulatedTime = this.frameInterval;
            }
            this.lastRenderTime = now;
            return true;
        }
        if (deltaTime > this.frameInterval * 3L) {
            this.lastRenderTime = now;
            this.accumulatedTime = 0L;
            return true;
        }
        return false;
    }

    private boolean canRenderAggressive() {
        long now = System.nanoTime();
        if (now - this.lastRenderTime >= this.frameInterval) {
            this.frameTimeHistory[this.frameTimeIndex] = now - this.lastRenderTime;
            this.frameTimeIndex = (this.frameTimeIndex + 1) % 10;
            this.lastRenderTime = now;
            return true;
        }
        return false;
    }

    public boolean canRenderOld() {
        if (!this.enabled) {
            return true;
        }
        long now = System.nanoTime();
        if (now - this.lastRenderTime >= this.frameInterval) {
            this.lastRenderTime = now;
            return true;
        }
        return false;
    }

    public void setMonitorHz(int hz) {
        if (hz <= 0) {
            FrameSync.LOGGER.warn("Invalid monitor Hz value: {}, using default 60", (Object)hz);
            hz = 60;
        }
        this.monitorHz = hz;
        this.frameInterval = 1000000000L / (long)hz;
        this.lastRenderTime = System.nanoTime();
        this.accumulatedTime = 0L;
        this.frameTimeIndex = 0;
        FrameSync.LOGGER.info("Monitor Hz updated to: {}", (Object)hz);
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        if (enabled) {
            this.lastRenderTime = System.nanoTime();
            this.accumulatedTime = 0L;
            this.frameTimeIndex = 0;
        }
        FrameSync.LOGGER.info("RenderLimiter {}", (Object)(enabled ? "enabled" : "disabled"));
    }

    public int getMonitorHz() {
        return this.monitorHz;
    }

    public boolean isEnabled() {
        return this.enabled;
    }

    public double getCurrentFPS() {
        if (!this.enabled) {
            return -1.0;
        }
        long totalTime = 0L;
        int validFrames = 0;
        for (long frameTime : this.frameTimeHistory) {
            if (frameTime <= 0L) continue;
            totalTime += frameTime;
            ++validFrames;
        }
        if (validFrames > 0) {
            long avgFrameTime = totalTime / (long)validFrames;
            return 1.0E9 / (double)avgFrameTime;
        }
        return this.monitorHz;
    }

    public double getCurrentFPSOld() {
        return !this.enabled ? -1.0 : (double)this.monitorHz;
    }

    public static int detectMonitorHz() {
        try {
            if (!GLFW.glfwInit()) {
                FrameSync.LOGGER.warn("GLFW not initialized, using default refresh rate: 60 Hz");
                return 60;
            }
            long primaryMonitor = GLFW.glfwGetPrimaryMonitor();
            if (primaryMonitor == 0L) {
                FrameSync.LOGGER.warn("No primary monitor found, using default refresh rate: 60 Hz");
                return 60;
            }
            GLFWVidMode vidMode = GLFW.glfwGetVideoMode((long)primaryMonitor);
            if (vidMode != null) {
                int refreshRate = vidMode.refreshRate();
                FrameSync.LOGGER.info("Detected monitor refresh rate: {} Hz", (Object)refreshRate);
                return refreshRate;
            }
        }
        catch (Exception e) {
            FrameSync.LOGGER.warn("Failed to detect monitor refresh rate: {}", (Object)e.getMessage());
        }
        FrameSync.LOGGER.info("Using default refresh rate: 60 Hz");
        return 60;
    }

    public static int safeDetectMonitorHz() {
        try {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client != null && client.getWindow() != null) {
                return RenderLimiter.detectMonitorHz();
            }
        }
        catch (Exception e) {
            FrameSync.LOGGER.debug("Client not ready for monitor detection: {}", (Object)e.getMessage());
        }
        FrameSync.LOGGER.info("Client not ready, using default refresh rate: 60 Hz");
        return 60;
    }
}

