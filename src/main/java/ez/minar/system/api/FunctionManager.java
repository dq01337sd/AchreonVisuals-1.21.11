package ez.minar.system.api;

import ez.minar.system.features.misc.Ambience;
import ez.minar.system.features.misc.AntiAFK;
import ez.minar.system.features.misc.Bots;
import ez.minar.system.features.misc.Saturation;
import ez.minar.system.features.render.*;
import lombok.Getter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class FunctionManager {
    @Getter private static final List<Function> functions = new ArrayList<>();

    public static void init() {
        register(new HUD(), new Particles(), new FireFly(), new JumpCircles(), new TargetESP(), new BlockOverlay(), new Predictions(), new ShaderSky(), new SwingAnimations(), new ViewModel(), new BeautifulHands(), new HandChams(), new Fog(), new NoRender(), new Arrows(), new HitWave(), new FullBright(), new ChinaHat(), new ScoreboardHealth(), new LineGlyphs());

        register(new Ambience(), new AntiAFK(), new Bots(), new Saturation());
    }

    private static void register(Function... functionArray) {
        functions.addAll(Arrays.asList(functionArray));
    }

    @SuppressWarnings("unchecked")
    public static <T extends Function> T getFunction(Class<T> clazz) {
        return (T) functions.stream()
                .filter(f -> f.getClass() == clazz)
                .findFirst()
                .orElse(null);
    }

    public static List<Function> getFunctionsByCategory(Category category) {
        return functions.stream()
                .filter(f -> f.getCategory() == category)
                .collect(Collectors.toList());
    }
}