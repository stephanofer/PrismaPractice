package com.stephanofer.prismapractice.hub.ui;

import com.stephanofer.prismapractice.api.common.ModeId;
import com.stephanofer.prismapractice.api.common.PlayerId;
import com.stephanofer.prismapractice.api.common.PlayerType;
import com.stephanofer.prismapractice.api.common.QueueId;
import com.stephanofer.prismapractice.api.common.RuntimeType;
import com.stephanofer.prismapractice.api.queue.MatchmakingProfile;
import com.stephanofer.prismapractice.api.queue.QueueDefinition;
import com.stephanofer.prismapractice.api.queue.QueueEntry;
import com.stephanofer.prismapractice.api.queue.QueueEntryRepository;
import com.stephanofer.prismapractice.api.queue.QueueRepository;
import com.stephanofer.prismapractice.api.queue.QueueType;
import com.stephanofer.prismapractice.api.queue.SearchExpansionStrategy;
import com.stephanofer.prismapractice.api.state.PlayerPresenceRepository;
import com.stephanofer.prismapractice.api.state.PlayerState;
import com.stephanofer.prismapractice.api.state.PlayerStatus;
import com.stephanofer.prismapractice.api.state.PlayerSubStatus;
import com.stephanofer.prismapractice.api.state.PracticePresence;
import com.stephanofer.prismapractice.core.application.queue.QueueJoinResult;
import com.stephanofer.prismapractice.core.application.queue.QueueLeaveResult;
import com.stephanofer.prismapractice.core.application.queue.QueueService;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

class HubQueueMenuServiceTest {

    private final PlayerId playerId = new PlayerId(UUID.fromString("11111111-1111-1111-1111-111111111111"));
    private final QueueId rankedSwordId = new QueueId("ranked-sword-1v1");
    private final QueueId rankedAxeId = new QueueId("ranked-axe-1v1");

    @Test
    void view_returnsAvailableForEnabledQueueWithoutActiveEntry() {
        InMemoryQueueRepository queueRepository = new InMemoryQueueRepository(Map.of(rankedSwordId, queue(rankedSwordId, "Ranked Sword 1v1")));
        InMemoryQueueEntryRepository queueEntryRepository = new InMemoryQueueEntryRepository();
        InMemoryPlayerPresenceRepository presenceRepository = new InMemoryPlayerPresenceRepository();
        QueueService queueService = mock(QueueService.class);

        HubQueueMenuService service = new HubQueueMenuService(queueRepository, queueEntryRepository, presenceRepository, queueService);
        QueueMenuView view = service.view(playerId, rankedSwordId);

        assertEquals(QueueMenuState.AVAILABLE, view.state());
        assertEquals("Ranked Sword 1v1", view.definition().displayName());
        assertEquals(0, view.playerCount());
    }

    @Test
    void view_returnsOtherQueueWhenPlayerIsAlreadyQueuedElsewhere() {
        InMemoryQueueRepository queueRepository = new InMemoryQueueRepository(Map.of(
                rankedSwordId, queue(rankedSwordId, "Ranked Sword 1v1"),
                rankedAxeId, queue(rankedAxeId, "Ranked Axe 1v1")
        ));
        InMemoryQueueEntryRepository queueEntryRepository = new InMemoryQueueEntryRepository();
        queueEntryRepository.save(entry(playerId, rankedAxeId));
        InMemoryPlayerPresenceRepository presenceRepository = new InMemoryPlayerPresenceRepository();
        QueueService queueService = mock(QueueService.class);

        HubQueueMenuService service = new HubQueueMenuService(queueRepository, queueEntryRepository, presenceRepository, queueService);
        QueueMenuView view = service.view(playerId, rankedSwordId);

        assertEquals(QueueMenuState.OTHER_QUEUE, view.state());
        assertEquals("Ranked Axe 1v1", view.activeQueueName());
    }

