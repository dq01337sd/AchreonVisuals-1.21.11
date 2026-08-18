/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.MinecraftClient
 *  net.minecraft.client.gui.DrawContext
 *  net.minecraft.text.Text
 */
package org.framesync.client;

import java.util.Objects;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import org.framesync.client.RenderManager;
import org.framesync.config.ModConfig;

public class FpsIndicator {
    private static final MinecraftClient client = MinecraftClient.getInstance();
    private static long lastUpdateTime = 0L;
    private static int frameCount = 0;
    private static double currentFps = 0.0;

    public static void updateFps() {
        ++frameCount;
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastUpdateTime >= 1000L) {
            currentFps = (double)frameCount * 1000.0 / (double)(currentTime - lastUpdateTime);
            frameCount = 0;
            lastUpdateTime = currentTime;
        }
    }

    public static void render(DrawContext context) {
        RenderManager renderManager;
        ModConfig config = ModConfig.getInstance();
        if (config.showFpsIndicator && !FpsIndicator.client.options.hudHidden && (renderManager = RenderManager.getInstance()).isInitialized()) {
            int effectiveLimit = config.getEffectiveHz();
            boolean unlimited = !config.enableRenderLimit || config.isUnlimitedFps();
            String fpsText = unlimited
                    ? String.format("FPS: %.1f (Unlimited)", currentFps)
                    : String.format("FPS: %.1f / %d Hz", currentFps, effectiveLimit);
            int color = unlimited ? 0xFFFFFF : FpsIndicator.getFpsColor(currentFps, effectiveLimit);
            int x = context.getScaledWindowWidth() - FpsIndicator.client.textRenderer.getWidth(fpsText) - 10;
            int y = 10;
            int var10001 = x - 2;
            int var10002 = y - 2;
            int var10003 = x + FpsIndicator.client.textRenderer.getWidth(fpsText) + 2;
            Objects.requireNonNull(FpsIndicator.client.textRenderer);
            context.fill(var10001, var10002, var10003, y + 9 + 2, Integer.MIN_VALUE);
            context.drawTextWithShadow(FpsIndicator.client.textRenderer, Text.literal(fpsText), x, y, color);
        }
    }

    private static int getFpsColor(double currentFps, int targetFps) {
        double ratio = currentFps / (double)targetFps;
        if (ratio >= 0.9) {
            return 65280;
        }
        return ratio >= 0.7 ? 0xFFFF00 : 0xFF0000;
    }
}
