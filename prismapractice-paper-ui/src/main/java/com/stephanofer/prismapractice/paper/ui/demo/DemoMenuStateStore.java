package com.stephanofer.prismapractice.paper.ui.demo;

import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class DemoMenuStateStore {

    private final Map<UUID, Map<String, String>> states = new ConcurrentHashMap<>();

    public int increment(Player player, String key) {
        Objects.requireNonNull(player, "player");
        Map<String, String> state = state(player.getUniqueId());
        int current = Integer.parseInt(state.getOrDefault(key, "0"));
        int next = current + 1;
        state.put(key, String.valueOf(next));
        return next;
    }

    public String cycle(Player player, String key, List<String> values, String defaultValue) {
        Objects.requireNonNull(player, "player");
        List<String> options = values == null || values.isEmpty() ? List.of(defaultValue) : List.copyOf(values);
        String current = get(player, key).orElse(defaultValue);
        int currentIndex = options.indexOf(current);
        int nextIndex = currentIndex < 0 ? 0 : (currentIndex + 1) % options.size();
        String next = options.get(nextIndex);
        put(player, key, next);
        return next;
    }

    public void put(Player player, String key, String value) {
        if (key == null || key.isBlank() || value == null) {
            return;
        }
        state(player.getUniqueId()).put(key, value);
    }

    public java.util.Optional<String> get(Player player, String key) {
        Map<String, String> state = states.get(player.getUniqueId());
        if (state == null) {
            return java.util.Optional.empty();
        }
        return java.util.Optional.ofNullable(state.get(key));
    }

    public Map<String, String> snapshot(Player player) {
        return Map.copyOf(state(player.getUniqueId()));
    }

    public void reset(Player player) {
        states.remove(player.getUniqueId());
    }

    public List<Integer> integerRange(int size) {
        List<Integer> values = new ArrayList<>(size);
        for (int index = 1; index <= size; index++) {
            values.add(index);
        }
        return values;
    }

    private Map<String, String> state(UUID playerId) {
        return states.computeIfAbsent(playerId, ignored -> new LinkedHashMap<>());
    }
}