    @Test
    void join_usesPresenceServerIdAndReturnsCurrentQueueAfterSuccess() {
        InMemoryQueueRepository queueRepository = new InMemoryQueueRepository(Map.of(rankedSwordId, queue(rankedSwordId, "Ranked Sword 1v1")));
        InMemoryQueueEntryRepository queueEntryRepository = new InMemoryQueueEntryRepository();
        InMemoryPlayerPresenceRepository presenceRepository = new InMemoryPlayerPresenceRepository();
        presenceRepository.save(PracticePresence.online(playerId, RuntimeType.HUB, "hub-lima-1", Instant.parse("2026-05-01T00:00:00Z")));
        QueueService queueService = mock(QueueService.class);
        doAnswer(invocation -> {
            QueueEntry saved = entry(invocation.getArgument(0), invocation.getArgument(1));
            queueEntryRepository.save(saved);
            return QueueJoinResult.success(saved, new PlayerState(playerId, PlayerStatus.IN_QUEUE, PlayerSubStatus.WAITING_MATCH, Instant.parse("2026-05-01T00:00:01Z")));
        }).when(queueService).joinQueue(eq(playerId), eq(rankedSwordId), eq("hub-lima-1"));

        HubQueueMenuService service = new HubQueueMenuService(queueRepository, queueEntryRepository, presenceRepository, queueService);
        QueueMenuActionResult result = service.join(playerId, rankedSwordId);

        assertTrue(result.success());
        assertEquals(QueueMenuAction.JOINED, result.action());
        assertEquals(QueueMenuState.CURRENT_QUEUE, result.view().state());
        assertEquals("Ranked Sword 1v1", result.view().activeQueueName());
    }

    @Test
    void join_switchesFromOtherQueueWithoutManualLeave() {
        InMemoryQueueRepository queueRepository = new InMemoryQueueRepository(Map.of(
                rankedSwordId, queue(rankedSwordId, "Ranked Sword 1v1"),
                rankedAxeId, queue(rankedAxeId, "Ranked Axe 1v1")
        ));
        InMemoryQueueEntryRepository queueEntryRepository = new InMemoryQueueEntryRepository();
        queueEntryRepository.save(entry(playerId, rankedAxeId));
        InMemoryPlayerPresenceRepository presenceRepository = new InMemoryPlayerPresenceRepository();
        QueueService queueService = mock(QueueService.class);

        doAnswer(invocation -> {
            PlayerId targetPlayerId = invocation.getArgument(0);
            QueueEntry removed = queueEntryRepository.findByPlayerId(targetPlayerId).orElseThrow();
            queueEntryRepository.removeByPlayerId(targetPlayerId);
            return QueueLeaveResult.success(removed, new PlayerState(targetPlayerId, PlayerStatus.HUB, PlayerSubStatus.NONE, Instant.parse("2026-05-01T00:00:01Z")), false);
        }).when(queueService).leaveQueue(eq(playerId));
        doAnswer(invocation -> {
            QueueEntry saved = entry(invocation.getArgument(0), invocation.getArgument(1));
            queueEntryRepository.save(saved);
            return QueueJoinResult.success(saved, new PlayerState(playerId, PlayerStatus.IN_QUEUE, PlayerSubStatus.WAITING_MATCH, Instant.parse("2026-05-01T00:00:02Z")));
        }).when(queueService).joinQueue(eq(playerId), eq(rankedSwordId), eq("hub"));

        HubQueueMenuService service = new HubQueueMenuService(queueRepository, queueEntryRepository, presenceRepository, queueService);
        QueueMenuActionResult result = service.join(playerId, rankedSwordId);

        assertTrue(result.success());
        assertEquals(QueueMenuAction.SWITCHED, result.action());
        assertEquals(QueueMenuState.CURRENT_QUEUE, result.view().state());
        assertEquals(rankedSwordId, result.joinedEntry().queueId());
    }

