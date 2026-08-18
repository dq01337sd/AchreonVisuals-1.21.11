package ez.minar.system.features.misc;

import ez.minar.system.api.Category;
import ez.minar.system.api.Function;
import ez.minar.system.api.NewFunction;
import ez.minar.system.events.EventHandler;
import ez.minar.system.events.impl.KeyEvent;
import ez.minar.system.settings.impl.KeybindSetting;
import ez.minar.system.ui.bots.BotsWheelScreen;
import org.lwjgl.glfw.GLFW;

@NewFunction(name = "Bots", desc = "Управление ботами через GUI", category = Category.PLAYER)
public class Bots extends Function {

    private final KeybindSetting guiBind = new KeybindSetting("Бинд GUI", GLFW.GLFW_KEY_UNKNOWN);

    public Bots() {
        addSettings(guiBind);
    }

    @EventHandler
    public void onKey(KeyEvent event) {
        if (event.getAction() != GLFW.GLFW_PRESS) return;
        
        if (guiBind.matchesKey(event.getInput().key())) {
            if (mc.currentScreen == null) {
                mc.setScreen(new BotsWheelScreen());
            }
        }
    }
}
