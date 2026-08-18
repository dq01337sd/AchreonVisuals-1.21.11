/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.Gson
 *  com.google.gson.GsonBuilder
 *  net.fabricmc.loader.api.FabricLoader
 */
package org.framesync.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import net.fabricmc.loader.api.FabricLoader;
import org.framesync.FrameSync;
import org.framesync.client.RenderLimiter;

public class ModConfig {
    private static final String CONFIG_FILE_NAME = "framesync.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static ModConfig instance;
    private static Path configPath;
    public int targetMonitorHz = 60;
    public boolean enableRenderLimit = true;
    public boolean autoDetectMonitorHz = true;
    public int minFpsLimit = 30;
    public boolean showFpsIndicator = false;
    public boolean lowEndMode = false;
    public boolean unlimitedFps = false;

    public static ModConfig getInstance() {
        if (instance == null) {
            instance = ModConfig.loadConfig();
        }
        return instance;
    }

    private static ModConfig loadConfig() {
        if (Files.exists(configPath, new LinkOption[0])) {
            try {
                String json = Files.readString(configPath);
                ModConfig config = (ModConfig)GSON.fromJson(json, ModConfig.class);
                if (config.targetMonitorHz <= 0) {
                    config.targetMonitorHz = 60;
                }
                if (config.minFpsLimit <= 0) {
                    config.minFpsLimit = 30;
                }
                FrameSync.LOGGER.info("Config loaded: {} Hz, enabled: {}", (Object)config.targetMonitorHz, (Object)config.enableRenderLimit);
                return config;
            }
            catch (Exception e) {
                FrameSync.LOGGER.error("Failed to load config, using defaults: {}", (Object)e.getMessage());
            }
        }
        ModConfig defaultConfig = new ModConfig();
        if (defaultConfig.autoDetectMonitorHz) {
            defaultConfig.targetMonitorHz = RenderLimiter.safeDetectMonitorHz();
        }
        defaultConfig.saveConfig();
        return defaultConfig;
    }

    public void saveConfig() {
        try {
            Files.createDirectories(configPath.getParent(), new FileAttribute[0]);
            String json = GSON.toJson((Object)this);
            Files.writeString(configPath, (CharSequence)json, new OpenOption[0]);
            FrameSync.LOGGER.info("Config saved: {} Hz, enabled: {}", (Object)this.targetMonitorHz, (Object)this.enableRenderLimit);
        }
        catch (IOException e) {
            FrameSync.LOGGER.error("Failed to save config: {}", (Object)e.getMessage());
        }
    }

    public void setTargetMonitorHz(int hz) {
        if (hz <= 0) {
            FrameSync.LOGGER.warn("Invalid Hz value: {}, ignoring", (Object)hz);
        } else {
            this.targetMonitorHz = hz;
            this.unlimitedFps = false;
            this.saveConfig();
        }
    }

    public void setUnlimitedFps(boolean unlimited) {
        this.unlimitedFps = unlimited;
        this.saveConfig();
    }

    public boolean isUnlimitedFps() {
        return this.unlimitedFps;
    }

    public void setEnableRenderLimit(boolean enabled) {
        this.enableRenderLimit = enabled;
        this.saveConfig();
    }

    public void setAutoDetectMonitorHz(boolean autoDetect) {
        this.autoDetectMonitorHz = autoDetect;
        if (autoDetect) {
            this.setTargetMonitorHz(RenderLimiter.safeDetectMonitorHz());
        }
        this.saveConfig();
    }

    public void setMinFpsLimit(int minFps) {
        if (minFps <= 0) {
            FrameSync.LOGGER.warn("Invalid min FPS value: {}, ignoring", (Object)minFps);
        } else {
            this.minFpsLimit = minFps;
            this.saveConfig();
        }
    }

    public void setShowFpsIndicator(boolean show) {
        this.showFpsIndicator = show;
        this.saveConfig();
    }

    public void setLowEndMode(boolean lowEnd) {
        this.lowEndMode = lowEnd;
        this.saveConfig();
    }

    public int getEffectiveHz() {
        return Math.max(this.targetMonitorHz, this.minFpsLimit);
    }

    public void resetToDefaults() {
        this.targetMonitorHz = this.autoDetectMonitorHz ? RenderLimiter.safeDetectMonitorHz() : 60;
        this.enableRenderLimit = true;
        this.autoDetectMonitorHz = true;
        this.minFpsLimit = 30;
        this.showFpsIndicator = false;
        this.lowEndMode = false;
        this.unlimitedFps = false;
        this.saveConfig();
        FrameSync.LOGGER.info("Config reset to defaults");
    }

    static {
        configPath = FabricLoader.getInstance().getConfigDir().resolve(CONFIG_FILE_NAME);
    }
}
