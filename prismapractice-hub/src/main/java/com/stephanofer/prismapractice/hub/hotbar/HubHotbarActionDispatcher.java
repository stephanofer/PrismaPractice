package com.stephanofer.prismapractice.hub.hotbar;

import com.stephanofer.prismapractice.api.common.PlayerId;
import com.stephanofer.prismapractice.core.application.queue.QueueLeaveResult;
import com.stephanofer.prismapractice.core.application.queue.QueueService;
import com.stephanofer.prismapractice.hub.HubQueueExitCause;
import com.stephanofer.prismapractice.hub.HubQueueFeedbackCoordinator;
import com.stephanofer.prismapractice.hub.HubQueueExitService;
import com.stephanofer.prismapractice.hub.HubScoreboardModule;
import com.stephanofer.prismapractice.paper.feedback.PaperFeedbackService;
import com.stephanofer.prismapractice.paper.scoreboard.ScoreboardUiFocus;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

final class HubHotbarActionDispatcher {

    private final JavaPlugin plugin;
    private final QueueService queueService;
    private final HubHotbarMenuController menuController;
    private final HubHotbarService hotbarService;
    private final HubScoreboardModule scoreboardModule;
    private final PaperFeedbackService feedbackService;
    private final HubQueueFeedbackCoordinator queueFeedbackCoordinator;
    private final Supplier<HubQueueExitService> queueExitServiceSupplier;
    private final Map<String, HubHotbarCustomActionHandler> customHandlers;

    HubHotbarActionDispatcher(JavaPlugin plugin, QueueService queueService, HubHotbarMenuController menuController, HubHotbarService hotbarService, HubScoreboardModule scoreboardModule, PaperFeedbackService feedbackService, HubQueueFeedbackCoordinator queueFeedbackCoordinator, Supplier<HubQueueExitService> queueExitServiceSupplier) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.queueService = Objects.requireNonNull(queueService, "queueService");
        this.menuController = Objects.requireNonNull(menuController, "menuController");
        this.hotbarService = Objects.requireNonNull(hotbarService, "hotbarService");
        this.scoreboardModule = Objects.requireNonNull(scoreboardModule, "scoreboardModule");
        this.feedbackService = Objects.requireNonNull(feedbackService, "feedbackService");
        this.queueFeedbackCoordinator = Objects.requireNonNull(queueFeedbackCoordinator, "queueFeedbackCoordinator");
        this.queueExitServiceSupplier = Objects.requireNonNull(queueExitServiceSupplier, "queueExitServiceSupplier");
        this.customHandlers = new ConcurrentHashMap<>();
    }

    void registerCustomHandler(String key, HubHotbarCustomActionHandler handler) {
        customHandlers.put(key, handler);
    }

    boolean dispatch(Player player, HubHotbarActionConfig action) {
        return switch (action.type()) {
            case OPEN_MENU -> openMenu(player, action);
            case RUN_PLAYER_COMMAND -> runPlayerCommand(player, action.command());
            case RUN_CONSOLE_COMMAND -> runConsoleCommand(player, action.command());
            case LEAVE_QUEUE -> leaveQueue(player);
            case CUSTOM -> runCustom(player, action);
        };
    }

    private boolean openMenu(Player player, HubHotbarActionConfig action) {
        boolean opened = menuController.openMenu(
                player,
                replacePlaceholders(player, action.pluginName()),
                replacePlaceholders(player, action.target()),
                action.page(),
                action.arguments().stream().map(argument -> replacePlaceholders(player, argument)).toList()
        );
        if (opened) {
            scoreboardModule.setUiFocus(new PlayerId(player.getUniqueId()), mapFocus(action.target()));
            scoreboardModule.scoreboardService().refresh(player, true);
        }
        return opened;
    }

    private boolean runPlayerCommand(Player player, String command) {
        return player.performCommand(stripLeadingSlash(replacePlaceholders(player, command)));
    }

    private boolean runConsoleCommand(Player player, String command) {
        return Bukkit.dispatchCommand(Bukkit.getConsoleSender(), stripLeadingSlash(replacePlaceholders(player, command)));
    }

    private boolean leaveQueue(Player player) {
        HubQueueExitService queueExitService = queueExitServiceSupplier.get();
        if (queueExitService != null) {
            QueueLeaveResult result = queueExitService.leave(player, HubQueueExitCause.HOTBAR_ITEM);
            return result.success() || result.repairedState();
        }
        QueueLeaveResult result = queueService.leaveQueue(new PlayerId(player.getUniqueId()));
        queueFeedbackCoordinator.clear(player);
        scoreboardModule.clearUiFocus(new PlayerId(player.getUniqueId()));
        scoreboardModule.scoreboardService().refresh(player, true);
        hotbarService.refresh(player, true);
        if (result.success()) {
            feedbackService.send(player, "queue-leave-success", Map.of(
                    "player", player.getName(),
                    "queue_name", result.removedEntry().queueId().toString(),
                    "queue_id", result.removedEntry().queueId().toString(),
                    "queue_players", "0",
                    "active_queue_name", "ninguna",
                    "status", "Disponible"
            ));
            return true;
        }
        feedbackService.send(player, "queue-leave-not-active", Map.of("player", player.getName()));
        return result.repairedState();
    }

    private boolean runCustom(Player player, HubHotbarActionConfig action) {
        HubHotbarCustomActionHandler handler = customHandlers.get(action.customKey());
        if (handler == null) {
            plugin.getLogger().warning("No custom hub item action handler registered for key '" + action.customKey() + "'.");
            return false;
        }
        return handler.execute(player, action);
    }

    private String replacePlaceholders(Player player, String value) {
        return value
                .replace("%player_name%", player.getName())
                .replace("%player_uuid%", player.getUniqueId().toString());
    }

    private String stripLeadingSlash(String command) {
        return command.startsWith("/") ? command.substring(1) : command;
    }

    private ScoreboardUiFocus mapFocus(String target) {
        if (target == null) {
            return ScoreboardUiFocus.DEFAULT;
        }
        return switch (target.trim().toLowerCase(java.util.Locale.ROOT)) {
            case "unranked" -> ScoreboardUiFocus.UNRANKED_MENU;
            case "ranked" -> ScoreboardUiFocus.RANKED_MENU;
            case "party", "party-leader" -> ScoreboardUiFocus.PARTY_MENU;
            case "settings" -> ScoreboardUiFocus.SETTINGS_MENU;
            case "profile" -> ScoreboardUiFocus.PROFILE_MENU;
            default -> ScoreboardUiFocus.DEFAULT;
        };
    }
}