    @Test
    void click_currentQueueLeavesQueueAndReturnsAvailableView() {
        InMemoryQueueRepository queueRepository = new InMemoryQueueRepository(Map.of(rankedSwordId, queue(rankedSwordId, "Ranked Sword 1v1")));
        InMemoryQueueEntryRepository queueEntryRepository = new InMemoryQueueEntryRepository();
        queueEntryRepository.save(entry(playerId, rankedSwordId));
        InMemoryPlayerPresenceRepository presenceRepository = new InMemoryPlayerPresenceRepository();
        QueueService queueService = mock(QueueService.class);

        doAnswer(invocation -> {
            PlayerId targetPlayerId = invocation.getArgument(0);
            QueueEntry removed = queueEntryRepository.findByPlayerId(targetPlayerId).orElseThrow();
            queueEntryRepository.removeByPlayerId(targetPlayerId);
            return QueueLeaveResult.success(removed, new PlayerState(targetPlayerId, PlayerStatus.HUB, PlayerSubStatus.NONE, Instant.parse("2026-05-01T00:00:01Z")), false);
        }).when(queueService).leaveQueue(eq(playerId));

        HubQueueMenuService service = new HubQueueMenuService(queueRepository, queueEntryRepository, presenceRepository, queueService);
        QueueMenuActionResult result = service.click(playerId, rankedSwordId);

        assertTrue(result.success());
        assertEquals(QueueMenuAction.LEFT_CURRENT, result.action());
        assertEquals(QueueMenuState.AVAILABLE, result.view().state());
        assertNull(result.joinedEntry());
    }

    private QueueDefinition queue(QueueId queueId, String displayName) {
        return new QueueDefinition(
                queueId,
                new ModeId("sword"),
                displayName,
                QueueType.RANKED,
                PlayerType.ONE_VS_ONE,
                MatchmakingProfile.QUALITY_FIRST,
                true,
                true,
                true,
                true,
                true,
                true,
                SearchExpansionStrategy.RANKED_STANDARD,
                true,
                Set.of(PlayerStatus.HUB),
                Set.of(PlayerStatus.IN_QUEUE, PlayerStatus.IN_MATCH),
                true,
                true,
                true,
                true,
                RuntimeType.MATCH
        );
    }

    private QueueEntry entry(PlayerId playerId, QueueId queueId) {
        return new QueueEntry(playerId, queueId, Instant.parse("2026-05-01T00:00:00Z"), MatchmakingProfile.QUALITY_FIRST, QueueType.RANKED, PlayerType.ONE_VS_ONE, "hub-1");
    }

    private static final class InMemoryQueueRepository implements QueueRepository {

        private final Map<QueueId, QueueDefinition> values;

        private InMemoryQueueRepository(Map<QueueId, QueueDefinition> values) {
            this.values = new ConcurrentHashMap<>(values);
        }

        @Override
        public Optional<QueueDefinition> findById(QueueId queueId) {
            return Optional.ofNullable(values.get(queueId));
        }

        @Override
        public QueueDefinition save(QueueDefinition queueDefinition) {
            values.put(queueDefinition.queueId(), queueDefinition);
            return queueDefinition;
        }
    }

    private static final class InMemoryQueueEntryRepository implements QueueEntryRepository {

        private final Map<PlayerId, QueueEntry> values = new ConcurrentHashMap<>();

        @Override
        public Optional<QueueEntry> findByPlayerId(PlayerId playerId) {
            return Optional.ofNullable(values.get(playerId));
        }

        @Override
        public List<QueueEntry> findByQueueId(QueueId queueId) {
            return values.values().stream().filter(entry -> entry.queueId().equals(queueId)).toList();
        }

        @Override
        public QueueEntry save(QueueEntry entry) {
            values.put(entry.playerId(), entry);
            return entry;
        }

        @Override
        public boolean removeByPlayerId(PlayerId playerId) {
            return values.remove(playerId) != null;
        }

        @Override
        public boolean remove(QueueId queueId, PlayerId playerId) {
            QueueEntry current = values.get(playerId);
            if (current == null || !current.queueId().equals(queueId)) {
                return false;
            }
            values.remove(playerId);
            return true;
        }
    }

    private static final class InMemoryPlayerPresenceRepository implements PlayerPresenceRepository {

        private final Map<PlayerId, PracticePresence> values = new ConcurrentHashMap<>();

        @Override
        public Optional<PracticePresence> find(PlayerId playerId) {
            return Optional.ofNullable(values.get(playerId));
        }

        @Override
        public PracticePresence save(PracticePresence presence) {
            values.put(presence.playerId(), presence);
            return presence;
        }

        @Override
        public void delete(PlayerId playerId) {
            values.remove(playerId);
        }
    }
}
