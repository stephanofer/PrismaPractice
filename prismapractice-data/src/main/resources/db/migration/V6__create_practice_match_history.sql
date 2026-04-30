CREATE TABLE IF NOT EXISTS practice_match_history (
    match_id CHAR(36) NOT NULL PRIMARY KEY,
    mode_id VARCHAR(64) NOT NULL,
    queue_type VARCHAR(16) NOT NULL,
    series_format VARCHAR(32) NOT NULL,
    arena_id VARCHAR(64) NOT NULL,
    region_id VARCHAR(32) NOT NULL,
    runtime_server_id VARCHAR(64) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    started_at TIMESTAMP NOT NULL,
    ended_at TIMESTAMP NOT NULL,
    duration_seconds BIGINT NOT NULL,
    winner_player_id CHAR(36) NOT NULL,
    KEY idx_practice_match_history_ended_at (ended_at)
);

CREATE TABLE IF NOT EXISTS practice_match_history_players (
    match_id CHAR(36) NOT NULL,
    player_id CHAR(36) NOT NULL,
    player_name VARCHAR(16) NOT NULL,
    side_name VARCHAR(16) NOT NULL,
    won BOOLEAN NOT NULL,
    sr_before INT NULL,
    sr_after INT NULL,
    ping_final INT NULL,
    final_health DOUBLE NULL,
    inventory_snapshot_json LONGTEXT NOT NULL,
    armor_snapshot_json LONGTEXT NOT NULL,
    offhand_snapshot_json LONGTEXT NOT NULL,
    remaining_consumables_json LONGTEXT NOT NULL,
    active_effects_json LONGTEXT NOT NULL,
    PRIMARY KEY (match_id, player_id),
    KEY idx_practice_match_history_players_player (player_id, match_id)
);

CREATE TABLE IF NOT EXISTS practice_match_history_stats (
    match_id CHAR(36) NOT NULL,
    player_id CHAR(36) NULL,
    scope_name VARCHAR(32) NOT NULL,
    stat_key VARCHAR(64) NOT NULL,
    stat_value VARCHAR(255) NOT NULL,
    KEY idx_practice_match_history_stats_match (match_id)
);

CREATE TABLE IF NOT EXISTS practice_match_history_events (
    match_id CHAR(36) NOT NULL,
    sequence_number INT NOT NULL,
    timestamp_offset_ms BIGINT NOT NULL,
    event_type VARCHAR(32) NOT NULL,
    actor_player_id CHAR(36) NULL,
    target_player_id CHAR(36) NULL,
    details_json LONGTEXT NOT NULL,
    PRIMARY KEY (match_id, sequence_number)
);
