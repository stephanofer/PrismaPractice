package com.stephanofer.prismapractice.core.application.leaderboard;

import com.stephanofer.prismapractice.api.common.ModeId;
import com.stephanofer.prismapractice.api.common.PlayerId;
import com.stephanofer.prismapractice.api.leaderboard.*;
import com.stephanofer.prismapractice.api.profile.PracticeProfile;
import com.stephanofer.prismapractice.api.profile.ProfileRepository;
import com.stephanofer.prismapractice.api.profile.ProfileVisibility;
import com.stephanofer.prismapractice.api.rating.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;

class LeaderboardProjectionServiceTest {

    private final PlayerId alpha = new PlayerId(UUID.fromString("11111111-1111-1111-1111-111111111111"));
    private final PlayerId beta = new PlayerId(UUID.fromString("22222222-2222-2222-2222-222222222222"));
    private final ModeId sword = new ModeId("sword");
    private InMemoryLeaderboardProjectionRepository projectionRepository;
    private InMemoryProfileRepository profileRepository;
    private InMemoryModeRatingRepository modeRatingRepository;
    private InMemoryGlobalRatingRepository globalRatingRepository;
    private LeaderboardProjectionService service;

    @BeforeEach
    void setUp() {
        projectionRepository = new InMemoryLeaderboardProjectionRepository();
        profileRepository = new InMemoryProfileRepository();
        modeRatingRepository = new InMemoryModeRatingRepository();
        globalRatingRepository = new InMemoryGlobalRatingRepository();
        service = new LeaderboardProjectionService(projectionRepository, profileRepository, modeRatingRepository, globalRatingRepository, () -> Optional.of(new SeasonContext("season-1", "Season 1", "ACTIVE", Instant.parse("2026-04-01T00:00:00Z"), null)));

        profileRepository.saveProfile(new PracticeProfile(alpha, "Alpha", "alpha", Instant.parse("2026-04-01T00:00:00Z"), Instant.parse("2026-04-30T00:00:00Z"), ProfileVisibility.PUBLIC, 1500, "emerald"));
        profileRepository.saveProfile(new PracticeProfile(beta, "Beta", "beta", Instant.parse("2026-04-01T00:00:00Z"), Instant.parse("2026-04-30T00:00:00Z"), ProfileVisibility.PUBLIC, 1700, "diamond"));
        globalRatingRepository.save(new GlobalRatingSnapshot(alpha, 1500, "emerald", 1500, "emerald", "season-1", Instant.now()));
        globalRatingRepository.save(new GlobalRatingSnapshot(beta, 1700, "diamond", 1700, "diamond", "season-1", Instant.now()));
        modeRatingRepository.save(new ModeRating(alpha, sword, 1550, "emerald", 1550, "emerald", true, 10, "season-1", Instant.now(), 10, 5));
        modeRatingRepository.save(new ModeRating(beta, sword, 1650, "diamond", 1650, "diamond", true, 10, "season-1", Instant.now(), 12, 4));
    }

    @Test
    void shouldRefreshAndQueryGlobalLeaderboard() {
        service.refreshPlayerGlobalProjection(alpha, null);
        service.refreshPlayerGlobalProjection(beta, null);

        List<LeaderboardEntry> top = service.getTop(new LeaderboardQuery(new LeaderboardScope(LeaderboardType.GLOBAL, null, null), 10, 0));

        assertEquals(2, top.size());
        assertEquals(beta, top.get(0).playerId());
        assertEquals(1, top.get(0).position());
        assertEquals(alpha, top.get(1).playerId());
    }

    @Test
    void shouldRebuildModeLeaderboard() {
        service.rebuildModeLeaderboard(sword, null);

        List<LeaderboardEntry> top = service.getTop(new LeaderboardQuery(new LeaderboardScope(LeaderboardType.MODE, sword, null), 10, 0));

        assertEquals(2, top.size());
        assertEquals(beta, top.get(0).playerId());
        assertEquals("diamond", top.get(0).rankKey());
    }

