package com.stephanofer.prismapractice.hub.hotbar;

import org.bukkit.entity.Player;

import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class HubStaffModeService {

    private final Set<UUID> enabledPlayers = ConcurrentHashMap.newKeySet();

    public boolean isEnabled(Player player) {
        Objects.requireNonNull(player, "player");
        return enabledPlayers.contains(player.getUniqueId());
    }

    public boolean toggle(Player player) {
        Objects.requireNonNull(player, "player");
        UUID uniqueId = player.getUniqueId();
        if (enabledPlayers.remove(uniqueId)) {
            return false;
        }
        enabledPlayers.add(uniqueId);
        return true;
    }

    public void disable(Player player) {
        Objects.requireNonNull(player, "player");
        enabledPlayers.remove(player.getUniqueId());
    }
}
