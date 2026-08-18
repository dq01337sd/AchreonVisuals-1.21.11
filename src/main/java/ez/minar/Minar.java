package ez.minar;

import ez.minar.system.api.FunctionManager;
import ez.minar.system.events.EventBus;
import ez.minar.system.events.EventManager;
import ez.minar.system.commands.MinarClientCommands;
import ez.minar.system.features.render.BlockOverlay;
import ez.minar.system.features.render.HitWave;
import ez.minar.system.features.render.FireFly;
import ez.minar.system.features.render.JumpCircles;
import ez.minar.system.features.render.Particles;
import ez.minar.system.features.render.Predictions;
import ez.minar.system.features.render.ShaderSky;
import ez.minar.system.features.render.TargetESP;
import ez.minar.system.features.render.ChinaHat;
import ez.minar.system.features.render.LineGlyphs;
import ez.minar.system.managers.AltManager;
import ez.minar.system.managers.ConfigManager;
import ez.minar.system.menu.ThemeManager;
import ez.minar.optimization.OptimizationInit;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;

public class Minar implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        ThemeManager.init();
        AltManager.init();
        FunctionManager.init();
        ConfigManager.init();
        MinarClientCommands.init();
        OptimizationInit.init();
        WorldRenderEvents.START_MAIN.register(ShaderSky::renderWorld);
        WorldRenderEvents.END_MAIN.register(Particles::renderWorld);
        WorldRenderEvents.END_MAIN.register(FireFly::renderWorld);
        WorldRenderEvents.END_MAIN.register(JumpCircles::renderWorld);
        WorldRenderEvents.END_MAIN.register(TargetESP::renderWorld);
        WorldRenderEvents.END_MAIN.register(BlockOverlay::renderWorld);
        WorldRenderEvents.END_MAIN.register(Predictions::renderWorld);
        WorldRenderEvents.END_MAIN.register(HitWave::renderWorld);
        WorldRenderEvents.END_MAIN.register(ChinaHat::renderWorld);
        WorldRenderEvents.END_MAIN.register(LineGlyphs::renderWorld);

        WorldRenderEvents.BEFORE_BLOCK_OUTLINE.register((context, outlineRenderState) ->
                BlockOverlay.shouldRenderVanillaBlockOutline());

        EventBus.register(new EventManager());
    }
}
