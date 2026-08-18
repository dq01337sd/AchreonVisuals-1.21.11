package ez.minar.system.features.misc;

import ez.minar.system.api.Category;
import ez.minar.system.api.Function;
import ez.minar.system.api.NewFunction;
import ez.minar.system.events.EventHandler;
import ez.minar.system.events.impl.Render2DEvent;
import ez.minar.system.settings.impl.NumberSetting;
import ez.minar.utils.render.pipeline.SaturationPipeline;

@NewFunction(name = "Saturation", desc = "Цветокоррекция: яркость, насыщенность, контраст, HUE", category = Category.MISC)
public class Saturation extends Function {
    private final NumberSetting brightness = new NumberSetting("Brightness", 0.0, -1.0, 1.0, 0.05);
    private final NumberSetting saturation = new NumberSetting("Saturation", 1.0, 0.0, 2.0, 0.05);
    private final NumberSetting contrast = new NumberSetting("Contrast", 1.0, 0.0, 2.0, 0.05);
    private final NumberSetting hue = new NumberSetting("Hue", 0.0, -180.0, 180.0, 5.0);

    public Saturation() {
        addSettings(brightness, saturation, contrast, hue);
    }

    @EventHandler
    public void onRender(Render2DEvent event) {
        SaturationPipeline.draw(
                (float) brightness.getValue(),
                (float) saturation.getValue(),
                (float) contrast.getValue(),
                (float) hue.getValue());
    }
}