package ez.minar.system.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import ez.minar.system.managers.ConfigManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.minecraft.command.CommandSource;
import net.minecraft.util.Formatting;

import java.util.Locale;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

public final class MinarClientCommands {
    private MinarClientCommands() {
    }

    public static void init() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> dispatcher.register(
                literal(".cfg")
                        .then(literal("dir").executes(context -> {
                            openDirectory();
                            return Command.SINGLE_SUCCESS;
                        }))
                        .then(literal("save")
                                .executes(context -> {
                                    save(null);
                                    return Command.SINGLE_SUCCESS;
                                })
                                .then(argument("name", StringArgumentType.word())
                                        .executes(context -> {
                                            save(StringArgumentType.getString(context, "name"));
                                            return Command.SINGLE_SUCCESS;
                                        })))
                        .then(literal("load")
                                .executes(context -> {
                                    load(null);
                                    return Command.SINGLE_SUCCESS;
                                })
                                .then(argument("name", StringArgumentType.word())
                                        .suggests((context, builder) -> CommandSource.suggestMatching(ConfigManager.getConfigNames(), builder))
                                        .executes(context -> {
                                            load(StringArgumentType.getString(context, "name"));
                                            return Command.SINGLE_SUCCESS;
                                        })))
                        .then(literal("reset").executes(context -> {
                            ConfigManager.reset();
                            CommandFeedback.message("Settings reset.", Formatting.GREEN);
                            return Command.SINGLE_SUCCESS;
                        }))
        ));

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> dispatcher.register(
                literal(".friend")
                        .then(literal("add")
                                .then(argument("nick", StringArgumentType.word())
                                        .executes(context -> executeFriend("add", StringArgumentType.getString(context, "nick")))))
                        .then(literal("remove")
                                .then(argument("nick", StringArgumentType.word())
                                        .executes(context -> executeFriend("remove", StringArgumentType.getString(context, "nick")))))
                        .then(literal("clear").executes(context -> {
                            FriendCommand.executeIfCommand(".friend clear");
                            return Command.SINGLE_SUCCESS;
                        }))
        ));



        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> dispatcher.register(
                literal(".bot")
                        .then(literal("list").executes(context -> {
                            BotCommand.executeIfCommand(".bot list");
                            return Command.SINGLE_SUCCESS;
                        }))
                        .then(literal("return").executes(context -> {
                            BotCommand.executeIfCommand(".bot return");
                            return Command.SINGLE_SUCCESS;
                        }))
                        .then(literal("connect")
                                .then(argument("name", StringArgumentType.word())
                                        .then(argument("ip", StringArgumentType.string())
                                                .executes(context -> {
                                                    BotCommand.executeIfCommand(".bot connect " + StringArgumentType.getString(context, "name") + " " + StringArgumentType.getString(context, "ip"));
                                                    return Command.SINGLE_SUCCESS;
                                                }))))
                        .then(literal("remove")
                                .then(argument("name", StringArgumentType.word())
                                        .executes(context -> {
                                            BotCommand.executeIfCommand(".bot remove " + StringArgumentType.getString(context, "name"));
                                            return Command.SINGLE_SUCCESS;
                                        })))
                        .then(literal("control")
                                .then(argument("name", StringArgumentType.word())
                                        .executes(context -> {
                                            BotCommand.executeIfCommand(".bot control " + StringArgumentType.getString(context, "name"));
                                            return Command.SINGLE_SUCCESS;
                                        })))
                        .then(literal("say")
                                .then(argument("name", StringArgumentType.word())
                                        .then(argument("message", StringArgumentType.greedyString())
                                                .executes(context -> {
                                                    BotCommand.executeIfCommand(".bot say " + StringArgumentType.getString(context, "name") + " " + StringArgumentType.getString(context, "message"));
                                                    return Command.SINGLE_SUCCESS;
                                                }))))
                        .then(literal("sayall")
                                .then(argument("message", StringArgumentType.greedyString())
                                        .executes(context -> {
                                            BotCommand.executeIfCommand(".bot sayall " + StringArgumentType.getString(context, "message"));
                                            return Command.SINGLE_SUCCESS;
                                        })))
        ));

ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> dispatcher.register(
                literal(".parse").executes(context -> {
                    ParseCommand.parsePlayers();
                    return Command.SINGLE_SUCCESS;
                })
        ));
    }

    private static int executeFriend(String action, String name) {
        FriendCommand.executeIfCommand(".friend " + action + " " + name);
        return Command.SINGLE_SUCCESS;
    }

    private static void openDirectory() {
        if (ConfigManager.openDirectory()) {
            CommandFeedback.message("Config folder opened.", Formatting.GREEN);
        } else {
            CommandFeedback.message("Could not open config folder.", Formatting.RED);
        }
    }

    private static void load(String name) {
        if (name != null && !ConfigManager.isValidConfigName(name)) {
            CommandFeedback.message("Invalid config name. Use letters, numbers, _ or -.", Formatting.YELLOW);
            return;
        }

        if (ConfigManager.load(name)) {
            CommandFeedback.message(configLabel(name) + " loaded.", Formatting.GREEN);
        } else {
            CommandFeedback.message(configLabel(name) + " was not found or could not be loaded.", Formatting.RED);
        }
    }

    private static void save(String name) {
        if (name != null && !ConfigManager.isValidConfigName(name)) {
            CommandFeedback.message("Invalid config name. Use letters, numbers, _ or -.", Formatting.YELLOW);
            return;
        }

        if (ConfigManager.save(name)) {
            CommandFeedback.message(configLabel(name) + " saved.", Formatting.GREEN);
        } else {
            CommandFeedback.message("Could not save " + configLabel(name).toLowerCase(Locale.ROOT) + ".", Formatting.RED);
        }
    }

    private static String configLabel(String name) {
        return name == null ? "Config" : "Config '" + name + "'";
    }
}
