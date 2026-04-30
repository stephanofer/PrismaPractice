package com.stephanofer.prismapractice.data.mysql.repository;

import com.stephanofer.prismapractice.api.common.PlayerId;
import com.stephanofer.prismapractice.api.history.*;
import com.stephanofer.prismapractice.api.match.MatchId;
import com.stephanofer.prismapractice.api.match.MatchSide;
import com.stephanofer.prismapractice.api.match.SeriesFormat;
import com.stephanofer.prismapractice.api.matchmaking.RegionId;
import com.stephanofer.prismapractice.api.queue.QueueType;
import com.stephanofer.prismapractice.api.arena.ArenaId;
import com.stephanofer.prismapractice.api.common.ModeId;
import com.stephanofer.prismapractice.data.mysql.MySqlStorage;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;

public final class MySqlMatchHistoryRepository implements MatchHistoryRepository {

    private final MySqlStorage storage;

    public MySqlMatchHistoryRepository(MySqlStorage storage) {
        this.storage = Objects.requireNonNull(storage, "storage");
    }

    @Override
    public boolean exists(MatchId matchId) {
        return storage.jdbcExecutor().queryOne("SELECT 1 FROM practice_match_history WHERE match_id = ?", st -> st.setString(1, matchId.toString()), rs -> 1).isPresent();
    }

    @Override
    public MatchHistorySummary save(MatchHistorySummary summary) {
        storage.transactionRunner().inTransaction(connection -> {
            try (var statement = connection.prepareStatement("""
                    INSERT INTO practice_match_history (
                        match_id, mode_id, queue_type, series_format, arena_id, region_id, runtime_server_id,
                        created_at, started_at, ended_at, duration_seconds, winner_player_id
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """)) {
                statement.setString(1, summary.matchId().toString());
                statement.setString(2, summary.modeId().toString());
                statement.setString(3, summary.queueType().name());
                statement.setString(4, summary.seriesFormat().name());
                statement.setString(5, summary.arenaId().toString());
                statement.setString(6, summary.regionId().value());
                statement.setString(7, summary.runtimeServerId());
                statement.setTimestamp(8, Timestamp.from(summary.createdAt()));
                statement.setTimestamp(9, Timestamp.from(summary.startedAt()));
                statement.setTimestamp(10, Timestamp.from(summary.endedAt()));
                statement.setLong(11, summary.durationSeconds());
                statement.setString(12, summary.winnerPlayerId().toString());
                statement.executeUpdate();
            }
            try (var deletePlayers = connection.prepareStatement("DELETE FROM practice_match_history_players WHERE match_id = ?");
                 var deleteStats = connection.prepareStatement("DELETE FROM practice_match_history_stats WHERE match_id = ?");
                 var deleteEvents = connection.prepareStatement("DELETE FROM practice_match_history_events WHERE match_id = ?")) {
                deletePlayers.setString(1, summary.matchId().toString()); deletePlayers.executeUpdate();
                deleteStats.setString(1, summary.matchId().toString()); deleteStats.executeUpdate();
                deleteEvents.setString(1, summary.matchId().toString()); deleteEvents.executeUpdate();
            }
            try (var st = connection.prepareStatement("""
                    INSERT INTO practice_match_history_players (
                        match_id, player_id, player_name, side_name, won, sr_before, sr_after, ping_final, final_health,
                        inventory_snapshot_json, armor_snapshot_json, offhand_snapshot_json, remaining_consumables_json, active_effects_json
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """)) {
                for (MatchHistoryPlayerSnapshot player : summary.players()) {
                    st.setString(1, summary.matchId().toString());
                    st.setString(2, player.playerId().toString());
                    st.setString(3, player.playerName());
                    st.setString(4, player.side().name());
                    st.setBoolean(5, player.won());
                    nullableInt(st, 6, player.srBefore());
                    nullableInt(st, 7, player.srAfter());
                    nullableInt(st, 8, player.pingFinal());
                    nullableDouble(st, 9, player.finalHealth());
                    st.setString(10, encodeSlotItems(player.inventorySnapshot().slotItems()));
                    st.setString(11, encodeArmorItems(player.inventorySnapshot().armorItems()));
                    st.setString(12, encodeItem(player.inventorySnapshot().offhandItem()));
                    st.setString(13, encodeConsumables(player.remainingConsumables()));
                    st.setString(14, encodeStringList(player.activeEffects()));
                    st.addBatch();
                }
                st.executeBatch();
            }
            try (var st = connection.prepareStatement("INSERT INTO practice_match_history_stats (match_id, player_id, scope_name, stat_key, stat_value) VALUES (?, ?, ?, ?, ?)")) {
                for (MatchHistoryStatEntry stat : summary.stats()) {
                    st.setString(1, summary.matchId().toString());
                    st.setString(2, null);
                    st.setString(3, stat.scope());
                    st.setString(4, stat.key());
                    st.setString(5, stat.value());
                    st.addBatch();
                }
                st.executeBatch();
            }
            try (var st = connection.prepareStatement("INSERT INTO practice_match_history_events (match_id, sequence_number, timestamp_offset_ms, event_type, actor_player_id, target_player_id, details_json) VALUES (?, ?, ?, ?, ?, ?, ?)")) {
                int sequence = 0;
                for (MatchHistoryEvent event : summary.events()) {
                    st.setString(1, summary.matchId().toString());
                    st.setInt(2, sequence++);
                    st.setLong(3, event.timestampOffsetMs());
                    st.setString(4, event.eventType().name());
                    st.setString(5, event.actorPlayerId() == null ? null : event.actorPlayerId().toString());
                    st.setString(6, event.targetPlayerId() == null ? null : event.targetPlayerId().toString());
                    st.setString(7, event.detailsJson());
                    st.addBatch();
                }
                st.executeBatch();
            }
            return summary;
        });
        return summary;
    }

