package ez.minar.utils.render.pipeline;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gl.UniformType;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Identifier;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.util.OptionalDouble;
import java.util.OptionalInt;

public class TitleBackgroundPipeline {
    private static final Identifier BACKGROUND_TEXTURE = Identifier.of("atheryx", "images/background.png");

    private TitleBackgroundPipeline() {
    }

    public static void draw() {
        MinecraftClient client = MinecraftClient.getInstance();
        float sw = ez.minar.utils.render.RenderUtil.getFixedScaledWidth();
        float sh = ez.minar.utils.render.RenderUtil.getFixedScaledHeight();
        float screenAspect = sw / sh;
        float imageAspect = 1920f / 1080f;
        
        float drawW, drawH, drawX, drawY;
        if (screenAspect > imageAspect) {
            drawW = sw;
            drawH = sw / imageAspect;
            drawX = 0;
            drawY = (sh - drawH) / 2f;
        } else {
            drawW = sh * imageAspect;
            drawH = sh;
            drawX = (sw - drawW) / 2f;
            drawY = 0;
        }
        
        var tex = client.getTextureManager().getTexture(BACKGROUND_TEXTURE);
        if (tex != null) {
            ez.minar.utils.render.pipeline.TexturePipeline.draw(
                ez.minar.utils.render.RenderUtil.createProjection(),
                drawX, drawY, drawW, drawH,
                tex.getGlTextureView(),
                java.awt.Color.WHITE.getRGB(),
                0f, 0f
            );
        }
    }

    public static void shutdown() {
    }
}
