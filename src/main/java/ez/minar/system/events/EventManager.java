package ez.minar.system.events;

import ez.minar.system.api.Function;
import ez.minar.system.api.FunctionManager;
import ez.minar.system.events.impl.KeyEvent;
import ez.minar.system.events.impl.UpdateEvent;
import ez.minar.system.menu.ClickGui;
import ez.minar.system.settings.impl.KeybindSetting;
import net.minecraft.client.MinecraftClient;
import org.lwjgl.glfw.GLFW;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public class EventManager {
    MinecraftClient mc = MinecraftClient.getInstance();
    public static ClickGui clickGui = new ClickGui();
    private final Map<Function, Boolean> mouseBindStates = new HashMap<>();

    public static void call(Object event) {
        for (Function function : FunctionManager.getFunctions()) {
            if (function == null || !function.isEnabled()) continue;

            for (Method method : function.getClass().getDeclaredMethods()) {
                if (!method.isAnnotationPresent(EventHandler.class)) continue;

                Class<?>[] params = method.getParameterTypes();
                if (params.length != 1) continue;
                if (!params[0].isAssignableFrom(event.getClass())) continue;

                try {
                    method.setAccessible(true);
                    method.invoke(function, event);
                } catch (Exception ignored) {
                }
            }
        }
    }

    @EventHandler
    public void Events(KeyEvent event) {
        int key = event.getInput().key();
        int action = event.getAction();

        if (key == GLFW.GLFW_KEY_RIGHT_SHIFT && action == GLFW.GLFW_PRESS) {
            if (mc.currentScreen == clickGui) {
                clickGui.close();
            } else {
                mc.setScreen(clickGui);
            }
        }

        if (mc.currentScreen == null && action == GLFW.GLFW_PRESS) {
            for (Function function : FunctionManager.getFunctions()) {
                if (function.getKeybind() == key && function.getKeybind() > 0) {
                    function.toggle();
                }
            }
        }
    }

    @EventHandler
    public void onUpdate(UpdateEvent event) {
        if (mc.currentScreen != null || mc.getWindow() == null) {
            mouseBindStates.clear();
            return;
        }

        long window = mc.getWindow().getHandle();
        for (Function function : FunctionManager.getFunctions()) {
            int keybind = function.getKeybind();
            if (!KeybindSetting.isMouseKey(keybind)) {
                mouseBindStates.remove(function);
                continue;
            }

            boolean down = GLFW.glfwGetMouseButton(window, KeybindSetting.getMouseButton(keybind)) == GLFW.GLFW_PRESS;
            boolean wasDown = mouseBindStates.getOrDefault(function, false);
            if (down && !wasDown) {
                function.toggle();
            }
            mouseBindStates.put(function, down);
        }
    }
}