    @Override
    public Optional<MatchHistorySummary> findByMatchId(MatchId matchId) {
        return baseSummary(matchId);
    }

    @Override
    public List<MatchHistorySummary> findRecentByPlayerId(PlayerId playerId, int limit, int offset) {
        List<MatchId> ids = storage.jdbcExecutor().query("""
                SELECT match_id FROM practice_match_history_players
                WHERE player_id = ?
                ORDER BY match_id DESC
                LIMIT ? OFFSET ?
                """, st -> {
            st.setString(1, playerId.toString());
            st.setInt(2, limit);
            st.setInt(3, offset);
        }, rs -> MatchId.fromString(rs.getString("match_id")));
        List<MatchHistorySummary> summaries = new ArrayList<>();
        for (MatchId id : ids) {
            baseSummary(id).ifPresent(summaries::add);
        }
        return List.copyOf(summaries);
    }

    private Optional<MatchHistorySummary> baseSummary(MatchId matchId) {
        Optional<MatchHistorySummary> base = storage.jdbcExecutor().queryOne("SELECT * FROM practice_match_history WHERE match_id = ?", st -> st.setString(1, matchId.toString()), rs -> new MatchHistorySummary(
                MatchId.fromString(rs.getString("match_id")),
                new ModeId(rs.getString("mode_id")),
                QueueType.valueOf(rs.getString("queue_type")),
                SeriesFormat.valueOf(rs.getString("series_format")),
                new ArenaId(rs.getString("arena_id")),
                new RegionId(rs.getString("region_id")),
                rs.getString("runtime_server_id"),
                rs.getTimestamp("created_at").toInstant(),
                rs.getTimestamp("started_at").toInstant(),
                rs.getTimestamp("ended_at").toInstant(),
                rs.getLong("duration_seconds"),
                PlayerId.fromString(rs.getString("winner_player_id")),
                List.of(),
                List.of(),
                List.of()
        ));
        if (base.isEmpty()) return Optional.empty();
        MatchHistorySummary summary = base.get();
        List<MatchHistoryPlayerSnapshot> players = storage.jdbcExecutor().query("SELECT * FROM practice_match_history_players WHERE match_id = ? ORDER BY side_name ASC", st -> st.setString(1, matchId.toString()), rs -> new MatchHistoryPlayerSnapshot(
                PlayerId.fromString(rs.getString("player_id")),
                rs.getString("player_name"),
                MatchSide.valueOf(rs.getString("side_name")),
                rs.getBoolean("won"),
                (Integer) rs.getObject("sr_before"),
                (Integer) rs.getObject("sr_after"),
                (Integer) rs.getObject("ping_final"),
                (Double) rs.getObject("final_health"),
                new InventorySnapshot(decodeSlotItems(rs.getString("inventory_snapshot_json")), decodeArmorItems(rs.getString("armor_snapshot_json")), decodeItem(rs.getString("offhand_snapshot_json"))),
                decodeConsumables(rs.getString("remaining_consumables_json")),
                decodeStringList(rs.getString("active_effects_json"))
        ));
        List<MatchHistoryStatEntry> stats = storage.jdbcExecutor().query("SELECT * FROM practice_match_history_stats WHERE match_id = ? ORDER BY stat_key ASC", st -> st.setString(1, matchId.toString()), rs -> new MatchHistoryStatEntry(rs.getString("scope_name"), rs.getString("stat_key"), rs.getString("stat_value")));
        List<MatchHistoryEvent> events = storage.jdbcExecutor().query("SELECT * FROM practice_match_history_events WHERE match_id = ? ORDER BY sequence_number ASC", st -> st.setString(1, matchId.toString()), rs -> new MatchHistoryEvent(rs.getLong("timestamp_offset_ms"), MatchHistoryEventType.valueOf(rs.getString("event_type")), nullablePlayer(rs.getString("actor_player_id")), nullablePlayer(rs.getString("target_player_id")), rs.getString("details_json")));
        return Optional.of(new MatchHistorySummary(summary.matchId(), summary.modeId(), summary.queueType(), summary.seriesFormat(), summary.arenaId(), summary.regionId(), summary.runtimeServerId(), summary.createdAt(), summary.startedAt(), summary.endedAt(), summary.durationSeconds(), summary.winnerPlayerId(), players, stats, events));
    }

