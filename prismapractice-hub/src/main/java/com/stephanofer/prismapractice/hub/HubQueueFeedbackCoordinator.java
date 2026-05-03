package com.stephanofer.prismapractice.hub;

import com.stephanofer.prismapractice.api.common.PlayerId;
import com.stephanofer.prismapractice.api.queue.QueueDefinition;
import com.stephanofer.prismapractice.api.queue.QueueEntry;
import com.stephanofer.prismapractice.api.queue.QueueEntryRepository;
import com.stephanofer.prismapractice.api.queue.QueueRepository;
import com.stephanofer.prismapractice.paper.feedback.PaperFeedbackService;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class HubQueueFeedbackCoordinator {

    private static final String QUEUE_STATUS_TEMPLATE = "queue-status-persistent";
    private final QueueRepository queueRepository;
    private final QueueEntryRepository queueEntryRepository;
    private final PaperFeedbackService feedbackService;
    private final Set<UUID> trackedPlayers = ConcurrentHashMap.newKeySet();
    private final BukkitTask refreshTask;

    public HubQueueFeedbackCoordinator(Plugin plugin, QueueRepository queueRepository, QueueEntryRepository queueEntryRepository, PaperFeedbackService feedbackService) {
        Objects.requireNonNull(plugin, "plugin");
        this.queueRepository = Objects.requireNonNull(queueRepository, "queueRepository");
        this.queueEntryRepository = Objects.requireNonNull(queueEntryRepository, "queueEntryRepository");
        this.feedbackService = Objects.requireNonNull(feedbackService, "feedbackService");
        this.refreshTask = plugin.getServer().getScheduler().runTaskTimer(plugin, this::refreshTrackedPlayers, 20L, 20L);
    }

    public void track(Player player) {
        Objects.requireNonNull(player, "player");
        trackedPlayers.add(player.getUniqueId());
        refresh(player);
    }

    public void clear(Player player) {
        Objects.requireNonNull(player, "player");
        trackedPlayers.remove(player.getUniqueId());
        feedbackService.clearPersistentSlots(player, QUEUE_STATUS_TEMPLATE);
    }

    public void close() {
        refreshTask.cancel();
        for (UUID trackedPlayer : trackedPlayers) {
            Player player = Bukkit.getPlayer(trackedPlayer);
            if (player != null && player.isOnline()) {
                feedbackService.clearPersistentSlots(player, QUEUE_STATUS_TEMPLATE);
            }
        }
        trackedPlayers.clear();
    }

    private void refreshTrackedPlayers() {
        for (UUID trackedPlayer : Set.copyOf(trackedPlayers)) {
            Player player = Bukkit.getPlayer(trackedPlayer);
            if (player == null || !player.isOnline()) {
                trackedPlayers.remove(trackedPlayer);
                continue;
            }
            refresh(player);
        }
    }

    private void refresh(Player player) {
        Optional<QueueEntry> activeEntry = queueEntryRepository.findByPlayerId(new PlayerId(player.getUniqueId()));
        if (activeEntry.isEmpty()) {
            clear(player);
            return;
        }
        feedbackService.send(player, QUEUE_STATUS_TEMPLATE, placeholders(player, activeEntry.get()));
    }

    private Map<String, String> placeholders(Player player, QueueEntry entry) {
        Map<String, String> placeholders = new LinkedHashMap<>();
        QueueDefinition definition = queueRepository.findById(entry.queueId()).orElse(null);
        Duration wait = Duration.between(entry.joinedAt(), Instant.now());
        long seconds = Math.max(wait.getSeconds(), 0L);
        placeholders.put("player", player.getName());
        placeholders.put("queue_name", definition == null ? entry.queueId().toString() : definition.displayName());
        placeholders.put("queue_id", entry.queueId().toString());
        placeholders.put("queue_players", Integer.toString(queueEntryRepository.findByQueueId(entry.queueId()).size()));
        placeholders.put("queue_wait_seconds", Long.toString(seconds));
        placeholders.put("queue_wait", seconds + "s");
        placeholders.put("queue_wait_clock", formatClock(seconds));
        return Map.copyOf(placeholders);
    }

    private String formatClock(long totalSeconds) {
        long hours = totalSeconds / 3600L;
        long minutes = (totalSeconds % 3600L) / 60L;
        long seconds = totalSeconds % 60L;
        if (hours > 0L) {
            return "%02d:%02d:%02d".formatted(hours, minutes, seconds);
        }
        return "%02d:%02d".formatted(minutes, seconds);
    }
}
