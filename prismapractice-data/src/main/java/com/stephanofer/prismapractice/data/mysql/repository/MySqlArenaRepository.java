package com.stephanofer.prismapractice.data.mysql.repository;

import com.stephanofer.prismapractice.api.arena.ArenaDefinition;
import com.stephanofer.prismapractice.api.arena.ArenaId;
import com.stephanofer.prismapractice.api.arena.ArenaRepository;
import com.stephanofer.prismapractice.api.arena.ArenaType;
import com.stephanofer.prismapractice.api.common.ModeId;
import com.stephanofer.prismapractice.api.common.PlayerType;
import com.stephanofer.prismapractice.api.common.RuntimeType;
import com.stephanofer.prismapractice.api.matchmaking.RegionId;
import com.stephanofer.prismapractice.data.mysql.MySqlStorage;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public final class MySqlArenaRepository implements ArenaRepository {

    private final MySqlStorage storage;

    public MySqlArenaRepository(MySqlStorage storage) {
        this.storage = Objects.requireNonNull(storage, "storage");
    }

    @Override
    public Optional<ArenaDefinition> findById(ArenaId arenaId) {
        return storage.jdbcExecutor().queryOne(
                """
                SELECT arena_id, display_name, arena_type, region_id, runtime_type, server_pool_key,
                       allowed_modes_csv, blocked_modes_csv, allowed_player_types_csv,
                       enabled, selectable, selection_weight, featured
                FROM practice_arenas
                WHERE arena_id = ?
                """,
                statement -> statement.setString(1, arenaId.toString()),
                resultSet -> mapArena(resultSet.getString("arena_id"), resultSet.getString("display_name"), resultSet.getString("arena_type"),
                        resultSet.getString("region_id"), resultSet.getString("runtime_type"), resultSet.getString("server_pool_key"),
                        resultSet.getString("allowed_modes_csv"), resultSet.getString("blocked_modes_csv"), resultSet.getString("allowed_player_types_csv"),
                        resultSet.getBoolean("enabled"), resultSet.getBoolean("selectable"), resultSet.getInt("selection_weight"), resultSet.getBoolean("featured"))
        );
    }

    @Override
    public List<ArenaDefinition> findCompatible(ArenaType arenaType, ModeId modeId, PlayerType playerType, RuntimeType runtimeType, RegionId regionId) {
        return storage.jdbcExecutor().query(
                """
                SELECT arena_id, display_name, arena_type, region_id, runtime_type, server_pool_key,
                       allowed_modes_csv, blocked_modes_csv, allowed_player_types_csv,
                       enabled, selectable, selection_weight, featured
                FROM practice_arenas
                WHERE arena_type = ?
                  AND region_id = ?
                  AND runtime_type = ?
                  AND enabled = TRUE
                  AND selectable = TRUE
                """,
                statement -> {
                    statement.setString(1, arenaType.name());
                    statement.setString(2, regionId.value());
                    statement.setString(3, runtimeType.name());
                },
                resultSet -> mapArena(resultSet.getString("arena_id"), resultSet.getString("display_name"), resultSet.getString("arena_type"),
                        resultSet.getString("region_id"), resultSet.getString("runtime_type"), resultSet.getString("server_pool_key"),
                        resultSet.getString("allowed_modes_csv"), resultSet.getString("blocked_modes_csv"), resultSet.getString("allowed_player_types_csv"),
                        resultSet.getBoolean("enabled"), resultSet.getBoolean("selectable"), resultSet.getInt("selection_weight"), resultSet.getBoolean("featured"))
        ).stream().filter(arena -> arena.allowedPlayerTypes().contains(playerType)
                && (arena.allowedModes().isEmpty() || arena.allowedModes().contains(modeId))
                && !arena.blockedModes().contains(modeId)).toList();
    }

    @Override
    public ArenaDefinition save(ArenaDefinition arenaDefinition) {
        storage.jdbcExecutor().update(
                """
                INSERT INTO practice_arenas (
                    arena_id, display_name, arena_type, region_id, runtime_type, server_pool_key,
                    allowed_modes_csv, blocked_modes_csv, allowed_player_types_csv,
                    enabled, selectable, selection_weight, featured, created_at, updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE
                    display_name = VALUES(display_name),
                    arena_type = VALUES(arena_type),
                    region_id = VALUES(region_id),
                    runtime_type = VALUES(runtime_type),
                    server_pool_key = VALUES(server_pool_key),
                    allowed_modes_csv = VALUES(allowed_modes_csv),
                    blocked_modes_csv = VALUES(blocked_modes_csv),
                    allowed_player_types_csv = VALUES(allowed_player_types_csv),
                    enabled = VALUES(enabled),
                    selectable = VALUES(selectable),
                    selection_weight = VALUES(selection_weight),
                    featured = VALUES(featured),
                    updated_at = VALUES(updated_at)
                """,
                statement -> {
                    Instant now = Instant.now();
                    statement.setString(1, arenaDefinition.arenaId().toString());
                    statement.setString(2, arenaDefinition.displayName());
                    statement.setString(3, arenaDefinition.arenaType().name());
                    statement.setString(4, arenaDefinition.regionId().value());
                    statement.setString(5, arenaDefinition.runtimeType().name());
                    statement.setString(6, arenaDefinition.serverPoolKey());
                    statement.setString(7, arenaDefinition.allowedModes().stream().map(ModeId::value).sorted().collect(Collectors.joining(",")));
                    statement.setString(8, arenaDefinition.blockedModes().stream().map(ModeId::value).sorted().collect(Collectors.joining(",")));
                    statement.setString(9, arenaDefinition.allowedPlayerTypes().stream().map(Enum::name).sorted().collect(Collectors.joining(",")));
                    statement.setBoolean(10, arenaDefinition.enabled());
                    statement.setBoolean(11, arenaDefinition.selectable());
                    statement.setInt(12, arenaDefinition.selectionWeight());
                    statement.setBoolean(13, arenaDefinition.featured());
                    statement.setTimestamp(14, Timestamp.from(now));
                    statement.setTimestamp(15, Timestamp.from(now));
                }
        );
        return arenaDefinition;
    }

    private ArenaDefinition mapArena(
            String arenaId,
            String displayName,
            String arenaType,
            String regionId,
            String runtimeType,
            String serverPoolKey,
            String allowedModes,
            String blockedModes,
            String allowedPlayerTypes,
            boolean enabled,
            boolean selectable,
            int selectionWeight,
            boolean featured
    ) {
        return new ArenaDefinition(
                new ArenaId(arenaId),
                displayName,
                ArenaType.valueOf(arenaType),
                new RegionId(regionId),
                RuntimeType.valueOf(runtimeType),
                serverPoolKey,
                parseModes(allowedModes),
                parseModes(blockedModes),
                parsePlayerTypes(allowedPlayerTypes),
                enabled,
                selectable,
                selectionWeight,
                featured
        );
    }

    private Set<ModeId> parseModes(String raw) {
        if (raw == null || raw.isBlank()) {
            return Set.of();
        }
        return Arrays.stream(raw.split(",")).map(String::trim).filter(value -> !value.isEmpty()).map(ModeId::new).collect(Collectors.toSet());
    }

    private Set<PlayerType> parsePlayerTypes(String raw) {
        if (raw == null || raw.isBlank()) {
            return Set.of();
        }
        return Arrays.stream(raw.split(",")).map(String::trim).filter(value -> !value.isEmpty()).map(PlayerType::valueOf).collect(Collectors.toSet());
    }
}
