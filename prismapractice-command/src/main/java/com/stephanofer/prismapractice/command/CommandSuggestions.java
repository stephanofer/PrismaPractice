package com.stephanofer.prismapractice.command;

import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.Collection;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

public final class CommandSuggestions {

    private static final int DEFAULT_LIMIT = 50;

    private CommandSuggestions() {
    }

    public static CompletableFuture<Suggestions> completeStrings(final SuggestionsBuilder builder, final Collection<String> values) {
        return completeStrings(builder, values, DEFAULT_LIMIT);
    }

    public static CompletableFuture<Suggestions> completeStrings(
        final SuggestionsBuilder builder,
        final Collection<String> values,
        final int limit
    ) {
        final String remaining = builder.getRemainingLowerCase();
        int matches = 0;

        for (final String value : values) {
            if (value == null) {
                continue;
            }

            if (!value.toLowerCase(Locale.ROOT).startsWith(remaining)) {
                continue;
            }

            builder.suggest(value);
            matches++;
            if (matches >= limit) {
                break;
            }
        }

        return builder.buildFuture();
    }

    public static <E extends Enum<E>> CompletableFuture<Suggestions> completeEnumNames(
        final SuggestionsBuilder builder,
        final Class<E> enumType
    ) {
        final E[] constants = enumType.getEnumConstants();
        for (final E constant : constants) {
            if (constant == null) {
                continue;
            }
            final String lowered = constant.name().toLowerCase(Locale.ROOT);
            if (lowered.startsWith(builder.getRemainingLowerCase())) {
                builder.suggest(lowered);
            }
        }
        return builder.buildFuture();
    }
}
