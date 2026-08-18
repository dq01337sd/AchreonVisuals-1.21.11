package ez.minar.system.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import ez.minar.system.managers.ConfigManager;
import net.minecraft.command.CommandSource;

import java.util.concurrent.CompletableFuture;

import static com.mojang.brigadier.builder.LiteralArgumentBuilder.literal;
import static com.mojang.brigadier.builder.RequiredArgumentBuilder.argument;

public final class DotCommandSuggestions {
    private static final CommandDispatcher<Object> DISPATCHER = new CommandDispatcher<>();

    static {
        DISPATCHER.register(literal(".cfg")
                .then(literal("dir"))
                .then(literal("save")
                        .then(argument("name", StringArgumentType.word())))
                .then(literal("load")
                        .then(argument("name", StringArgumentType.word())
                                .suggests((context, builder) -> CommandSource.suggestMatching(ConfigManager.getConfigNames(), builder))))
                .then(literal("reset")));

        DISPATCHER.register(literal(".friend")
                .then(literal("add")
                        .then(argument("nick", StringArgumentType.word())))
                .then(literal("remove")
                        .then(argument("nick", StringArgumentType.word())))
                .then(literal("clear")));

        DISPATCHER.register(literal(".parse"));
    }

    private DotCommandSuggestions() {
    }

    public static CompletableFuture<Suggestions> getSuggestions(String input, int cursor) {
        return DISPATCHER.getCompletionSuggestions(DISPATCHER.parse(input, new Object()), cursor);
    }
}