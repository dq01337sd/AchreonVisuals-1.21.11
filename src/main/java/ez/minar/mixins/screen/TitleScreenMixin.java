package ez.minar.mixins.screen;

import ez.minar.system.menu.TitleScreenMenuRenderer;
import ez.minar.utils.render.pipeline.TitleBackgroundPipeline;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.LogoDrawer;
import net.minecraft.client.gui.screen.SplashTextRenderer;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.PressableTextWidget;
import net.minecraft.client.gui.widget.TextIconButtonWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

@Mixin(TitleScreen.class)
public abstract class TitleScreenMixin extends Screen {
    protected TitleScreenMixin(Text title) {
        super(title);
    }

    @Redirect(method = "render",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/screen/TitleScreen;renderPanoramaBackground(Lnet/minecraft/client/gui/DrawContext;F)V"))
    private void atheryx$renderCustomBackground(TitleScreen screen, DrawContext context, float deltaTicks) {
        TitleBackgroundPipeline.draw();
    }

    @Inject(method = "render", at = @At("HEAD"))
    private void atheryx$captureContext(DrawContext context, int mouseX, int mouseY, float deltaTicks,
                                        CallbackInfo ci) {
        TitleScreenMenuRenderer.hideTitleScreenExtras((TitleScreen) (Object) this);
        TitleScreenMenuRenderer.setContext(context);
    }

    @Inject(method = "init", at = @At("RETURN"))
    private void atheryx$layoutButtons(CallbackInfo ci) {
        TitleScreen screen = (TitleScreen) (Object) this;
        List<ButtonWidget.Text> buttons = new ArrayList<>();

        TitleScreenMenuRenderer.hideTitleScreenExtras(screen);

        for (Element element : screen.children()) {
            if (element instanceof TextIconButtonWidget iconButton) {
                iconButton.visible = false;
                iconButton.active = false;
            } else if (element instanceof PressableTextWidget textWidget) {
                textWidget.visible = false;
                textWidget.active = false;
            } else if (element instanceof ButtonWidget.Text textButton) {
                buttons.add(textButton);
            }
        }

        int centerX = screen.width / 2;
        int btnW = 72;
        int btnH = 59;
        int gap = 16;
        int y = screen.height / 2 + 30;

        ButtonWidget altManagerBtn = ButtonWidget.builder(net.minecraft.text.Text.literal("Открыть альтменеджер"), btn -> {
            net.minecraft.client.MinecraftClient.getInstance().setScreen(new ez.minar.system.ui.alts.AltManagerScreen(screen));
        }).dimensions(0, 0, btnW, btnH).build();
        
        this.addDrawableChild(altManagerBtn);
        
        List<ButtonWidget> layoutButtons = new ArrayList<>();
        if (buttons.size() >= 5) {
            layoutButtons.add(buttons.get(0)); // singleplayer
            layoutButtons.add(buttons.get(1)); // multiplayer
            layoutButtons.add(altManagerBtn);  // alt manager
            layoutButtons.add(buttons.get(3)); // options
            layoutButtons.add(buttons.get(4)); // quit
            
            buttons.get(2).visible = false; // hide realms
            buttons.get(2).active = false;
        } else {
            layoutButtons.add(altManagerBtn);
        }

        int rowWidth = btnW * layoutButtons.size() + gap * (layoutButtons.size() - 1);
        int x = centerX - rowWidth / 2;

        for (int i = 0; i < layoutButtons.size(); i++) {
            layoutButtons.get(i).setDimensionsAndPosition(btnW, btnH, x + (btnW + gap) * i, y);
        }

        String[] icons = {"P", "N", "U", "O", "Q"};
        for (int i = 0; i < layoutButtons.size(); i++) {
            TitleScreenMenuRenderer.registerButtonIcon(layoutButtons.get(i), icons[i]);
        }
    }

    @Redirect(method = "render",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/LogoDrawer;draw(Lnet/minecraft/client/gui/DrawContext;IF)V"))
    private void atheryx$hideMinecraftLogo(LogoDrawer logoDrawer, DrawContext context, int screenWidth, float alpha) {
    }

    @Redirect(method = "render",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/screen/SplashTextRenderer;render(Lnet/minecraft/client/gui/DrawContext;ILnet/minecraft/client/font/TextRenderer;F)V"))
    private void atheryx$hideSplashText(SplashTextRenderer splashText, DrawContext context, int screenWidth,
                                        TextRenderer textRenderer, float alpha) {
    }

    @Redirect(method = "render",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/DrawContext;drawTextWithShadow(Lnet/minecraft/client/font/TextRenderer;Ljava/lang/String;III)V"))
    private void atheryx$hideVersionText(DrawContext context, TextRenderer textRenderer, String text, int x, int y, int color) {
    }
}
