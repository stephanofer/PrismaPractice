package com.stephanofer.prismapractice.command;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.concurrent.CompletableFuture;

@FunctionalInterface
public interface CommandSuggestionProvider {

    CompletableFuture<Suggestions> suggest(PaperCommandContext context, SuggestionsBuilder builder) throws CommandSyntaxException;
}
