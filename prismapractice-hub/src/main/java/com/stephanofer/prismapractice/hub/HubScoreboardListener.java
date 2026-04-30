package com.stephanofer.prismapractice.hub;

import com.stephanofer.prismapractice.api.common.PlayerId;
import com.stephanofer.prismapractice.paper.scoreboard.PaperScoreboardService;
import com.stephanofer.prismapractice.paper.scoreboard.ScoreboardUiStateService;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;

import java.util.Objects;

final class HubScoreboardListener implements Listener {

    private final ScoreboardUiStateService uiStateService;
    private final PaperScoreboardService scoreboardService;

    HubScoreboardListener(ScoreboardUiStateService uiStateService, PaperScoreboardService scoreboardService) {
        this.uiStateService = Objects.requireNonNull(uiStateService, "uiStateService");
        this.scoreboardService = Objects.requireNonNull(scoreboardService, "scoreboardService");
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }
        uiStateService.clear(new PlayerId(player.getUniqueId()));
        scoreboardService.refresh(player, true);
    }
}