    @Test
    void shouldSkipMissingProfileOnRefresh() {
        PlayerId ghost = new PlayerId(UUID.fromString("33333333-3333-3333-3333-333333333333"));
        globalRatingRepository.save(new GlobalRatingSnapshot(ghost, 1800, "diamond", 1800, "diamond", "season-1", Instant.now()));

        service.refreshPlayerGlobalProjection(ghost, null);

        List<LeaderboardEntry> top = service.getTop(new LeaderboardQuery(new LeaderboardScope(LeaderboardType.GLOBAL, null, null), 10, 0));
        assertTrue(top.isEmpty());
    }

    private static final class InMemoryLeaderboardProjectionRepository implements LeaderboardProjectionRepository {
        private final Map<String, Map<String, LeaderboardEntry>> values = new ConcurrentHashMap<>();
        @Override public void upsert(LeaderboardScope scope, LeaderboardEntry entry) { values.computeIfAbsent(key(scope), unused -> new ConcurrentHashMap<>()).put(entry.playerId().toString(), entry); }
        @Override public void remove(LeaderboardScope scope, PlayerId playerId) { values.computeIfAbsent(key(scope), unused -> new ConcurrentHashMap<>()).remove(playerId.toString()); }
        @Override public List<LeaderboardEntry> top(LeaderboardQuery query) {
            return values.getOrDefault(key(query.scope()), Map.of()).values().stream()
                    .sorted((a, b) -> Integer.compare(b.score(), a.score()))
                    .skip(query.offset())
                    .limit(query.limit())
                    .map(entry -> new LeaderboardEntry(entry.playerId(), entry.playerName(), entry.score(), entry.rankKey(), 1))
                    .toList();
        }
        @Override public void clear(LeaderboardScope scope) { values.remove(key(scope)); }
        private String key(LeaderboardScope scope) { return scope.type() + ":" + (scope.modeId() == null ? "-" : scope.modeId()) + ":" + (scope.seasonId() == null ? "-" : scope.seasonId()); }
    }

    private static final class InMemoryProfileRepository implements ProfileRepository {
        private final Map<String, PracticeProfile> profiles = new ConcurrentHashMap<>();
        @Override public Optional<PracticeProfile> findProfile(PlayerId playerId) { return Optional.ofNullable(profiles.get(playerId.toString())); }
        @Override public PracticeProfile saveProfile(PracticeProfile profile) { profiles.put(profile.playerId().toString(), profile); return profile; }
        @Override public Optional<com.stephanofer.prismapractice.api.profile.PracticeSettings> findSettings(PlayerId playerId) { return Optional.empty(); }
        @Override public com.stephanofer.prismapractice.api.profile.PracticeSettings saveSettings(com.stephanofer.prismapractice.api.profile.PracticeSettings settings) { return settings; }
    }

    private static final class InMemoryModeRatingRepository implements ModeRatingRepository {
        private final Map<String, ModeRating> values = new ConcurrentHashMap<>();
        @Override public Optional<ModeRating> find(PlayerId playerId, ModeId modeId, String seasonId) { return Optional.ofNullable(values.get(playerId + ":" + modeId + ":" + seasonId)); }
        @Override public List<ModeRating> findByPlayerId(PlayerId playerId, String seasonId) { return values.values().stream().filter(v -> v.playerId().equals(playerId) && v.seasonId().equals(seasonId)).toList(); }
        @Override public List<ModeRating> findByModeId(ModeId modeId, String seasonId) { return values.values().stream().filter(v -> v.modeId().equals(modeId) && v.seasonId().equals(seasonId)).toList(); }
        @Override public ModeRating save(ModeRating modeRating) { values.put(modeRating.playerId() + ":" + modeRating.modeId() + ":" + modeRating.seasonId(), modeRating); return modeRating; }
    }

    private static final class InMemoryGlobalRatingRepository implements GlobalRatingRepository {
        private final Map<String, GlobalRatingSnapshot> values = new ConcurrentHashMap<>();
        @Override public Optional<GlobalRatingSnapshot> find(PlayerId playerId, String seasonId) { return Optional.ofNullable(values.get(playerId + ":" + seasonId)); }
        @Override public List<GlobalRatingSnapshot> findBySeasonId(String seasonId) { return values.values().stream().filter(v -> v.seasonId().equals(seasonId)).toList(); }
        @Override public GlobalRatingSnapshot save(GlobalRatingSnapshot snapshot) { values.put(snapshot.playerId() + ":" + snapshot.seasonId(), snapshot); return snapshot; }
    }
}