    private static PlayerId nullablePlayer(String raw) { return raw == null || raw.isBlank() ? null : PlayerId.fromString(raw); }
    private static void nullableInt(java.sql.PreparedStatement st, int index, Integer value) throws java.sql.SQLException { if (value == null) st.setNull(index, java.sql.Types.INTEGER); else st.setInt(index, value); }
    private static void nullableDouble(java.sql.PreparedStatement st, int index, Double value) throws java.sql.SQLException { if (value == null) st.setNull(index, java.sql.Types.DOUBLE); else st.setDouble(index, value); }

    private static String encodeSlotItems(Map<Integer, ItemSnapshot> slotItems) { return slotItems.entrySet().stream().sorted(Map.Entry.comparingByKey()).map(e -> e.getKey() + "=" + encodeItem(e.getValue())).reduce((a,b) -> a + ";" + b).orElse(""); }
    private static Map<Integer, ItemSnapshot> decodeSlotItems(String raw) { Map<Integer, ItemSnapshot> values = new LinkedHashMap<>(); if (raw == null || raw.isBlank()) return values; for (String part : raw.split(";")) { String[] entry = part.split("=", 2); if (entry.length == 2) values.put(Integer.parseInt(entry[0]), decodeItem(entry[1])); } return values; }
    private static String encodeArmorItems(Map<String, ItemSnapshot> armorItems) { return armorItems.entrySet().stream().sorted(Map.Entry.comparingByKey()).map(e -> e.getKey() + "=" + encodeItem(e.getValue())).reduce((a,b) -> a + ";" + b).orElse(""); }
    private static Map<String, ItemSnapshot> decodeArmorItems(String raw) { Map<String, ItemSnapshot> values = new LinkedHashMap<>(); if (raw == null || raw.isBlank()) return values; for (String part : raw.split(";")) { String[] entry = part.split("=", 2); if (entry.length == 2) values.put(entry[0], decodeItem(entry[1])); } return values; }
    private static String encodeConsumables(Map<String, Integer> values) { return values.entrySet().stream().sorted(Map.Entry.comparingByKey()).map(e -> escape(e.getKey()) + ":" + e.getValue()).reduce((a,b) -> a + ";" + b).orElse(""); }
    private static Map<String, Integer> decodeConsumables(String raw) { Map<String, Integer> values = new LinkedHashMap<>(); if (raw == null || raw.isBlank()) return values; for (String part : raw.split(";")) { String[] entry = part.split(":", 2); if (entry.length == 2) values.put(unescape(entry[0]), Integer.parseInt(entry[1])); } return values; }
    private static String encodeStringList(List<String> values) { return values.stream().map(MySqlMatchHistoryRepository::escape).reduce((a,b) -> a + ";" + b).orElse(""); }
    private static List<String> decodeStringList(String raw) { if (raw == null || raw.isBlank()) return List.of(); return Arrays.stream(raw.split(";", -1)).map(MySqlMatchHistoryRepository::unescape).toList(); }
    private static String encodeItem(ItemSnapshot item) {
        if (item == null) return "";
        String enchants = item.enchants().entrySet().stream().sorted(Map.Entry.comparingByKey()).map(e -> escape(e.getKey()) + ":" + e.getValue()).reduce((a,b) -> a + "," + b).orElse("");
        return String.join("|",
                escape(item.materialKey()),
                Integer.toString(item.amount()),
                escape(item.displayName()),
                escape(item.lore()),
                item.customModelData() == null ? "" : item.customModelData().toString(),
                enchants,
                item.damage() == null ? "" : item.damage().toString());
    }
    private static ItemSnapshot decodeItem(String raw) {
        if (raw == null || raw.isBlank()) return null;
        String[] parts = raw.split("\\|", -1);
        Map<String, Integer> enchants = new LinkedHashMap<>();
        if (parts.length > 5 && !parts[5].isBlank()) {
            for (String part : parts[5].split(",")) {
                String[] enchant = part.split(":", 2);
                if (enchant.length == 2) enchants.put(unescape(enchant[0]), Integer.parseInt(enchant[1]));
            }
        }
        return new ItemSnapshot(unescape(parts[0]), Integer.parseInt(parts[1]), unescape(parts[2]), unescape(parts[3]), parts[4].isBlank() ? null : Integer.parseInt(parts[4]), enchants, parts.length < 7 || parts[6].isBlank() ? null : Integer.parseInt(parts[6]));
    }
    private static String escape(String value) { return value == null ? "" : value.replace("\\", "\\\\").replace("|", "\\p").replace(";", "\\s").replace(":", "\\c").replace("=", "\\e").replace(",", "\\m"); }
    private static String unescape(String value) { return value == null ? "" : value.replace("\\m", ",").replace("\\e", "=").replace("\\c", ":").replace("\\s", ";").replace("\\p", "|").replace("\\\\", "\\"); }
}
