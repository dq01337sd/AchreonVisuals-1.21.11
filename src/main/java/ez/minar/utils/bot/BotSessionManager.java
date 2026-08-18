package ez.minar.utils.bot;

import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.ReferenceCountUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.multiplayer.ConnectScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.network.ClientConnection;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.session.Session;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.network.packet.c2s.common.KeepAliveC2SPacket;
import net.minecraft.network.packet.c2s.play.TeleportConfirmC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.s2c.common.DisconnectS2CPacket;
import net.minecraft.network.packet.s2c.common.KeepAliveS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.network.packet.s2c.play.HealthUpdateS2CPacket;
import net.minecraft.network.packet.s2c.common.CommonPingS2CPacket;
import net.minecraft.network.packet.c2s.common.CommonPongC2SPacket;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import ez.minar.mixins.interfaces.IMinecraftClient;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public final class BotSessionManager {
    public static final List<BotConnection> connections = new ArrayList<>();
    private static final Map<BotConnection, Integer> botTickCounters = new WeakHashMap<>();

    static {
        ez.minar.system.events.EventBus.register(new Object() {
            @ez.minar.system.events.EventHandler
            public void onUpdate(ez.minar.system.events.impl.UpdateEvent event) {
                for (BotConnection bot : connections) {
                    if (bot.player() != null && bot.handler() != null && bot.connection() != null && bot.connection().isOpen()) {
                        int ticks = botTickCounters.getOrDefault(bot, 0) + 1;
                        if (ticks >= 20) {
                            bot.handler().getConnection().send(new PlayerMoveC2SPacket.OnGroundOnly(bot.player().isOnGround(), bot.player().horizontalCollision));
                            ticks = 0;
                        }
                        botTickCounters.put(bot, ticks);
                    }
                }
            }
        });
    }

    public static List<BotConnection> getConnections() {
        return new ArrayList<>(connections);
    }

    public static void connect(String name, String address) {
        MinecraftClient mc = MinecraftClient.getInstance();
        freezeCurrentSession();
        ((IMinecraftClient) mc).setSession(createSessionWithName(mc.getSession(), name));
        mc.execute(() -> ConnectScreen.connect(new MultiplayerScreen(new TitleScreen()), mc, ServerAddress.parse(address), new ServerInfo(address, address, ServerInfo.ServerType.OTHER), false, null));
        connections.removeIf(b -> b.name().equalsIgnoreCase(name));
    }

    public static void pulseBots(boolean rightClick) {
        for (BotConnection bot : connections) {
            if (bot.handler() == null || bot.player() == null) continue;
            if (rightClick) {
                bot.handler().sendPacket(new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, 0, bot.player().getYaw(), bot.player().getPitch()));
            } else {
                bot.handler().sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
            }
        }
    }

    public static void sayAll(String message) {
        for (BotConnection bot : connections) {
            if (bot.handler() == null) continue;
            if (message.startsWith("/")) {
                bot.handler().sendChatCommand(message.substring(1));
            } else {
                bot.handler().sendChatMessage(message);
            }
        }
    }

    public static boolean control(String name) {
        MinecraftClient mc = MinecraftClient.getInstance();
        return connections.stream().filter(b -> b.name().equalsIgnoreCase(name)).findFirst().map(bot -> {
            freezeCurrentSession();
            connections.remove(bot);
            io.netty.channel.Channel ch = getChannel(bot.connection());
            if (ch != null && ch.pipeline().get("bot_filter") != null) ch.pipeline().remove("bot_filter");

            mc.world = bot.world();
            mc.player = bot.player();
            mc.setCameraEntity(bot.player());
            mc.interactionManager = bot.interactionManager();

            try {
                for (Field f : MinecraftClient.class.getDeclaredFields()) {
                    if (ClientPlayNetworkHandler.class.isAssignableFrom(f.getType()) || f.getType().isAssignableFrom(ClientPlayNetworkHandler.class)) {
                        if (f.getType() != Object.class) {
                            f.setAccessible(true);
                            f.set(mc, bot.handler());
                            break;
                        }
                    }
                }
            } catch (Exception ignored) {}

            if (mc.worldRenderer != null) {
                mc.worldRenderer.setWorld(bot.world());
                mc.worldRenderer.reload();
            }

            ((IMinecraftClient) mc).setSession(createSessionWithName(mc.getSession(), bot.name()));
            mc.setScreen(null);
            return true;
        }).orElse(false);
    }

    private static void freezeCurrentSession() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.getNetworkHandler() != null && mc.world != null) {
            ClientPlayNetworkHandler h = mc.getNetworkHandler();
            makeNettyBot(h, mc.getSession().getUsername(), mc.player);
            connections.add(new BotConnection(mc.getSession().getUsername(), mc.getCurrentServerEntry() != null ? mc.getCurrentServerEntry().address : "", h.getConnection(), h, mc.world, mc.player, mc.interactionManager));

            mc.world = null;
            mc.player = null;
            try {
                for (Field f : MinecraftClient.class.getDeclaredFields()) {
                    if (ClientPlayNetworkHandler.class.isAssignableFrom(f.getType()) || f.getType().isAssignableFrom(ClientPlayNetworkHandler.class)) {
                        if (f.getType() != Object.class) {
                            f.setAccessible(true);
                            f.set(mc, null);
                            break;
                        }
                    }
                }
            } catch (Exception ignored) {}
        }
    }


    private static void makeNettyBot(ClientPlayNetworkHandler handler, String name, net.minecraft.client.network.ClientPlayerEntity botPlayer) {
        io.netty.channel.Channel channel = getChannel(handler.getConnection());
        if (channel == null) return;
        channel.pipeline().addBefore("packet_handler", "bot_filter", new io.netty.channel.ChannelDuplexHandler() {
            @Override
            public void channelRead(io.netty.channel.ChannelHandlerContext ctx, Object msg) throws Exception {
                if (msg instanceof net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket p) {
                    MinecraftClient.getInstance().execute(() -> {
                        handler.getConnection().send(new net.minecraft.network.packet.c2s.play.TeleportConfirmC2SPacket(p.teleportId()));
                        if (botPlayer != null) {
                            double d = p.change().position().x;
                            double e = p.change().position().y;
                            double f = p.change().position().z;
                            float g = p.change().yaw();
                            float h = p.change().pitch();
                            if (p.relatives().contains(net.minecraft.network.packet.s2c.play.PositionFlag.X)) d += botPlayer.getX();
                            if (p.relatives().contains(net.minecraft.network.packet.s2c.play.PositionFlag.Y)) e += botPlayer.getY();
                            if (p.relatives().contains(net.minecraft.network.packet.s2c.play.PositionFlag.Z)) f += botPlayer.getZ();
                            if (p.relatives().contains(net.minecraft.network.packet.s2c.play.PositionFlag.Y_ROT)) g += botPlayer.getYaw();
                            if (p.relatives().contains(net.minecraft.network.packet.s2c.play.PositionFlag.X_ROT)) h += botPlayer.getPitch();

                            botPlayer.setPosition(d, e, f);
                            botPlayer.setYaw(g);
                            botPlayer.setPitch(h);

                            handler.getConnection().send(new net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.Full(
                                    botPlayer.getX(), botPlayer.getY(), botPlayer.getZ(),
                                    botPlayer.getYaw(), botPlayer.getPitch(),
                                    botPlayer.isOnGround(), botPlayer.horizontalCollision));
                            
                            // Reset tick counter for this bot since we just sent a position packet
                            for (BotConnection b : connections) {
                                if (b.player() == botPlayer) botTickCounters.put(b, 0);
                            }
                        }
                    });
                    io.netty.util.ReferenceCountUtil.release(msg); return;
                }

                if (msg instanceof net.minecraft.network.packet.s2c.play.HealthUpdateS2CPacket p) {
                    MinecraftClient.getInstance().execute(() -> {
                        if (botPlayer != null) botPlayer.setHealth(p.getHealth());
                    });
                    io.netty.util.ReferenceCountUtil.release(msg); return;
                }

                if (msg instanceof net.minecraft.network.packet.s2c.common.DisconnectS2CPacket) { 
                    connections.removeIf(b -> b.name().equalsIgnoreCase(name)); 
                    ctx.close(); 
                    io.netty.util.ReferenceCountUtil.release(msg); return; 
                }

                if (msg instanceof net.minecraft.network.packet.s2c.common.KeepAliveS2CPacket p) {
                    handler.getConnection().send(new net.minecraft.network.packet.c2s.common.KeepAliveC2SPacket(p.getId()));
                    io.netty.util.ReferenceCountUtil.release(msg); return;
                }

                if (msg instanceof net.minecraft.network.packet.s2c.common.CommonPingS2CPacket p) {
                    handler.getConnection().send(new net.minecraft.network.packet.c2s.common.CommonPongC2SPacket(p.getParameter()));
                    io.netty.util.ReferenceCountUtil.release(msg); return;
                }

                // Drop everything else to prevent ClientPlayNetworkHandler crashes
                io.netty.util.ReferenceCountUtil.release(msg);
            }
        });
    }

    public static boolean say(String n, String m) {
        return connections.stream().filter(b -> b.name().equalsIgnoreCase(n)).findFirst().map(b -> {
            if (m.startsWith("/")) b.handler().sendChatCommand(m.substring(1)); else b.handler().sendChatMessage(m);
            return true;
        }).orElse(false);
    }

    public static boolean remove(String n) {
        return connections.removeIf(b -> { if (b.name().equalsIgnoreCase(n)) { b.connection().disconnect(Text.literal("Removed")); return true; } return false; });
    }

    public static boolean restore() { return !connections.isEmpty() && control(connections.get(connections.size() - 1).name()); }

    private static Channel getChannel(ClientConnection c) {
        try { for (Field f : ClientConnection.class.getDeclaredFields()) { if (Channel.class.isAssignableFrom(f.getType())) { f.setAccessible(true); return (Channel) f.get(c); } } } catch (Exception ignored) {}
        return null;
    }

    private static Session createSessionWithName(Session current, String name) {
        try {
            Constructor<Session> c = Session.class.getDeclaredConstructor(String.class, UUID.class, String.class, Optional.class, Optional.class);
            c.setAccessible(true);
            return c.newInstance(name, UUID.randomUUID(), current.getAccessToken(), Optional.empty(), Optional.empty());
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    public record BotConnection(String name, String address, ClientConnection connection, ClientPlayNetworkHandler handler, ClientWorld world, ClientPlayerEntity player, ClientPlayerInteractionManager interactionManager) {}
}
