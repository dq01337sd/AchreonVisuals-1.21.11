package ez.minar.utils.math;

public class Easings {
    public static float OutBack(float x) {
        float c1 = 1.70158f;
        float c3 = c1 + 1.0f;
        return (float) (1.0 + c3 * Math.pow(x - 1.0, 3.0) + c1 * Math.pow(x - 1.0, 2.0));
    }

    public static float OutCubic(float x) {
        return (float) (1.0 - Math.pow(1.0 - x, 3.0));
    }
}
