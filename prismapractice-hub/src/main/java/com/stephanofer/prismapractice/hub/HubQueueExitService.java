package com.stephanofer.prismapractice.hub;

import com.stephanofer.prismapractice.api.common.PlayerId;
import com.stephanofer.prismapractice.core.application.queue.QueueLeaveResult;
import com.stephanofer.prismapractice.core.application.queue.QueueService;
import com.stephanofer.prismapractice.hub.hotbar.HubHotbarService;
import com.stephanofer.prismapractice.paper.feedback.PaperFeedbackService;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.Objects;

public final class HubQueueExitService {

    private final QueueService queueService;
    private final HubHotbarService hotbarService;
    private final HubScoreboardModule scoreboardModule;
    private final PaperFeedbackService feedbackService;
    private final HubQueueFeedbackCoordinator queueFeedbackCoordinator;

    public HubQueueExitService(
            QueueService queueService,
            HubHotbarService hotbarService,
            HubScoreboardModule scoreboardModule,
            PaperFeedbackService feedbackService,
            HubQueueFeedbackCoordinator queueFeedbackCoordinator
    ) {
        this.queueService = Objects.requireNonNull(queueService, "queueService");
        this.hotbarService = Objects.requireNonNull(hotbarService, "hotbarService");
        this.scoreboardModule = Objects.requireNonNull(scoreboardModule, "scoreboardModule");
        this.feedbackService = Objects.requireNonNull(feedbackService, "feedbackService");
        this.queueFeedbackCoordinator = Objects.requireNonNull(queueFeedbackCoordinator, "queueFeedbackCoordinator");
    }

    public QueueLeaveResult leave(Player player, HubQueueExitCause cause) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(cause, "cause");
        QueueLeaveResult result = queueService.leaveQueue(new PlayerId(player.getUniqueId()));
        queueFeedbackCoordinator.clear(player);
        scoreboardModule.clearUiFocus(new PlayerId(player.getUniqueId()));
        if (cause != HubQueueExitCause.DISCONNECT && cause != HubQueueExitCause.RESPAWN_SAFETY) {
            scoreboardModule.scoreboardService().refresh(player, true);
            hotbarService.refresh(player, true);
        }
        if (shouldNotify(cause)) {
            if (result.success()) {
                feedbackService.send(player, "queue-leave-success", Map.of("player", player.getName()));
            } else {
                feedbackService.send(player, "queue-leave-not-active", Map.of("player", player.getName()));
            }
        }
        return result;
    }

    private boolean shouldNotify(HubQueueExitCause cause) {
        return switch (cause) {
            case MENU_TOGGLE, HOTBAR_ITEM, COMMAND -> true;
            case DISCONNECT, RESPAWN_SAFETY -> false;
        };
    }
}
