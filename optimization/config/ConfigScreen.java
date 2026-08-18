/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.gui.DrawContext
 *  net.minecraft.client.gui.Element
 *  net.minecraft.client.gui.screen.Screen
 *  net.minecraft.client.gui.widget.ButtonWidget
 *  net.minecraft.client.gui.widget.CyclingButtonWidget
 *  net.minecraft.client.gui.widget.SliderWidget
 *  net.minecraft.text.Text
 */
package org.framesync.config;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.text.Text;
import org.framesync.client.RenderLimiter;
import org.framesync.client.RenderManager;
import org.framesync.config.ModConfig;

public class ConfigScreen
extends Screen {
    private static final int MIN_HZ = 30;
    private static final int HZ_RANGE = 210;
    private static final double UNLIMITED_THRESHOLD = 0.999;
    private final Screen parent;
    private final ModConfig config;
    private SliderWidget hzSlider;
    private CyclingButtonWidget<Boolean> enableButton;
    private CyclingButtonWidget<Boolean> autoDetectButton;
    private CyclingButtonWidget<Boolean> fpsIndicatorButton;
    private SliderWidget minFpsSlider;
    private CyclingButtonWidget<Boolean> lowEndModeButton;

    public ConfigScreen(Screen parent) {
        super((Text)Text.translatable((String)"framesync.config.title"));
        this.parent = parent;
        this.config = ModConfig.getInstance();
    }

    protected void init() {
        this.clearChildren();
        int centerX = this.width / 2;
        int startY = this.height / 4;
        int buttonWidth = 200;
        int buttonHeight = 20;
        int spacing = 25;
        Text initialHzText = this.config.isUnlimitedFps()
                ? Text.translatable("framesync.config.monitor_hz_unlimited")
                : Text.translatable("framesync.config.monitor_hz", this.config.targetMonitorHz);
        double initialHzValue = this.config.isUnlimitedFps()
                ? 1.0
                : ((double)this.config.targetMonitorHz - MIN_HZ) / HZ_RANGE;
        this.hzSlider = new SliderWidget(centerX - buttonWidth / 2, startY, buttonWidth, buttonHeight, initialHzText, initialHzValue){

            protected void updateMessage() {
                if (this.value >= UNLIMITED_THRESHOLD) {
                    this.setMessage(Text.translatable("framesync.config.monitor_hz_unlimited"));
                } else {
                    int hz = (int)(MIN_HZ + this.value * HZ_RANGE);
                    this.setMessage(Text.translatable("framesync.config.monitor_hz", hz));
                }
            }

            protected void applyValue() {
                if (this.value >= UNLIMITED_THRESHOLD) {
                    ConfigScreen.this.config.setUnlimitedFps(true);
                } else {
                    int hz = (int)(MIN_HZ + this.value * HZ_RANGE);
                    ConfigScreen.this.config.setTargetMonitorHz(hz);
                }
                ConfigScreen.this.applyConfigToLimiter();
            }
        };
        this.addDrawableChild(this.hzSlider);
        this.enableButton = CyclingButtonWidget.onOffBuilder(Text.translatable("framesync.config.enabled"), Text.translatable("framesync.config.disabled"), this.config.enableRenderLimit).build(centerX - buttonWidth / 2, startY + spacing, buttonWidth, buttonHeight, Text.translatable("framesync.config.enable_render_limit"), (button, enabled) -> {
            this.config.setEnableRenderLimit(enabled);
            this.applyConfigToLimiter();
        });
        this.addDrawableChild(this.enableButton);
        this.autoDetectButton = CyclingButtonWidget.onOffBuilder(Text.translatable("framesync.config.auto_on"), Text.translatable("framesync.config.auto_off"), this.config.autoDetectMonitorHz).build(centerX - buttonWidth / 2, startY + spacing * 2, buttonWidth, buttonHeight, Text.translatable("framesync.config.auto_detect"), (button, autoDetect) -> {
            this.config.setAutoDetectMonitorHz(autoDetect);
            if (autoDetect.booleanValue()) {
                int detectedHz = RenderLimiter.detectMonitorHz();
                this.config.setTargetMonitorHz(detectedHz);
                this.applyConfigToLimiter();
                this.init();
            }
        });
        this.addDrawableChild(this.autoDetectButton);
        this.minFpsSlider = new SliderWidget(centerX - buttonWidth / 2, startY + spacing * 3, buttonWidth, buttonHeight, (Text)Text.translatable((String)"framesync.config.min_fps", (Object[])new Object[]{this.config.minFpsLimit}), ((double)this.config.minFpsLimit - 15.0) / 105.0){

            protected void updateMessage() {
                int minFps = (int)(15.0 + this.value * 105.0);
                this.setMessage((Text)Text.translatable((String)"framesync.config.min_fps", (Object[])new Object[]{minFps}));
            }

            protected void applyValue() {
                int minFps = (int)(15.0 + this.value * 105.0);
                ConfigScreen.this.config.setMinFpsLimit(minFps);
                ConfigScreen.this.applyConfigToLimiter();
            }
        };
        this.addDrawableChild(this.minFpsSlider);
        this.fpsIndicatorButton = CyclingButtonWidget.onOffBuilder(Text.translatable("framesync.config.show"), Text.translatable("framesync.config.hide"), this.config.showFpsIndicator).build(centerX - buttonWidth / 2, startY + spacing * 4, buttonWidth, buttonHeight, Text.translatable("framesync.config.fps_indicator"), (button, show) -> this.config.setShowFpsIndicator(show));
        this.addDrawableChild(this.fpsIndicatorButton);
        this.addDrawableChild(ButtonWidget.builder(Text.translatable("framesync.config.reset_defaults"), button -> {
            this.config.resetToDefaults();
            this.applyConfigToLimiter();
            this.init();
        }).dimensions(centerX - buttonWidth / 2, startY + spacing * 6, buttonWidth, buttonHeight).build());
        this.addDrawableChild(ButtonWidget.builder(Text.translatable("gui.done"), button -> this.close()).dimensions(centerX - buttonWidth / 2, startY + spacing * 7, buttonWidth, buttonHeight).build());
        this.lowEndModeButton = CyclingButtonWidget.onOffBuilder(Text.translatable("framesync.config.lowend_on"), Text.translatable("framesync.config.lowend_off"), this.config.lowEndMode).build(centerX - buttonWidth / 2, startY + spacing * 5, buttonWidth, buttonHeight, Text.translatable("framesync.config.lowend_mode"), (button, lowEnd) -> {
            this.config.setLowEndMode(lowEnd);
            this.applyConfigToLimiter();
        });
        this.addDrawableChild(this.lowEndModeButton);
    }

    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 20, 0xFFFFFF);
        String configuredLimit = this.config.isUnlimitedFps() ? "Unlimited" : this.config.targetMonitorHz + " Hz";
        String info = String.format("Current: %s, %s", configuredLimit, this.config.enableRenderLimit ? "Enabled" : "Disabled");
        context.drawCenteredTextWithShadow(this.textRenderer, (Text)Text.literal((String)info), this.width / 2, this.height - 30, 0xAAAAAA);
        super.render(context, mouseX, mouseY, delta);
    }

    public void close() {
        this.client.setScreen(this.parent);
    }

    private void applyConfigToLimiter() {
        RenderManager.getInstance().updateSettings(this.config.getEffectiveHz(), this.config.enableRenderLimit);
    }
}
