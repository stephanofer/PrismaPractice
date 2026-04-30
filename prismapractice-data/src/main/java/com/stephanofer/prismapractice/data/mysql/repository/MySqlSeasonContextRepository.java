package com.stephanofer.prismapractice.data.mysql.repository;

import com.stephanofer.prismapractice.api.rating.SeasonContext;
import com.stephanofer.prismapractice.api.rating.SeasonContextRepository;
import com.stephanofer.prismapractice.data.mysql.MySqlStorage;

import java.sql.Timestamp;
import java.util.Objects;
import java.util.Optional;

public final class MySqlSeasonContextRepository implements SeasonContextRepository {

    private final MySqlStorage storage;

    public MySqlSeasonContextRepository(MySqlStorage storage) { this.storage = Objects.requireNonNull(storage, "storage"); }

    @Override
    public Optional<SeasonContext> findActive() {
        return storage.jdbcExecutor().queryOne("SELECT season_id, display_name, status, started_at, ended_at FROM practice_seasons WHERE status = 'ACTIVE' ORDER BY started_at DESC", st -> {}, rs -> new SeasonContext(rs.getString("season_id"), rs.getString("display_name"), rs.getString("status"), rs.getTimestamp("started_at").toInstant(), toInstant(rs.getTimestamp("ended_at"))));
    }

    private static java.time.Instant toInstant(Timestamp value) { return value == null ? null : value.toInstant(); }
}
