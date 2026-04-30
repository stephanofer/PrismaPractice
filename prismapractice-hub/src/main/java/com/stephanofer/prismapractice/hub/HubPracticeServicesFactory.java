package com.stephanofer.prismapractice.hub;

import com.stephanofer.prismapractice.api.arena.ArenaOperationalStateRepository;
import com.stephanofer.prismapractice.api.arena.ArenaRepository;
import com.stephanofer.prismapractice.api.arena.ArenaReservationRepository;
import com.stephanofer.prismapractice.api.history.MatchHistoryRepository;
import com.stephanofer.prismapractice.api.leaderboard.LeaderboardProjectionRepository;
import com.stephanofer.prismapractice.api.match.ActiveMatchRepository;
import com.stephanofer.prismapractice.api.match.MatchRepository;
import com.stephanofer.prismapractice.api.matchmaking.MatchmakingSnapshotRepository;
import com.stephanofer.prismapractice.api.profile.ProfileRepository;
import com.stephanofer.prismapractice.api.queue.PlayerPartyIndexRepository;
import com.stephanofer.prismapractice.api.queue.QueueEntryRepository;
import com.stephanofer.prismapractice.api.queue.QueueRepository;
import com.stephanofer.prismapractice.api.rating.GlobalRatingRepository;
import com.stephanofer.prismapractice.api.rating.ModeRatingRepository;
import com.stephanofer.prismapractice.api.rating.RankTierRepository;
import com.stephanofer.prismapractice.api.rating.RatingChangeRepository;
import com.stephanofer.prismapractice.api.rating.SeasonContextRepository;
import com.stephanofer.prismapractice.core.application.arena.ArenaAllocationService;
import com.stephanofer.prismapractice.core.application.history.HistoryService;
import com.stephanofer.prismapractice.core.application.leaderboard.LeaderboardProjectionService;
import com.stephanofer.prismapractice.core.application.match.MatchService;
import com.stephanofer.prismapractice.core.application.matchmaking.MatchmakingService;
import com.stephanofer.prismapractice.core.application.rating.RatingService;
import com.stephanofer.prismapractice.api.state.PlayerOperationLockRepository;
import com.stephanofer.prismapractice.api.state.PlayerPresenceRepository;
import com.stephanofer.prismapractice.api.state.PlayerStateRepository;
import com.stephanofer.prismapractice.core.application.profile.ProfileService;
import com.stephanofer.prismapractice.core.application.queue.QueueService;
import com.stephanofer.prismapractice.core.application.state.PlayerStateService;
import com.stephanofer.prismapractice.data.mysql.MySqlStorage;
import com.stephanofer.prismapractice.data.mysql.repository.MySqlArenaRepository;
import com.stephanofer.prismapractice.data.mysql.repository.MySqlGlobalRatingRepository;
import com.stephanofer.prismapractice.data.mysql.repository.MySqlMatchRepository;
import com.stephanofer.prismapractice.data.mysql.repository.MySqlMatchHistoryRepository;
import com.stephanofer.prismapractice.data.mysql.repository.MySqlModeRatingRepository;
import com.stephanofer.prismapractice.data.mysql.repository.MySqlProfileRepository;
import com.stephanofer.prismapractice.data.mysql.repository.MySqlQueueRepository;
import com.stephanofer.prismapractice.data.mysql.repository.MySqlRankTierRepository;
import com.stephanofer.prismapractice.data.mysql.repository.MySqlRatingChangeRepository;
import com.stephanofer.prismapractice.data.mysql.repository.MySqlSeasonContextRepository;
import com.stephanofer.prismapractice.data.redis.RedisStorage;
import com.stephanofer.prismapractice.data.redis.repository.RedisArenaOperationalStateRepository;
import com.stephanofer.prismapractice.data.redis.repository.RedisArenaReservationRepository;
import com.stephanofer.prismapractice.data.redis.repository.RedisActiveMatchRepository;
import com.stephanofer.prismapractice.data.redis.repository.RedisLeaderboardProjectionRepository;
import com.stephanofer.prismapractice.data.redis.repository.RedisPlayerOperationLockRepository;
import com.stephanofer.prismapractice.data.redis.repository.RedisPlayerPartyIndexRepository;
import com.stephanofer.prismapractice.data.redis.repository.RedisPlayerPresenceRepository;
import com.stephanofer.prismapractice.data.redis.repository.RedisPlayerStateRepository;
import com.stephanofer.prismapractice.data.redis.repository.RedisQueueEntryRepository;
import com.stephanofer.prismapractice.data.redis.repository.RedisMatchmakingSnapshotRepository;

