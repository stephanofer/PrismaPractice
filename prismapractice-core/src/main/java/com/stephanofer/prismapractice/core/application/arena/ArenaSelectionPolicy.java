package com.stephanofer.prismapractice.core.application.arena;

import com.stephanofer.prismapractice.api.arena.ArenaDefinition;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public final class ArenaSelectionPolicy {

    public List<ArenaDefinition> prioritize(List<ArenaDefinition> candidates) {
        Objects.requireNonNull(candidates, "candidates");
        return candidates.stream()
                .sorted(Comparator.comparingInt((ArenaDefinition arena) -> arena.featured() ? 1 : 0)
                        .thenComparingInt(ArenaDefinition::selectionWeight)
                        .reversed()
                        .thenComparing(arena -> arena.arenaId().value()))
                .toList();
    }
}
