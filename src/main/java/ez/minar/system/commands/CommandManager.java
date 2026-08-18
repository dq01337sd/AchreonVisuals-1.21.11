package ez.minar.system.commands;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

public final class CommandManager {
    private CommandManager() {
    }

    public static boolean executeIfCommand(String input) {
        return FriendCommand.executeIfCommand(input) || ConfigCommand.executeIfCommand(input) || BotCommand.executeIfCommand(input) || executeParse(input);
    }

    public static List<String> suggestionsFor(String input) {
        String commandLine = input.stripLeading().toLowerCase(Locale.ROOT);
        if (".".equals(commandLine)) {
            return Stream.of(".friend", ".cfg", ".bot").sorted().toList();
        }

        if (commandLine.startsWith(".p") && ".parse".startsWith(commandLine)) {
            return List.of(".parse");
        }

        return Stream.of(
                        FriendCommand.suggestionsFor(input).stream(),
                        ConfigCommand.suggestionsFor(input).stream(),
                        BotCommand.suggestionsFor(input).stream()
                )
                .flatMap(stream -> stream)
                .sorted(Comparator.naturalOrder())
                .toList();
    }

    private static boolean executeParse(String input) {
        if (!".parse".equalsIgnoreCase(input.strip())) {
            return false;
        }

        ParseCommand.parsePlayers();
        return true;
    }

    public static String tabCompletionFor(String input) {
        List<String> suggestions = suggestionsFor(input);
        if (suggestions.isEmpty()) {
            return null;
        }

        String suggestion = suggestions.getFirst();
        if (".friend".equals(suggestion) || ".cfg".equals(suggestion) || ".bot".equals(suggestion)) {
            return suggestion + " ";
        }
        if (suggestion.endsWith("<nick>")) {
            return suggestion.substring(0, suggestion.length() - "<nick>".length());
        }
        return suggestion;
    }
}