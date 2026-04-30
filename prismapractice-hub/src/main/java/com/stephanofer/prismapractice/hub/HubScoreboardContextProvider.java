package com.stephanofer.prismapractice.hub;

import com.stephanofer.prismapractice.api.common.PlayerId;
import com.stephanofer.prismapractice.api.common.RuntimeType;
import com.stephanofer.prismapractice.api.profile.PracticeProfile;
import com.stephanofer.prismapractice.api.profile.PracticeSettings;
import com.stephanofer.prismapractice.api.queue.QueueDefinition;
import com.stephanofer.prismapractice.api.queue.QueueEntry;
import com.stephanofer.prismapractice.paper.scoreboard.PlayerScoreboardDataCache;
import com.stephanofer.prismapractice.paper.scoreboard.ScoreboardContextProvider;
import com.stephanofer.prismapractice.paper.scoreboard.ScoreboardContextSnapshot;
import com.stephanofer.prismapractice.paper.scoreboard.ScoreboardPartyRole;
import com.stephanofer.prismapractice.paper.scoreboard.ScoreboardQueueView;
import com.stephanofer.prismapractice.paper.scoreboard.ScoreboardUiStateService;
import org.bukkit.entity.Player;

import java.util.Objects;
import java.util.Optional;

final class HubScoreboardContextProvider implements ScoreboardContextProvider {

    private final HubPracticeServices practiceServices;
    private final PlayerScoreboardDataCache dataCache;
    private final ScoreboardUiStateService uiStateService;

    HubScoreboardContextProvider(HubPracticeServices practiceServices, PlayerScoreboardDataCache dataCache, ScoreboardUiStateService uiStateService) {
        this.practiceServices = Objects.requireNonNull(practiceServices, "practiceServices");
        this.dataCache = Objects.requireNonNull(dataCache, "dataCache");
        this.uiStateService = Objects.requireNonNull(uiStateService, "uiStateService");
    }

    @Override
    public ScoreboardContextSnapshot snapshot(Player player) {
        PlayerId playerId = new PlayerId(player.getUniqueId());
        PracticeProfile profile = dataCache.profile(playerId).orElse(null);
        PracticeSettings settings = dataCache.settings(playerId);
        boolean inParty = practiceServices.playerPartyIndexRepository().isInParty(playerId);
        return new ScoreboardContextSnapshot(
                RuntimeType.HUB,
                practiceServices.playerStateService().findCurrentState(playerId).orElse(null),
                practiceServices.playerStateService().findPresence(playerId).orElse(null),
                profile,
                settings,
                inParty,
                inParty ? ScoreboardPartyRole.MEMBER : ScoreboardPartyRole.NONE,
                uiStateService.focus(playerId),
                queueView(playerId).orElse(null)
        );
    }

    private Optional<ScoreboardQueueView> queueView(PlayerId playerId) {
        Optional<QueueEntry> entry = practiceServices.queueEntryRepository().findByPlayerId(playerId);
        if (entry.isEmpty()) {
            return Optional.empty();
        }
        Optional<QueueDefinition> definition = practiceServices.queueRepository().findById(entry.get().queueId());
        if (definition.isEmpty()) {
            return Optional.of(new ScoreboardQueueView(
                    entry.get().queueId().toString(),
                    entry.get().queueType().name(),
                    entry.get().queueType(),
                    entry.get().playerType(),
                    entry.get().joinedAt(),
                    0
            ));
        }
        return Optional.of(new ScoreboardQueueView(
                definition.get().queueId().toString(),
                definition.get().displayName(),
                definition.get().queueType(),
                definition.get().playerType(),
                entry.get().joinedAt(),
                practiceServices.queueEntryRepository().findByQueueId(definition.get().queueId()).size()
        ));
    }
}
