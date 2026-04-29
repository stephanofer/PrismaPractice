package com.stephanofer.prismapractice.command;

import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CommandSuggestionsTest {

    @Test
    void shouldFilterSuggestionsByPrefix() {
        final SuggestionsBuilder builder = new SuggestionsBuilder("/party in", 7);
        final var suggestions = CommandSuggestions.completeStrings(builder, List.of("invite", "kick", "info")).join();

        assertEquals(List.of("info", "invite"), suggestions.getList().stream().map(s -> s.getText()).sorted().toList());
    }

    @Test
    void shouldRespectSuggestionLimit() {
        final SuggestionsBuilder builder = new SuggestionsBuilder("/mode ", 6);
        final var suggestions = CommandSuggestions.completeStrings(builder, List.of("a", "b", "c"), 2).join();

        assertEquals(2, suggestions.getList().size());
    }
}
