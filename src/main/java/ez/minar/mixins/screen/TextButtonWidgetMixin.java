package ez.minar.mixins.screen;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ButtonWidget.Text.class)
public class TextButtonWidgetMixin {
    @Inject(method = "drawIcon", at = @At("HEAD"), cancellable = true)
    private void atheryx$drawBoldTitleButton(DrawContext context, int mouseX, int mouseY, float deltaTicks,
                                             CallbackInfo ci) {
        if (!(MinecraftClient.getInstance().currentScreen instanceof TitleScreen)) {
            return;
        }

        ci.cancel();
    }
}
