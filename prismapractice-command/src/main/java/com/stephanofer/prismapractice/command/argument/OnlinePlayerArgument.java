package com.stephanofer.prismapractice.command.argument;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.stephanofer.prismapractice.command.CommandSuggestions;
import io.papermc.paper.command.brigadier.MessageComponentSerializer;
import io.papermc.paper.command.brigadier.argument.CustomArgumentType;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public final class OnlinePlayerArgument implements CustomArgumentType.Converted<Player, String> {

    private static final DynamicCommandExceptionType PLAYER_NOT_FOUND = new DynamicCommandExceptionType(name ->
        MessageComponentSerializer.message().serialize(Component.text("Player not online: " + name))
    );

    @Override
    public Player convert(final String nativeType) throws CommandSyntaxException {
        final Player player = Bukkit.getPlayerExact(nativeType);
        if (player == null) {
            throw PLAYER_NOT_FOUND.create(nativeType);
        }
        return player;
    }

    @Override
    public ArgumentType<String> getNativeType() {
        return StringArgumentType.word();
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(final CommandContext<S> context, final SuggestionsBuilder builder) {
        final Collection<String> playerNames = Bukkit.getOnlinePlayers().stream()
            .map(Player::getName)
            .toList();
        return CommandSuggestions.completeStrings(builder, playerNames);
    }
}
