#version 330

layout(std140) uniform Uniforms {
    vec4 uScreen;
    vec4 uTime;
};

in vec2 vUV;
out vec4 fragColor;

float hash(vec2 p) {
    p = fract(p * vec2(127.1, 311.7));
    p += dot(p, p + 19.19);
    return fract(p.x * p.y);
}

float noise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    float a = hash(i);
    float b = hash(i + vec2(1.0, 0.0));
    float c = hash(i + vec2(0.0, 1.0));
    float d = hash(i + vec2(1.0, 1.0));
    return mix(mix(a, b, f.x), mix(c, d, f.x), f.y);
}

float fbm(vec2 p) {
    float v = 0.0;
    float a = 0.5;
    for (int i = 0; i < 5; i++) {
        v += noise(p) * a;
        p = mat2(1.62, 1.13, -1.13, 1.62) * p + vec2(4.7, 2.9);
        a *= 0.52;
    }
    return v;
}

float ridge(vec2 p) {
    float v = 0.0;
    float a = 0.55;
    for (int i = 0; i < 4; i++) {
        float n = 1.0 - abs(noise(p) * 2.0 - 1.0);
        v += n * n * a;
        p = p * 2.05 + vec2(5.3, 7.1);
        a *= 0.5;
    }
    return v;
}

float mountain(vec2 uv, float offset, float scale) {
    float h = offset + fbm(vec2(uv.x * scale, 1.8)) * 0.22;
    return smoothstep(h + 0.02, h - 0.02, uv.y);
}

void main() {
    vec2 uv = vUV;
    float aspect = uScreen.x / max(uScreen.y, 1.0);
    vec2 p = vec2((uv.x - 0.5) * aspect + 0.5, uv.y);
    float time = uTime.x;

    vec3 skyTop = vec3(0.015, 0.050, 0.073);
    vec3 skyBottom = vec3(0.115, 0.175, 0.150);
    vec3 color = mix(skyBottom, skyTop, smoothstep(0.12, 1.0, uv.y));

    float cloud = fbm(vec2(p.x * 2.1 + time * 0.015, p.y * 1.5 - time * 0.010));
    color += vec3(0.045, 0.065, 0.060) * smoothstep(0.42, 0.88, cloud) * (1.0 - uv.y);

    float farRange = mountain(p, 0.44, 2.7);
    color = mix(color, vec3(0.018, 0.075, 0.055), farRange * 0.62);

    float nearRange = mountain(p + vec2(time * 0.004, 0.0), 0.32, 4.2);
    color = mix(color, vec3(0.010, 0.050, 0.034), nearRange * 0.82);

    vec2 forestUv = vec2(p.x * 5.0, p.y * 2.8 + time * 0.018);
    float canopy = ridge(forestUv + fbm(forestUv * 0.55));
    float forestMask = smoothstep(0.56, 0.30, uv.y);
    vec3 forest = mix(vec3(0.008, 0.038, 0.022), vec3(0.030, 0.120, 0.060), canopy);
    color = mix(color, forest, forestMask * 0.92);

    float mistBand = smoothstep(0.20, 0.62, uv.y) * smoothstep(0.78, 0.38, uv.y);
    float mist = fbm(vec2(p.x * 2.8 - time * 0.025, p.y * 4.4 + 8.0));
    color = mix(color, vec3(0.44, 0.58, 0.50), mistBand * smoothstep(0.34, 0.78, mist) * 0.22);

    float sun = exp(-dot((p - vec2(0.28, 0.74)) * vec2(1.0, 1.35), (p - vec2(0.28, 0.74)) * vec2(1.0, 1.35)) * 8.0);
    color += vec3(0.20, 0.23, 0.15) * sun * 0.22;

    float edge = uv.x * uv.y * (1.0 - uv.x) * (1.0 - uv.y);
    float vignette = smoothstep(0.0, 0.095, edge);
    color *= mix(0.52, 1.02, vignette);

    color += (hash(gl_FragCoord.xy + time) - 0.5) / 255.0;
    fragColor = vec4(color, 1.0);
}
