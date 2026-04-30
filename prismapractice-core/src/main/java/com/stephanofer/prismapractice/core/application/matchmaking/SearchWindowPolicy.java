package com.stephanofer.prismapractice.core.application.matchmaking;

import com.stephanofer.prismapractice.api.matchmaking.MatchmakingSearchWindow;
import com.stephanofer.prismapractice.api.queue.SearchExpansionStrategy;

import java.time.Duration;
import java.util.List;
import java.util.Objects;

public final class SearchWindowPolicy {

    public MatchmakingSearchWindow resolve(SearchExpansionStrategy strategy, Duration waited) {
        Objects.requireNonNull(strategy, "strategy");
        Objects.requireNonNull(waited, "waited");
        List<MatchmakingSearchWindow> windows = switch (strategy) {
            case RANKED_STANDARD -> rankedWindows();
            case UNRANKED_STANDARD -> unrankedWindows();
        };
        MatchmakingSearchWindow current = windows.getFirst();
        for (MatchmakingSearchWindow window : windows) {
            if (!waited.minus(window.minimumQueueTime()).isNegative()) {
                current = window;
            }
        }
        return current;
    }

    private List<MatchmakingSearchWindow> rankedWindows() {
        return List.of(
                new MatchmakingSearchWindow("ranked-0-15", Duration.ZERO, 75, 72),
                new MatchmakingSearchWindow("ranked-15-30", Duration.ofSeconds(15), 125, 66),
                new MatchmakingSearchWindow("ranked-30-45", Duration.ofSeconds(30), 200, 58),
                new MatchmakingSearchWindow("ranked-45-60", Duration.ofSeconds(45), 300, 48),
                new MatchmakingSearchWindow("ranked-60-plus", Duration.ofSeconds(60), 450, 40)
        );
    }

    private List<MatchmakingSearchWindow> unrankedWindows() {
        return List.of(
                new MatchmakingSearchWindow("unranked-0-10", Duration.ZERO, 150, 52),
                new MatchmakingSearchWindow("unranked-10-25", Duration.ofSeconds(10), 250, 44),
                new MatchmakingSearchWindow("unranked-25-45", Duration.ofSeconds(25), 400, 36),
                new MatchmakingSearchWindow("unranked-45-plus", Duration.ofSeconds(45), 600, 28)
        );
    }
}
