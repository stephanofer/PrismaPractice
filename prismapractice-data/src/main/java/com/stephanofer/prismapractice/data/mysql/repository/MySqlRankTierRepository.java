package com.stephanofer.prismapractice.data.mysql.repository;

import com.stephanofer.prismapractice.api.rating.RankTier;
import com.stephanofer.prismapractice.api.rating.RankTierRepository;
import com.stephanofer.prismapractice.data.mysql.MySqlStorage;

import java.util.List;
import java.util.Objects;

public final class MySqlRankTierRepository implements RankTierRepository {

    private final MySqlStorage storage;

    public MySqlRankTierRepository(MySqlStorage storage) { this.storage = Objects.requireNonNull(storage, "storage"); }

    @Override
    public List<RankTier> findEnabled() {
        return storage.jdbcExecutor().query("SELECT rank_key, display_name, min_sr, max_sr, sort_order, enabled FROM practice_rank_tiers WHERE enabled = TRUE ORDER BY sort_order ASC", rs -> new RankTier(rs.getString("rank_key"), rs.getString("display_name"), rs.getInt("min_sr"), (Integer) rs.getObject("max_sr"), rs.getInt("sort_order"), rs.getBoolean("enabled")));
    }
}
