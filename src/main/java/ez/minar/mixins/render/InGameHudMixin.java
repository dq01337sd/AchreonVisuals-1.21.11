package ez.minar.mixins.render;

import ez.minar.system.events.EventBus;
import ez.minar.system.events.EventManager;
import ez.minar.system.events.impl.Render2DEvent;
import ez.minar.system.features.render.HUD;
import ez.minar.system.features.render.NoRender;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(InGameHud.class)
public class InGameHudMixin {
    MinecraftClient mc = MinecraftClient.getInstance();

    @Inject(method = "render", at = @At("RETURN"))
    private void renderHud(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        ez.minar.optimization.FpsIndicator.updateFps();
        ez.minar.optimization.FpsIndicator.render(context);
        EventBus.post(new Render2DEvent(tickCounter, context));
    }

    @Inject(method = "renderCrosshair", at = @At("HEAD"), cancellable = true)
    private void renderCrosshair(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        if (mc.currentScreen == EventManager.clickGui || NoRender.shouldDisableCrosshair()) {
            ci.cancel();
        }
    }

    @Inject(method = "renderVignetteOverlay", at = @At("HEAD"), cancellable = true)
    private void minar$noRenderVignette(DrawContext context, Entity entity, CallbackInfo ci) {
        if (NoRender.shouldDisableVignette()) {
            ci.cancel();
        }
    }

    @Inject(method = "renderNauseaOverlay", at = @At("HEAD"), cancellable = true)
    private void minar$noRenderBadEffects(DrawContext context, float distortionStrength, CallbackInfo ci) {
        if (NoRender.shouldDisableBadEffects()) {
            ci.cancel();
        }
    }

    @Inject(method = "renderHotbar", at = @At("HEAD"), cancellable = true)
    private void minar$cancelDefaultHotbar(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        if (HUD.shouldDisableDefaultHotbar()) {
            ci.cancel();
        }
    }

    @Inject(method = "renderStatusBars", at = @At("HEAD"), cancellable = true)
    private void minar$cancelDefaultStatusBars(DrawContext context, CallbackInfo ci) {
        if (HUD.shouldDisableDefaultHotbar()) {
            ci.cancel();
        }
    }

    @Inject(method = "renderStatusEffectOverlay", at = @At("HEAD"), cancellable = true)
    private void minar$hideDefaultStatusEffectIcons(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        if (HUD.shouldHideDefaultStatusEffectIcons()) {
            ci.cancel();
        }
    }

    @Inject(method = "renderHeldItemTooltip", at = @At("HEAD"), cancellable = true)
    private void minar$cancelDefaultHeldItemTooltip(DrawContext context, CallbackInfo ci) {
        if (HUD.shouldDisableDefaultHotbar()) {
            ci.cancel();
        }
    }

    @Inject(method = "shouldShowExperienceBar", at = @At("HEAD"), cancellable = true)
    private void minar$hideDefaultExperienceBar(CallbackInfoReturnable<Boolean> cir) {
        if (HUD.shouldDisableDefaultHotbar()) {
            cir.setReturnValue(false);
        }
    }

    @Redirect(method = "getCurrentBarType",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerInteractionManager;hasExperienceBar()Z"))
    private boolean minar$disableExperienceBarType(ClientPlayerInteractionManager interactionManager) {
        return !HUD.shouldDisableDefaultHotbar() && interactionManager.hasExperienceBar();
    }

    @Redirect(method = "renderMainHud",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerInteractionManager;hasExperienceBar()Z"))
    private boolean minar$disableExperienceLevelText(ClientPlayerInteractionManager interactionManager) {
        return !HUD.shouldDisableDefaultHotbar() && interactionManager.hasExperienceBar();
    }

    @Inject(method = "renderScoreboardSidebar", at = @At("HEAD"), cancellable = true)
    private void minar$cancelScoreboard(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        if (HUD.shouldDisableDefaultScoreboard()) {
            ci.cancel();
        }
    }

}
