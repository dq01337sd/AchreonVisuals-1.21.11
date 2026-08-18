/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.ClientModInitializer
 *  net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
 *  net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper
 *  net.minecraft.client.MinecraftClient
 *  net.minecraft.client.gui.screen.Screen
 *  net.minecraft.client.option.KeyBinding
 *  net.minecraft.client.util.InputUtil$Type
 */
package org.framesync.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Identifier;
import org.framesync.FrameSync;
import org.framesync.client.RenderManager;
import org.framesync.config.ConfigScreen;
import org.framesync.config.ModConfig;

public class FrameSyncClient
implements ClientModInitializer {
    private static KeyBinding configKeyBinding;
    private static final KeyBinding.Category CATEGORY = KeyBinding.Category.create(Identifier.of(FrameSync.MOD_ID, "config"));
    private static boolean renderSystemInitialized;

    public void onInitializeClient() {
        FrameSync.LOGGER.info("FrameSync client initialized!");
        this.registerKeyBindings();
        this.registerEventHandlers();
    }

    private void initializeRenderSystemSafely() {
        MinecraftClient client;
        if (!renderSystemInitialized && (client = MinecraftClient.getInstance()) != null && client.getWindow() != null) {
            try {
                ModConfig config = ModConfig.getInstance();
                RenderManager.getInstance().initialize(config.getEffectiveHz(), config.enableRenderLimit);
                renderSystemInitialized = true;
                FrameSync.LOGGER.info("Render system initialized with {} Hz", (Object)config.getEffectiveHz());
            }
            catch (Exception e) {
                FrameSync.LOGGER.error("Failed to initialize render system: {}", (Object)e.getMessage());
            }
        }
    }

    private void registerKeyBindings() {
        configKeyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding("framesync.key.config", InputUtil.Type.KEYSYM, 71, CATEGORY));
    }

    private void registerEventHandlers() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            this.initializeRenderSystemSafely();
            while (configKeyBinding.wasPressed()) {
                if (client.currentScreen != null) continue;
                client.setScreen((Screen)new ConfigScreen(null));
            }
        });
    }

    static {
        renderSystemInitialized = false;
    }
}
