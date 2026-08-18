package ez.minar.utils.math;

import java.util.Random;

public class PerlinNoise {
    private final int[] p = new int[512];

    public PerlinNoise() {
        this(System.currentTimeMillis());
    }

    public PerlinNoise(long seed) {
        Random random = new Random(seed);
        int[] permutation = new int[256];
        for (int i = 0; i < 256; i++) {
            permutation[i] = i;
        }

        for (int i = 0; i < 256; i++) {
            int j = random.nextInt(256 - i) + i;
            int temp = permutation[i];
            permutation[i] = permutation[j];
            permutation[j] = temp;
        }

        for (int i = 0; i < 256; i++) {
            p[i] = p[i + 256] = permutation[i];
        }
    }

    public double noise(double x) {
        return noise(x, 0.0, 0.0);
    }

    public double noise(double x, double y) {
        return noise(x, y, 0.0);
    }

    public double noise(double x, double y, double z) {
        int xIndex = (int) Math.floor(x) & 0xFF;
        int yIndex = (int) Math.floor(y) & 0xFF;
        int zIndex = (int) Math.floor(z) & 0xFF;
        x -= Math.floor(x);
        y -= Math.floor(y);
        z -= Math.floor(z);

        double u = fade(x);
        double v = fade(y);
        double w = fade(z);
        int a = p[xIndex] + yIndex;
        int aa = p[a] + zIndex;
        int ab = p[a + 1] + zIndex;
        int b = p[xIndex + 1] + yIndex;
        int ba = p[b] + zIndex;
        int bb = p[b + 1] + zIndex;

        return lerp(w,
                lerp(v,
                        lerp(u, grad(p[aa], x, y, z), grad(p[ba], x - 1.0, y, z)),
                        lerp(u, grad(p[ab], x, y - 1.0, z), grad(p[bb], x - 1.0, y - 1.0, z))),
                lerp(v,
                        lerp(u, grad(p[aa + 1], x, y, z - 1.0), grad(p[ba + 1], x - 1.0, y, z - 1.0)),
                        lerp(u, grad(p[ab + 1], x, y - 1.0, z - 1.0), grad(p[bb + 1], x - 1.0, y - 1.0, z - 1.0))));
    }

    private static double fade(double t) {
        return t * t * t * (t * (t * 6.0 - 15.0) + 10.0);
    }

    private static double lerp(double t, double a, double b) {
        return a + t * (b - a);
    }

    private static double grad(int hash, double x, double y, double z) {
        int h = hash & 15;
        double u = h < 8 ? x : y;
        double v = h < 4 ? y : (h != 12 && h != 14 ? z : x);
        return ((h & 1) == 0 ? u : -u) + ((h & 2) == 0 ? v : -v);
    }
}
