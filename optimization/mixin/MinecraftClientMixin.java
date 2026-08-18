package org.framesync.mixin;

import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.textures.GpuTexture;
import net.minecraft.client.MinecraftClient;
import org.framesync.client.RenderManager;
import org.framesync.config.ModConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Recreates FrameSync's original frame-skipping optimization through
 * Minecraft's native skip path. Minecraft still blits the last complete main
 * framebuffer and updates its loop/FPS accounting, while the expensive
 * GameRenderer invocation is skipped until the next FrameSync render slot.
 */
@Mixin(MinecraftClient.class)
public class MinecraftClientMixin {
    @Shadow
    public boolean skipGameRender;

    @Unique
    private boolean framesync$previousSkipGameRender;

    @Unique
    private boolean framesync$changedSkipGameRender;

    @Inject(method = "render", at = @At("HEAD"))
    private void framesync$beforeRender(boolean tick, CallbackInfo ci) {
        this.framesync$previousSkipGameRender = this.skipGameRender;
        this.framesync$changedSkipGameRender = false;

        ModConfig config = ModConfig.getInstance();
        if (this.skipGameRender || !config.enableRenderLimit || config.isUnlimitedFps()) {
            return;
        }

        RenderManager renderManager = RenderManager.getInstance();
        if (renderManager.isInitialized() && !renderManager.shouldRender()) {
            this.skipGameRender = true;
            this.framesync$changedSkipGameRender = true;
        }
    }

    /**
     * Minecraft 1.21.11 clears the main framebuffer before it checks
     * skipGameRender. Preserve the previous complete framebuffer on a
     * FrameSync-skipped loop so blitToScreen never receives a blank texture.
     */
    @Redirect(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/systems/CommandEncoder;clearColorAndDepthTextures(Lcom/mojang/blaze3d/textures/GpuTexture;ILcom/mojang/blaze3d/textures/GpuTexture;D)V"
            )
    )
    private void framesync$preserveLastFrame(
            CommandEncoder encoder,
            GpuTexture colorTexture,
            int clearColor,
            GpuTexture depthTexture,
            double clearDepth
    ) {
        if (!this.framesync$changedSkipGameRender) {
            encoder.clearColorAndDepthTextures(colorTexture, clearColor, depthTexture, clearDepth);
        }
    }

    @Inject(method = "render", at = @At("RETURN"))
    private void framesync$afterRender(boolean tick, CallbackInfo ci) {
        if (this.framesync$changedSkipGameRender) {
            this.skipGameRender = this.framesync$previousSkipGameRender;
            this.framesync$changedSkipGameRender = false;
        }
    }
}
