package ez.minar.system.commands;

import ez.minar.utils.bot.BotSessionManager;
import net.minecraft.util.Formatting;

import java.util.List;
import java.util.Locale;

public final class BotCommand {
    public static final String COMMAND = "bot";

    private BotCommand() {
    }

    public static boolean executeIfCommand(String input) {
        String commandLine = input.trim();
        if (!isBotCommand(commandLine)) {
            return false;
        }

        String[] arguments = commandLine.split("\\s+");
        if (arguments.length < 2) {
            showUsage();
            return true;
        }

        switch (arguments[1].toLowerCase(Locale.ROOT)) {
            case "connect" -> connect(arguments);
            case "remove" -> remove(arguments);
            case "return" -> restore(arguments);
            case "control" -> control(arguments);
            case "say" -> say(arguments);
            case "sayall" -> sayAll(arguments);
            case "list" -> list(arguments);
            default -> showUsage();
        }
        return true;
    }

    public static List<String> suggestionsFor(String input) {
        String commandLine = input.stripLeading().toLowerCase(Locale.ROOT);
        if (".".equals(commandLine) || (commandLine.startsWith(".") && ("." + COMMAND).startsWith(commandLine))) {
            return List.of(".bot");
        }
        if (!commandLine.startsWith(".bot ")) {
            return List.of();
        }

        String attribute = commandLine.substring(".bot ".length());
        if (attribute.contains(" ") && !attribute.toLowerCase(Locale.ROOT).startsWith("say ") && !attribute.toLowerCase(Locale.ROOT).startsWith("sayall ") && !attribute.toLowerCase(Locale.ROOT).startsWith("connect ")) {
            return List.of();
        }

        if (!attribute.contains(" ")) {
            return List.of(".bot connect <name> <ip>", ".bot remove <name>", ".bot return", ".bot control <name>", ".bot say <name> <msg>", ".bot sayall <msg>", ".bot list").stream()
                    .filter(suggestion -> suggestion.substring(".bot ".length())
                            .toLowerCase(Locale.ROOT).startsWith(attribute))
                    .toList();
        }
        
        return List.of();
    }

    private static void connect(String[] arguments) {
        if (arguments.length != 4) {
            CommandFeedback.message("Usage: .bot connect <name> <ip>", Formatting.YELLOW);
            return;
        }
        String name = arguments[2];
        String ip = arguments[3];
        BotSessionManager.connect(name, ip);
        CommandFeedback.message("Connected: " + name + " -> " + ip + " (Previous session frozen)", Formatting.GREEN);
    }

    private static void remove(String[] arguments) {
        if (arguments.length != 3) {
            CommandFeedback.message("Usage: .bot remove <name>", Formatting.YELLOW);
            return;
        }
        String name = arguments[2];
        if (BotSessionManager.remove(name)) {
            CommandFeedback.message("Bot disconnected and removed: " + name, Formatting.GREEN);
        } else {
            CommandFeedback.message("Bot not found: " + name, Formatting.RED);
        }
    }

    private static void control(String[] arguments) {
        if (arguments.length != 3) {
            CommandFeedback.message("Usage: .bot control <name>", Formatting.YELLOW);
            return;
        }
        String name = arguments[2];
        if (BotSessionManager.control(name)) {
            CommandFeedback.message("Switched to bot: " + name, Formatting.GREEN);
        } else {
            CommandFeedback.message("Bot not found: " + name, Formatting.RED);
        }
    }

    private static void say(String[] arguments) {
        if (arguments.length < 4) {
            CommandFeedback.message("Usage: .bot say <name> <message>", Formatting.YELLOW);
            return;
        }
        String name = arguments[2];
        StringBuilder message = new StringBuilder();
        for (int i = 3; i < arguments.length; i++) {
            message.append(arguments[i]).append(" ");
        }
        if (BotSessionManager.say(name, message.toString().trim())) {
            CommandFeedback.message("Message from " + name + " sent.", Formatting.GREEN);
        } else {
            CommandFeedback.message("Bot not found: " + name, Formatting.RED);
        }
    }

    private static void sayAll(String[] arguments) {
        if (arguments.length < 3) {
            CommandFeedback.message("Usage: .bot sayall <message>", Formatting.YELLOW);
            return;
        }
        StringBuilder message = new StringBuilder();
        for (int i = 2; i < arguments.length; i++) {
            message.append(arguments[i]).append(" ");
        }
        BotSessionManager.sayAll(message.toString().trim());
        CommandFeedback.message("Message sent from all bots.", Formatting.GREEN);
    }

    private static void restore(String[] arguments) {
        if (BotSessionManager.restore()) {
            CommandFeedback.message("Returned to previous session", Formatting.GREEN);
        } else {
            CommandFeedback.message("No saved session to return to", Formatting.RED);
        }
    }

    private static void list(String[] arguments) {
        List<BotSessionManager.BotConnection> connections = BotSessionManager.getConnections();
        if (connections.isEmpty()) {
            CommandFeedback.message("Bot list is empty", Formatting.YELLOW);
            return;
        }
        CommandFeedback.message("Connected bots:", Formatting.AQUA);
        for (BotSessionManager.BotConnection connection : connections) {
            CommandFeedback.message("- " + connection.name() + " @ " + connection.address(), Formatting.AQUA);
        }
    }

    private static boolean isBotCommand(String input) {
        String lowerInput = input.toLowerCase(Locale.ROOT);
        return lowerInput.equals(".bot") || lowerInput.startsWith(".bot ");
    }

    private static void showUsage() {
        CommandFeedback.message(".bot connect <name> <ip> - connect a bot", Formatting.GRAY);
        CommandFeedback.message(".bot control <name> - switch to bot", Formatting.GRAY);
        CommandFeedback.message(".bot say <name> <message> - send chat as bot", Formatting.GRAY);
        CommandFeedback.message(".bot sayall <message> - send chat as all bots", Formatting.GRAY);
        CommandFeedback.message(".bot remove <name> - disconnect bot", Formatting.GRAY);
        CommandFeedback.message(".bot return - return to previous session", Formatting.GRAY);
        CommandFeedback.message(".bot list - list connected bots", Formatting.GRAY);
    }
}
