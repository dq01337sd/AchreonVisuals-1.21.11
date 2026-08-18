package ez.minar.system.ui.bots;

import ez.minar.utils.bot.BotSessionManager;
import ez.minar.utils.bot.BotSessionManager.BotConnection;
import ez.minar.utils.math.Easings;
import ez.minar.utils.render.RenderUtil;
import ez.minar.utils.render.msdf.Msdf;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Click;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.awt.Color;
import java.util.List;

public class BotsWheelScreen extends Screen {

    private long startTime;
    private long lastFrameTime;
    private final java.util.Map<String, Float> hoverStates = new java.util.HashMap<>();
    private boolean isAddingBot = false;
    private String nameInput = "";
    private String ipInput = "";
    private boolean nameFocused = false;
    private boolean ipFocused = false;
    private int selectedBotIndex = -1;
    private boolean showContextMenu = false;
    private float contextMenuX = 0, contextMenuY = 0;
    private BotConnection contextMenuBot = null;

    private static final float ANIMATION_DURATION = 400f;

    public BotsWheelScreen() {
        super(Text.literal("Bots Wheel"));
    }

    @Override
    protected void init() {
        super.init();
        startTime = System.currentTimeMillis();
        lastFrameTime = System.currentTimeMillis();
        
        int centerX = RenderUtil.getFixedScaledWidth() / 2;
        int centerY = RenderUtil.getFixedScaledHeight() / 2;

        // Fields are now custom rendered
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        // override to remove default vanilla blur/dim
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        int scaledWidth = RenderUtil.getFixedScaledWidth();
        int scaledHeight = RenderUtil.getFixedScaledHeight();
        float mx = RenderUtil.convertX(mouseX);
        float my = RenderUtil.convertY(mouseY);

        long currentTime = System.currentTimeMillis();
        float deltaMs = (float) (currentTime - lastFrameTime);
        lastFrameTime = currentTime;

        float timePassed = currentTime - startTime;
        float progress = Math.min(1f, timePassed / ANIMATION_DURATION);
        float anim = Easings.OutBack(progress);

        float centerX = scaledWidth / 2f;
        float centerY = scaledHeight / 2f;
        float radius = 100f * anim;

        if (anim > 0.01f) {
            // Background wheel
            RenderUtil.shadow(centerX - radius, centerY - radius, radius * 2, radius * 2, radius, 20f, 0.4f, 2f, new Color(10, 10, 10, 200));
            RenderUtil.rect(centerX - radius, centerY - radius, radius * 2, radius * 2, radius, new Color(15, 15, 15, 230));

            // Central "+" button
            float plusSize = 30f * anim;
            boolean hoverPlus = mx > centerX - plusSize / 2 && mx < centerX + plusSize / 2 && my > centerY - plusSize / 2 && my < centerY + plusSize / 2;
            Color plusColor = hoverPlus ? new Color(60, 150, 255) : new Color(40, 120, 220);
            RenderUtil.rect(centerX - plusSize / 2, centerY - plusSize / 2, plusSize, plusSize, plusSize / 2, plusColor);
            RenderUtil.text(context, centerX, centerY - 6f * anim, "+", 18f * anim, Color.WHITE, "center");

            // Draw bots around
            List<BotConnection> bots = BotSessionManager.getConnections();
            int botCount = bots.size();
            for (int i = 0; i < botCount; i++) {
                BotConnection bot = bots.get(i);
                float angle = (float) (i * (Math.PI * 2) / botCount) - (float) Math.PI / 2;
                float botX = centerX + (float) Math.cos(angle) * (radius * 0.7f);
                float botY = centerY + (float) Math.sin(angle) * (radius * 0.7f);
                
                float baseSize = 24f * anim;
                boolean hoverBot = mx > botX - baseSize / 2 && mx < botX + baseSize / 2 && my > botY - baseSize / 2 && my < botY + baseSize / 2;
                
                float currentHover = hoverStates.getOrDefault(bot.name(), 0f);
                float targetHover = hoverBot ? 1f : 0f;
                currentHover += (targetHover - currentHover) * (deltaMs / 100f);
                currentHover = Math.max(0f, Math.min(1f, currentHover));
                hoverStates.put(bot.name(), currentHover);
                
                float hoverAnim = ez.minar.utils.math.Easings.OutBack(currentHover);
                float currentSize = baseSize + (12f * hoverAnim);
                
                net.minecraft.entity.player.SkinTextures skin = bot.player() != null 
                        ? bot.player().getSkin() 
                        : net.minecraft.client.util.DefaultSkinHelper.getSkinTextures(java.util.UUID.randomUUID());
                net.minecraft.util.Identifier texture = skin.body().texturePath();
                
                int alpha = (int)(255 * Math.max(0f, Math.min(1f, anim)));
                int colorRGB = new Color(255, 255, 255, alpha).getRGB();
                
                var view = client.getTextureManager().getTexture(texture).getGlTextureView();
                
                float hr = currentSize / 4f;
                ez.minar.utils.render.pipeline.TexturePipeline.draw(RenderUtil.createProjection(), botX - currentSize / 2, botY - currentSize / 2, currentSize, view, colorRGB, hr, 0f, 8f / 64f, 8f / 64f, 8f / 64f, 8f / 64f, false);
                ez.minar.utils.render.pipeline.TexturePipeline.draw(RenderUtil.createProjection(), botX - currentSize / 2, botY - currentSize / 2, currentSize, view, colorRGB, hr, 0f, 40f / 64f, 8f / 64f, 8f / 64f, 8f / 64f, false);
                
                if (currentHover > 0.01f) {
                    float flyY = botY + (currentSize / 2f + 8f) * hoverAnim;
                    int nameAlpha = (int)(255 * Math.max(0f, Math.min(1f, currentHover * 2f * anim)));
                    Color nameCol = new Color(255, 255, 255, nameAlpha);
                    RenderUtil.text(context, botX, flyY, bot.name(), Math.max(0f, 10f * anim), nameCol, "center");
                }
            }

            // Draw Inputs if Adding Bot
            if (isAddingBot) {
                // Dim background
                RenderUtil.rect(0, 0, scaledWidth, scaledHeight, 0, new Color(0, 0, 0, 150));
                
                float panelW = 160f;
                float panelH = 120f;
                float panelX = centerX - panelW / 2;
                float panelY = centerY - panelH / 2;
                
                RenderUtil.rect(panelX, panelY, panelW, panelH, 8f, new Color(20, 20, 20, 255));
                RenderUtil.text(context, centerX, panelY + 10, "Connect Bot", 14f, Color.WHITE, "center");

                // Custom Input Fields
                float fieldW = 120f;
                float fieldH = 20f;
                float nameFieldX = centerX - fieldW / 2;
                float nameFieldY = panelY + 30f;
                float ipFieldX = centerX - fieldW / 2;
                float ipFieldY = panelY + 60f;

                // Name field
                RenderUtil.rect(nameFieldX, nameFieldY, fieldW, fieldH, 4f, nameFocused ? new Color(40, 40, 45, 255) : new Color(30, 30, 35, 255));
                if (nameFocused) RenderUtil.outline(nameFieldX, nameFieldY, fieldW, fieldH, 4f, 1f, new Color(80, 160, 255));
                if (nameInput.isEmpty() && !nameFocused) {
                    RenderUtil.text(context, nameFieldX + 5, nameFieldY + 5, "Nickname", 10f, new Color(150, 150, 150));
                } else {
                    RenderUtil.text(context, nameFieldX + 5, nameFieldY + 5, nameInput + (nameFocused && (System.currentTimeMillis() / 500 % 2 == 0) ? "_" : ""), 10f, Color.WHITE);
                }

                // IP field
                RenderUtil.rect(ipFieldX, ipFieldY, fieldW, fieldH, 4f, ipFocused ? new Color(40, 40, 45, 255) : new Color(30, 30, 35, 255));
                if (ipFocused) RenderUtil.outline(ipFieldX, ipFieldY, fieldW, fieldH, 4f, 1f, new Color(80, 160, 255));
                if (ipInput.isEmpty() && !ipFocused) {
                    RenderUtil.text(context, ipFieldX + 5, ipFieldY + 5, "IP Address", 10f, new Color(150, 150, 150));
                } else {
                    RenderUtil.text(context, ipFieldX + 5, ipFieldY + 5, ipInput + (ipFocused && (System.currentTimeMillis() / 500 % 2 == 0) ? "_" : ""), 10f, Color.WHITE);
                }

                // Connect button
                float btnW = 80f;
                float btnH = 20f;
                float btnX = centerX - btnW / 2;
                float btnY = panelY + 90f;
                boolean hoverBtn = mx > btnX && mx < btnX + btnW && my > btnY && my < btnY + btnH;
                RenderUtil.rect(btnX, btnY, btnW, btnH, 4f, hoverBtn ? new Color(0, 150, 0) : new Color(0, 120, 0));
                RenderUtil.text(context, centerX, btnY + 5, "Connect", 10f, Color.WHITE, "center");
            }
            
            // Draw Context Menu
            if (showContextMenu && contextMenuBot != null) {
                float menuW = 80f;
                float menuH = 80f; // 4 buttons
                RenderUtil.rect(contextMenuX, contextMenuY, menuW, menuH, 4f, new Color(30, 30, 30, 255));
                
                String[] options = {"Control", "Say All", "Pulse", "Remove"};
                for (int i = 0; i < options.length; i++) {
                    float optY = contextMenuY + (i * 20f);
                    boolean hoverOpt = mx > contextMenuX && mx < contextMenuX + menuW && my > optY && my < optY + 20f;
                    if (hoverOpt) {
                        RenderUtil.rect(contextMenuX, optY, menuW, 20f, 0f, new Color(60, 60, 60, 255));
                    }
                    Color textColor = options[i].equals("Remove") ? new Color(255, 80, 80) : Color.WHITE;
                    RenderUtil.text(context, contextMenuX + menuW / 2, optY + 6f, options[i], 10f, textColor, "center");
                }
            }
        }
        
        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        float scaledWidth = RenderUtil.getFixedScaledWidth();
        float scaledHeight = RenderUtil.getFixedScaledHeight();
        double mouseX = click.x();
        double mouseY = click.y();
        int button = click.button();
        float mx = RenderUtil.convertX((float) mouseX);
        float my = RenderUtil.convertY((float) mouseY);
        float centerX = scaledWidth / 2f;
        float centerY = scaledHeight / 2f;

        if (showContextMenu) {
            float menuW = 80f;
            float menuH = 80f;
            if (mx >= contextMenuX && mx <= contextMenuX + menuW && my >= contextMenuY && my <= contextMenuY + menuH) {
                int optIndex = (int) ((my - contextMenuY) / 20f);
                if (optIndex == 0) BotSessionManager.control(contextMenuBot.name());
                if (optIndex == 1) client.player.sendMessage(Text.literal("Чат от имени бота в разработке..."), false);
                if (optIndex == 2) BotSessionManager.pulseBots(false);
                if (optIndex == 3) BotSessionManager.remove(contextMenuBot.name());
                showContextMenu = false;
                return true;
            } else {
                showContextMenu = false; // close on outside click
                return true;
            }
        }

        if (isAddingBot) {
            float panelW = 160f;
            float panelH = 120f;
            float panelX = centerX - panelW / 2;
            float panelY = centerY - panelH / 2;
            
            float btnW = 80f;
            float btnH = 20f;
            float btnX = centerX - btnW / 2;
            float btnY = panelY + 90f;
            
            if (mx > btnX && mx < btnX + btnW && my > btnY && my < btnY + btnH) {
                if (!nameInput.isEmpty() && !ipInput.isEmpty()) {
                    BotSessionManager.connect(nameInput, ipInput);
                    isAddingBot = false;
                }
                return true;
            }
            
            float fieldW = 120f;
            float fieldH = 20f;
            float nameFieldX = centerX - fieldW / 2;
            float nameFieldY = panelY + 30f;
            float ipFieldX = centerX - fieldW / 2;
            float ipFieldY = panelY + 60f;
            
            nameFocused = (mx >= nameFieldX && mx <= nameFieldX + fieldW && my >= nameFieldY && my <= nameFieldY + fieldH);
            ipFocused = (mx >= ipFieldX && mx <= ipFieldX + fieldW && my >= ipFieldY && my <= ipFieldY + fieldH);
            
            if (nameFocused || ipFocused) return true;
            
            // Click outside panel to close
            if (mx < panelX || mx > panelX + panelW || my < panelY || my > panelY + panelH) {
                isAddingBot = false;
            }
            return true;
        }

        float radius = 100f;
        float plusSize = 30f;
        
        if (mx > centerX - plusSize / 2 && mx < centerX + plusSize / 2 && my > centerY - plusSize / 2 && my < centerY + plusSize / 2) {
            if (button == 0) {
                isAddingBot = true;
                return true;
            }
        }

        List<BotConnection> bots = BotSessionManager.getConnections();
        int botCount = bots.size();
        for (int i = 0; i < botCount; i++) {
            BotConnection bot = bots.get(i);
            float angle = (float) (i * (Math.PI * 2) / botCount) - (float) Math.PI / 2;
            float botX = centerX + (float) Math.cos(angle) * (radius * 0.7f);
            float botY = centerY + (float) Math.sin(angle) * (radius * 0.7f);
            float botSize = 24f;
            
            if (mx > botX - botSize / 2 && mx < botX + botSize / 2 && my > botY - botSize / 2 && my < botY + botSize / 2) {
                if (button == 1) { // Right click
                    showContextMenu = true;
                    contextMenuX = mx;
                    contextMenuY = my;
                    contextMenuBot = bot;
                } else if (button == 0) { // Left click = control
                    BotSessionManager.control(bot.name());
                }
                return true;
            }
        }

        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        if (input.key() == GLFW.GLFW_KEY_ESCAPE) {
            if (showContextMenu) {
                showContextMenu = false;
                return true;
            }
            if (isAddingBot) {
                isAddingBot = false;
                return true;
            }
            this.close();
            return true;
        }
        if (isAddingBot) {
            if (input.key() == GLFW.GLFW_KEY_BACKSPACE) {
                if (nameFocused && nameInput.length() > 0) {
                    nameInput = nameInput.substring(0, nameInput.length() - 1);
                    return true;
                }
                if (ipFocused && ipInput.length() > 0) {
                    ipInput = ipInput.substring(0, ipInput.length() - 1);
                    return true;
                }
            } else if (input.key() == GLFW.GLFW_KEY_ENTER) {
                if (nameFocused) {
                    nameFocused = false;
                    ipFocused = true;
                    return true;
                } else if (ipFocused) {
                    if (!nameInput.isEmpty() && !ipInput.isEmpty()) {
                        BotSessionManager.connect(nameInput, ipInput);
                        isAddingBot = false;
                    }
                    return true;
                }
            } else if (input.key() == GLFW.GLFW_KEY_V && (GLFW.glfwGetKey(client.getWindow().getHandle(), GLFW.GLFW_KEY_LEFT_CONTROL) == GLFW.GLFW_PRESS || GLFW.glfwGetKey(client.getWindow().getHandle(), GLFW.GLFW_KEY_RIGHT_CONTROL) == GLFW.GLFW_PRESS)) {
                String clipboard = client.keyboard.getClipboard();
                if (nameFocused && nameInput.length() < 16) {
                    nameInput += clipboard;
                    if (nameInput.length() > 16) nameInput = nameInput.substring(0, 16);
                }
                if (ipFocused && ipInput.length() < 64) {
                    ipInput += clipboard;
                    if (ipInput.length() > 64) ipInput = ipInput.substring(0, 64);
                }
                return true;
            }
        }
        return super.keyPressed(input);
    }

    @Override
    public boolean charTyped(CharInput input) {
        if (isAddingBot) {
            if (input.isValidChar()) {
                if (nameFocused && nameInput.length() < 16) {
                    nameInput += input.asString();
                    return true;
                }
                if (ipFocused && ipInput.length() < 64) {
                    ipInput += input.asString();
                    return true;
                }
            }
        }
        return super.charTyped(input);
    }
    
    @Override
    public boolean shouldPause() {
        return false;
    }
}