import java.time.Clock;

final class HubPracticeServicesFactory {

    private HubPracticeServicesFactory() {
    }

    static HubPracticeServices create(MySqlStorage storage, RedisStorage redisStorage) {
        ProfileRepository profileRepository = new MySqlProfileRepository(storage);
        PlayerStateRepository playerStateRepository = new RedisPlayerStateRepository(redisStorage);
        PlayerPresenceRepository playerPresenceRepository = new RedisPlayerPresenceRepository(redisStorage);
        PlayerOperationLockRepository playerOperationLockRepository = new RedisPlayerOperationLockRepository(redisStorage);
        QueueRepository queueRepository = new MySqlQueueRepository(storage);
        QueueEntryRepository queueEntryRepository = new RedisQueueEntryRepository(redisStorage);
        MatchmakingSnapshotRepository matchmakingSnapshotRepository = new RedisMatchmakingSnapshotRepository(redisStorage);
        ArenaRepository arenaRepository = new MySqlArenaRepository(storage);
        ArenaOperationalStateRepository arenaOperationalStateRepository = new RedisArenaOperationalStateRepository(redisStorage);
        ArenaReservationRepository arenaReservationRepository = new RedisArenaReservationRepository(redisStorage);
        MatchRepository matchRepository = new MySqlMatchRepository(storage);
        ActiveMatchRepository activeMatchRepository = new RedisActiveMatchRepository(redisStorage);
        ModeRatingRepository modeRatingRepository = new MySqlModeRatingRepository(storage);
        GlobalRatingRepository globalRatingRepository = new MySqlGlobalRatingRepository(storage);
        RankTierRepository rankTierRepository = new MySqlRankTierRepository(storage);
        SeasonContextRepository seasonContextRepository = new MySqlSeasonContextRepository(storage);
        RatingChangeRepository ratingChangeRepository = new MySqlRatingChangeRepository(storage);
        MatchHistoryRepository matchHistoryRepository = new MySqlMatchHistoryRepository(storage);
        LeaderboardProjectionRepository leaderboardProjectionRepository = new RedisLeaderboardProjectionRepository(redisStorage);
        PlayerPartyIndexRepository playerPartyIndexRepository = new RedisPlayerPartyIndexRepository(redisStorage);

        Clock clock = Clock.systemUTC();
        ProfileService profileService = new ProfileService(profileRepository, clock);
        PlayerStateService playerStateService = new PlayerStateService(playerStateRepository, playerPresenceRepository, playerOperationLockRepository, clock);
        QueueService queueService = new QueueService(queueRepository, queueEntryRepository, matchmakingSnapshotRepository, profileRepository, playerPartyIndexRepository, playerStateService, playerOperationLockRepository, clock);
        MatchmakingService matchmakingService = new MatchmakingService(queueRepository, queueEntryRepository, matchmakingSnapshotRepository, playerStateService, playerOperationLockRepository, clock);
        ArenaAllocationService arenaAllocationService = new ArenaAllocationService(arenaRepository, arenaOperationalStateRepository, arenaReservationRepository, matchmakingService, playerStateService, playerOperationLockRepository, clock, redisStorage.ttlPolicies().arenaLock().multipliedBy(2));
        MatchService matchService = new MatchService(matchRepository, activeMatchRepository, arenaAllocationService, matchmakingService, playerStateService, clock);
        RatingService ratingService = new RatingService(matchRepository, modeRatingRepository, globalRatingRepository, rankTierRepository, seasonContextRepository, ratingChangeRepository, clock);
        HistoryService historyService = new HistoryService(matchHistoryRepository);
        LeaderboardProjectionService leaderboardProjectionService = new LeaderboardProjectionService(leaderboardProjectionRepository, profileRepository, modeRatingRepository, globalRatingRepository, seasonContextRepository);

        return new HubPracticeServices(
                profileRepository,
                playerStateRepository,
                playerPresenceRepository,
                playerOperationLockRepository,
                queueRepository,
                queueEntryRepository,
                matchmakingSnapshotRepository,
                arenaRepository,
                arenaOperationalStateRepository,
                arenaReservationRepository,
                matchRepository,
                activeMatchRepository,
                modeRatingRepository,
                globalRatingRepository,
                rankTierRepository,
                seasonContextRepository,
                ratingChangeRepository,
                matchHistoryRepository,
                leaderboardProjectionRepository,
                playerPartyIndexRepository,
                profileService,
                playerStateService,
                queueService,
                matchmakingService,
                arenaAllocationService,
                matchService,
                ratingService,
                historyService,
                leaderboardProjectionService
        );
    }
}
