package ez.minar.system.features.render;

import ez.minar.system.api.Category;
import ez.minar.system.api.Function;
import ez.minar.system.api.NewFunction;
import ez.minar.system.settings.impl.BooleanSetting;

@NewFunction(name = "NoRender", desc = "Отключает выбранные визуальные эффекты", category = Category.RENDER)
public class NoRender extends Function {
    public static NoRender Instance;

    private final BooleanSetting vignette = new BooleanSetting("Виньетка", true);
    private final BooleanSetting badEffects = new BooleanSetting("Плохие эффекты", true);
    private final BooleanSetting fireOverlay = new BooleanSetting("Огонь на весь экран", true);
    private final BooleanSetting hurtCamera = new BooleanSetting("Тряска камеры", true);
    private final BooleanSetting crosshair = new BooleanSetting("Скрыть прицел", false);
    private final BooleanSetting particles = new BooleanSetting("Все частицы", false);

    public NoRender() {
        Instance = this;
        addSettings(vignette, badEffects, fireOverlay, hurtCamera, crosshair, particles);
    }

    public static boolean shouldDisableVignette() {
        return Instance != null && Instance.isEnabled() && Instance.vignette.isEnabled();
    }

    public static boolean shouldDisableBadEffects() {
        return Instance != null && Instance.isEnabled() && Instance.badEffects.isEnabled();
    }

    public static boolean shouldDisableFireOverlay() {
        return Instance != null && Instance.isEnabled() && Instance.fireOverlay.isEnabled();
    }

    public static boolean shouldDisableHurtCamera() {
        return Instance != null && Instance.isEnabled() && Instance.hurtCamera.isEnabled();
    }

    public static boolean shouldDisableCrosshair() {
        return Instance != null && Instance.isEnabled() && Instance.crosshair.isEnabled();
    }

    public static boolean shouldDisableParticles() {
        return Instance != null && Instance.isEnabled() && Instance.particles.isEnabled();
    }
}
