package com.stephanofer.prismapractice.hub.ui;

import com.stephanofer.prismapractice.api.common.PlayerId;
import com.stephanofer.prismapractice.api.common.QueueId;
import com.stephanofer.prismapractice.hub.HubQueueFeedbackCoordinator;
import com.stephanofer.prismapractice.hub.HubScoreboardModule;
import com.stephanofer.prismapractice.hub.hotbar.HubHotbarService;
import com.stephanofer.prismapractice.paper.feedback.PaperFeedbackService;
import com.stephanofer.prismapractice.paper.ui.menu.ZMenuUiService;
import fr.maxlego08.menu.api.MenuItemStack;
import fr.maxlego08.menu.api.button.Button;
import fr.maxlego08.menu.api.engine.InventoryEngine;
import fr.maxlego08.menu.api.utils.Placeholders;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

final class QueueEntryButton extends Button {

    private final HubQueueMenuService queueMenuService;
    private final HubScoreboardModule scoreboardModule;
    private final PaperFeedbackService feedbackService;
    private final HubQueueFeedbackCoordinator queueFeedbackCoordinator;
    private final Supplier<HubHotbarService> hotbarServiceSupplier;
    private final Supplier<ZMenuUiService> menuUiServiceSupplier;
    private final QueueId queueId;
    private final boolean closeOnSuccess;
    private final boolean refreshMenuOnClick;
    private final MenuItemStack currentQueueItem;
    private final MenuItemStack otherQueueItem;
    private final MenuItemStack disabledItem;
    private final MenuItemStack missingItem;

    QueueEntryButton(
            HubQueueMenuService queueMenuService,
            HubScoreboardModule scoreboardModule,
            PaperFeedbackService feedbackService,
            HubQueueFeedbackCoordinator queueFeedbackCoordinator,
            Supplier<HubHotbarService> hotbarServiceSupplier,
            Supplier<ZMenuUiService> menuUiServiceSupplier,
            QueueId queueId,
            boolean closeOnSuccess,
            boolean refreshMenuOnClick,
            MenuItemStack currentQueueItem,
            MenuItemStack otherQueueItem,
            MenuItemStack disabledItem,
            MenuItemStack missingItem
    ) {
        this.queueMenuService = Objects.requireNonNull(queueMenuService, "queueMenuService");
        this.scoreboardModule = Objects.requireNonNull(scoreboardModule, "scoreboardModule");
        this.feedbackService = Objects.requireNonNull(feedbackService, "feedbackService");
        this.queueFeedbackCoordinator = Objects.requireNonNull(queueFeedbackCoordinator, "queueFeedbackCoordinator");
        this.hotbarServiceSupplier = Objects.requireNonNull(hotbarServiceSupplier, "hotbarServiceSupplier");
        this.menuUiServiceSupplier = Objects.requireNonNull(menuUiServiceSupplier, "menuUiServiceSupplier");
        this.queueId = Objects.requireNonNull(queueId, "queueId");
        this.closeOnSuccess = closeOnSuccess;
        this.refreshMenuOnClick = refreshMenuOnClick;
        this.currentQueueItem = currentQueueItem;
        this.otherQueueItem = otherQueueItem;
        this.disabledItem = disabledItem;
        this.missingItem = missingItem;
    }

    @Override
    public ItemStack getCustomItemStack(Player player, boolean useCache, Placeholders placeholders) {
        QueueMenuView view = queueMenuService.view(new PlayerId(player.getUniqueId()), queueId);
        MenuItemStack resolvedItem = resolveItem(view.state());
        return resolvedItem.build(player, false, QueueMenuButtonSupport.placeholders(player, view));
    }

    @Override
    public void onClick(Player player, InventoryClickEvent event, InventoryEngine inventory, int slot, Placeholders placeholders) {
        playConfiguredSound(player);
        PlayerId playerId = new PlayerId(player.getUniqueId());
        QueueMenuActionResult result = queueMenuService.click(playerId, queueId);

        Map<String, String> feedbackPlaceholders = feedbackPlaceholders(player, result.view());
        if (result.success()) {
            switch (result.action()) {
                case JOINED, SWITCHED -> {
                    feedbackService.send(player, "queue-join-success", feedbackPlaceholders);
                    queueFeedbackCoordinator.track(player);
                    scoreboardModule.clearUiFocus(playerId);
                }
                case LEFT_CURRENT -> {
                    feedbackService.send(player, "queue-leave-success", feedbackPlaceholders);
                    queueFeedbackCoordinator.clear(player);
                    scoreboardModule.clearUiFocus(playerId);
                }
                case FAILED -> {
                }
            }
        } else {
            feedbackService.send(player, templateFor(result.view().state()), feedbackPlaceholders);
        }

        refreshRuntimeState(player);
        if (result.success() && (result.action() == QueueMenuAction.LEFT_CURRENT || closeOnSuccess)) {
            player.closeInventory();
        } else if (refreshMenuOnClick) {
            menuUiServiceSupplier.get().updateInventory(player);
        }
    }

    private void playConfiguredSound(Player player) {
        if (getSound() != null) {
            getSound().play(player);
        }
    }

    private MenuItemStack resolveItem(QueueMenuState state) {
        return switch (state) {
            case CURRENT_QUEUE -> currentQueueItem != null ? currentQueueItem : getItemStack();
            case OTHER_QUEUE -> otherQueueItem != null ? otherQueueItem : getItemStack();
            case DISABLED -> disabledItem != null ? disabledItem : getItemStack();
            case MISSING -> missingItem != null ? missingItem : getItemStack();
            case AVAILABLE -> getItemStack();
        };
    }

    private void refreshRuntimeState(Player player) {
        HubHotbarService hotbarService = hotbarServiceSupplier.get();
        if (hotbarService != null) {
            hotbarService.refresh(player, true);
        }
        scoreboardModule.scoreboardService().refresh(player, true);
    }

    private Map<String, String> feedbackPlaceholders(Player player, QueueMenuView view) {
        Map<String, String> placeholders = new LinkedHashMap<>();
        placeholders.put("player", player.getName());
        placeholders.put("queue_name", view.definition() == null ? view.targetQueueId().toString() : view.definition().displayName());
        placeholders.put("queue_id", view.targetQueueId().toString());
        placeholders.put("queue_players", Integer.toString(view.playerCount()));
        placeholders.put("active_queue_name", view.activeQueueName().isBlank() ? "ninguna" : view.activeQueueName());
        placeholders.put("status", QueueMenuButtonSupport.statusText(view.state()));
        return Map.copyOf(placeholders);
    }

    private String templateFor(QueueMenuState state) {
        return switch (state) {
            case CURRENT_QUEUE -> "queue-join-already-current";
            case OTHER_QUEUE -> "queue-join-already-other";
            case DISABLED -> "queue-join-disabled";
            case MISSING -> "queue-join-missing";
            case AVAILABLE -> "queue-join-failed";
        };
    }
}
