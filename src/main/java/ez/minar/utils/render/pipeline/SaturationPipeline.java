package ez.minar.utils.render.pipeline;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.textures.TextureFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.GpuSampler;
import net.minecraft.client.gl.UniformType;
import net.minecraft.util.Identifier;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.util.OptionalDouble;
import java.util.OptionalInt;

public class SaturationPipeline {
    private static final int UNIFORM_SIZE = 64;

    private static RenderPipeline pipeline;
    private static GpuBuffer uniformBuffer;
    private static GpuTexture copyTexture;
    private static GpuTextureView copyTextureView;
    private static int lastWidth;
    private static int lastHeight;

    private SaturationPipeline() {
    }

    public static void init() {
        if (pipeline != null) return;

        pipeline = RenderPipeline.builder()
                .withLocation(Identifier.of("atheryx", "saturation"))
                .withVertexShader(Identifier.of("atheryx", "saturation_vertex"))
                .withFragmentShader(Identifier.of("atheryx", "saturation_fragment"))
                .withVertexFormat(VertexFormat.builder().build(), VertexFormat.DrawMode.TRIANGLES)
                .withUniform("Uniforms", UniformType.UNIFORM_BUFFER)
                .withSampler("Sampler0")
                .withBlend(BlendFunction.TRANSLUCENT)
                .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                .withDepthWrite(false)
                .withCull(false)
                .build();

        uniformBuffer = RenderSystem.getDevice().createBuffer(
                () -> "SaturationPipeline Uniforms",
                GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_COPY_DST,
                UNIFORM_SIZE);
    }

    public static void draw(float brightness, float saturation, float contrast, float hue) {
        MinecraftClient client = MinecraftClient.getInstance();
        Framebuffer framebuffer = client.getFramebuffer();
        if (framebuffer == null || framebuffer.getColorAttachment() == null) return;

        if (pipeline == null) init();
        if (pipeline == null || uniformBuffer == null) return;

        int fbWidth = framebuffer.textureWidth;
        int fbHeight = framebuffer.textureHeight;
        ensureCopyTexture(fbWidth, fbHeight);
        if (copyTexture == null || copyTextureView == null) return;

        ByteBuffer buffer = MemoryUtil.memAlloc(UNIFORM_SIZE);
        buffer.putFloat(brightness).putFloat(0f).putFloat(0f).putFloat(0f);
        buffer.putFloat(saturation).putFloat(0f).putFloat(0f).putFloat(0f);
        buffer.putFloat(contrast).putFloat(0f).putFloat(0f).putFloat(0f);
        buffer.putFloat(hue).putFloat(0f).putFloat(0f).putFloat(0f);
        buffer.flip();

        CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
        encoder.copyTextureToTexture(framebuffer.getColorAttachment(), copyTexture, 0, 0, 0, 0, 0, fbWidth, fbHeight);
        encoder.writeToBuffer(uniformBuffer.slice(), buffer);
        MemoryUtil.memFree(buffer);

        GpuSampler sampler = RenderSystem.getSamplerCache().get(FilterMode.LINEAR);
        try (RenderPass pass = encoder.createRenderPass(
                () -> "SaturationPipeline",
                framebuffer.getColorAttachmentView(),
                OptionalInt.empty(),
                framebuffer.getDepthAttachmentView(),
                OptionalDouble.of(1.0))) {
            pass.setPipeline(pipeline);
            pass.setUniform("Uniforms", uniformBuffer);
            pass.bindTexture("Sampler0", copyTextureView, sampler);
            pass.draw(0, 3);
        }
    }

    private static void ensureCopyTexture(int width, int height) {
        if (copyTexture != null && width == lastWidth && height == lastHeight) return;

        if (copyTextureView != null) {
            copyTextureView.close();
            copyTextureView = null;
        }
        if (copyTexture != null) {
            copyTexture.close();
            copyTexture = null;
        }

        copyTexture = RenderSystem.getDevice().createTexture(
                () -> "atheryx:saturation_copy",
                GpuTexture.USAGE_COPY_DST | GpuTexture.USAGE_TEXTURE_BINDING,
                TextureFormat.RGBA8,
                width, height, 1, 1);
        copyTextureView = RenderSystem.getDevice().createTextureView(copyTexture);
        lastWidth = width;
        lastHeight = height;
    }

    public static void shutdown() {
        if (uniformBuffer != null) {
            uniformBuffer.close();
            uniformBuffer = null;
        }
        if (copyTextureView != null) {
            copyTextureView.close();
            copyTextureView = null;
        }
        if (copyTexture != null) {
            copyTexture.close();
            copyTexture = null;
        }
        lastWidth = 0;
        lastHeight = 0;
    }
}