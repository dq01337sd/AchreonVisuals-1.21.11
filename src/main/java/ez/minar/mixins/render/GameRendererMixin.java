package ez.minar.mixins.render;

import ez.minar.system.events.EventBus;
import ez.minar.system.events.impl.Render2DEvent;
import ez.minar.system.api.FunctionManager;
import ez.minar.system.features.render.HandChams;
import ez.minar.system.features.render.NoRender;
import ez.minar.system.managers.RotationManager;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.registry.entry.RegistryEntry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.joml.Matrix4f;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
    @Shadow
    private void renderHand(float tickProgress, boolean sleeping, Matrix4f positionMatrix) {
    }

    @Inject(method = "render", at = @At("RETURN"))
    private void render(RenderTickCounter tickCounter, boolean tick, CallbackInfo ci) {
        RotationManager.updateRender(tickCounter.getTickProgress(true));
        EventBus.post(new Render2DEvent(tickCounter));
    }

    @Unique
    private boolean minar$capturingChamsHand;

    @Inject(method = "renderHand", at = @At("RETURN"))
    private void minar$drawChamsHandPost(float tickProgress, boolean sleeping, Matrix4f positionMatrix, CallbackInfo ci) {
        HandChams chams = FunctionManager.getFunction(HandChams.class);
        if (chams == null || !chams.shouldRenderHands() || minar$capturingChamsHand) return;

        minar$capturingChamsHand = true;
        try {
            chams.renderMask(() -> renderHand(tickProgress, sleeping, positionMatrix));
        } finally {
            minar$capturingChamsHand = false;
        }
        chams.drawPostEffect();
    }

    @Inject(method = "tiltViewWhenHurt", at = @At("HEAD"), cancellable = true)
    private void minar$noRenderHurtCamera(MatrixStack matrices, float tickProgress, CallbackInfo ci) {
        if (NoRender.shouldDisableHurtCamera()) {
            ci.cancel();
        }
    }

    @Redirect(method = "renderWorld",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/network/ClientPlayerEntity;getEffectFadeFactor(Lnet/minecraft/registry/entry/RegistryEntry;F)F"))
    private float minar$noRenderNauseaProjection(ClientPlayerEntity player, RegistryEntry<StatusEffect> effect,
                                                 float tickProgress) {
        if (NoRender.shouldDisableBadEffects() && effect == StatusEffects.NAUSEA) {
            return 0.0f;
        }

        return player.getEffectFadeFactor(effect, tickProgress);
    }
}
