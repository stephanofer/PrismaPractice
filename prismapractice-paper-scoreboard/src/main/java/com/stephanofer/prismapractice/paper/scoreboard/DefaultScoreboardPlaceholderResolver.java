package com.stephanofer.prismapractice.paper.scoreboard;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

public final class DefaultScoreboardPlaceholderResolver implements ScoreboardPlaceholderResolver {

    @Override
    public Map<String, String> resolve(Player player, ScoreboardContextSnapshot snapshot) {
        Map<String, String> values = new LinkedHashMap<>();
        values.put("player_name", player.getName());
        values.put("player_uuid", player.getUniqueId().toString());
        values.put("ping", Integer.toString(Math.max(player.getPing(), 0)));
        values.put("online_players", Integer.toString(Bukkit.getOnlinePlayers().size()));
        values.put("runtime", snapshot.runtimeType().name());
        values.put("state", snapshot.status() == null ? "UNKNOWN" : snapshot.status().name());
        values.put("sub_state", snapshot.subStatus() == null ? "NONE" : snapshot.subStatus().name());
        values.put("ui_focus", snapshot.uiFocus().name());
        values.put("party_status", snapshot.inParty() ? "In Party" : "Solo");
        values.put("party_role", snapshot.partyRole().name());
        values.put("rating", snapshot.profile() == null ? "0" : Integer.toString(snapshot.profile().currentGlobalRating()));
        values.put("global_rank", snapshot.profile() == null ? "Unranked" : snapshot.profile().currentGlobalRankKey());
        values.put("server_id", snapshot.presence() == null ? "unknown" : snapshot.presence().serverId());

        if (snapshot.queueView() != null) {
            values.put("queue_id", snapshot.queueView().queueId());
            values.put("queue_name", snapshot.queueView().displayName());
            values.put("queue_type", snapshot.queueView().queueType().name());
            values.put("queue_player_type", snapshot.queueView().playerType().name());
            values.put("queue_players", Integer.toString(snapshot.queueView().playerCount()));
            values.put("queue_wait", formatDuration(Duration.between(snapshot.queueView().joinedAt(), Instant.now())));
        } else {
            values.put("queue_id", "none");
            values.put("queue_name", "None");
            values.put("queue_type", "NONE");
            values.put("queue_player_type", "NONE");
            values.put("queue_players", "0");
            values.put("queue_wait", "00:00");
        }

        return values;
    }

    private String formatDuration(Duration duration) {
        long seconds = Math.max(duration.getSeconds(), 0L);
        long hours = seconds / 3600L;
        long minutes = (seconds % 3600L) / 60L;
        long remainingSeconds = seconds % 60L;
        if (hours > 0L) {
            return "%02d:%02d:%02d".formatted(hours, minutes, remainingSeconds);
        }
        return "%02d:%02d".formatted(minutes, remainingSeconds);
    }
}
