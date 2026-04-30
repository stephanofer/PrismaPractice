CREATE TABLE IF NOT EXISTS practice_players (
    player_id CHAR(36) NOT NULL PRIMARY KEY,
    current_name VARCHAR(16) NOT NULL,
    normalized_name VARCHAR(16) NOT NULL,
    first_seen_at TIMESTAMP NOT NULL,
    last_seen_at TIMESTAMP NOT NULL,
    profile_visibility VARCHAR(16) NOT NULL,
    current_global_rating INT NOT NULL,
    current_global_rank_key VARCHAR(64) NOT NULL,
    UNIQUE KEY uk_practice_players_normalized_name (normalized_name)
);

CREATE TABLE IF NOT EXISTS practice_player_settings (
    player_id CHAR(36) NOT NULL PRIMARY KEY,
    chat_enabled BOOLEAN NOT NULL,
    allow_duels BOOLEAN NOT NULL,
    friends_only_duels BOOLEAN NOT NULL,
    allow_party_invites BOOLEAN NOT NULL,
    allow_spectators BOOLEAN NOT NULL,
    show_lobby_players BOOLEAN NOT NULL,
    show_scoreboard BOOLEAN NOT NULL,
    allow_event_alerts BOOLEAN NOT NULL,
    ping_range_preference VARCHAR(32) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_practice_player_settings_player FOREIGN KEY (player_id) REFERENCES practice_players (player_id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS practice_modes (
    mode_id VARCHAR(64) NOT NULL PRIMARY KEY,
    display_name VARCHAR(64) NOT NULL,
    enabled BOOLEAN NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS practice_queues (
    queue_id VARCHAR(64) NOT NULL PRIMARY KEY,
    mode_id VARCHAR(64) NOT NULL,
    display_name VARCHAR(64) NOT NULL,
    queue_type VARCHAR(16) NOT NULL,
    player_type VARCHAR(32) NOT NULL,
    matchmaking_profile VARCHAR(32) NOT NULL,
    enabled BOOLEAN NOT NULL,
    visible_in_menu BOOLEAN NOT NULL,
    rated BOOLEAN NOT NULL,
    uses_skill_rating BOOLEAN NOT NULL,
    uses_ping_range BOOLEAN NOT NULL,
    uses_region_selection BOOLEAN NOT NULL,
    search_expansion_strategy VARCHAR(32) NOT NULL,
    blocked_if_in_party BOOLEAN NOT NULL,
    allowed_states_csv VARCHAR(255) NOT NULL,
    blocked_states_csv VARCHAR(255) NOT NULL,
    affects_global_rating BOOLEAN NOT NULL,
    affects_visible_rank BOOLEAN NOT NULL,
    affects_season_stats BOOLEAN NOT NULL,
    affects_leaderboards BOOLEAN NOT NULL,
    target_runtime VARCHAR(16) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_practice_queues_mode FOREIGN KEY (mode_id) REFERENCES practice_modes (mode_id),
    KEY idx_practice_queues_mode_id (mode_id),
    KEY idx_practice_queues_enabled_visible (enabled, visible_in_menu)
);

INSERT INTO practice_modes (mode_id, display_name, enabled, created_at, updated_at)
VALUES
    ('sword', 'Sword', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('axe', 'Axe', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('builduhc', 'BuildUHC', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('crystalpvp', 'CrystalPvP', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('pot', 'Pot', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('smp', 'SMP', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON DUPLICATE KEY UPDATE
    display_name = VALUES(display_name),
    enabled = VALUES(enabled),
    updated_at = VALUES(updated_at);

INSERT INTO practice_queues (
    queue_id, mode_id, display_name, queue_type, player_type, matchmaking_profile,
    enabled, visible_in_menu, rated, uses_skill_rating, uses_ping_range, uses_region_selection, search_expansion_strategy,
    blocked_if_in_party, allowed_states_csv, blocked_states_csv,
    affects_global_rating, affects_visible_rank, affects_season_stats, affects_leaderboards,
    target_runtime, created_at, updated_at
)
VALUES
    ('ranked-sword-1v1', 'sword', 'Ranked Sword 1v1', 'RANKED', 'ONE_VS_ONE', 'QUALITY_FIRST', TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, 'RANKED_STANDARD', TRUE, 'HUB', 'IN_MATCH,IN_QUEUE,TRANSFERRING,EDITING_LAYOUT,IN_PUBLIC_FFA,IN_PARTY_FFA,IN_EVENT,SPECTATING', TRUE, TRUE, TRUE, TRUE, 'MATCH', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('ranked-axe-1v1', 'axe', 'Ranked Axe 1v1', 'RANKED', 'ONE_VS_ONE', 'QUALITY_FIRST', TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, 'RANKED_STANDARD', TRUE, 'HUB', 'IN_MATCH,IN_QUEUE,TRANSFERRING,EDITING_LAYOUT,IN_PUBLIC_FFA,IN_PARTY_FFA,IN_EVENT,SPECTATING', TRUE, TRUE, TRUE, TRUE, 'MATCH', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('ranked-builduhc-1v1', 'builduhc', 'Ranked BuildUHC 1v1', 'RANKED', 'ONE_VS_ONE', 'QUALITY_FIRST', TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, 'RANKED_STANDARD', TRUE, 'HUB', 'IN_MATCH,IN_QUEUE,TRANSFERRING,EDITING_LAYOUT,IN_PUBLIC_FFA,IN_PARTY_FFA,IN_EVENT,SPECTATING', TRUE, TRUE, TRUE, TRUE, 'MATCH', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('ranked-crystalpvp-1v1', 'crystalpvp', 'Ranked CrystalPvP 1v1', 'RANKED', 'ONE_VS_ONE', 'QUALITY_FIRST', TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, 'RANKED_STANDARD', TRUE, 'HUB', 'IN_MATCH,IN_QUEUE,TRANSFERRING,EDITING_LAYOUT,IN_PUBLIC_FFA,IN_PARTY_FFA,IN_EVENT,SPECTATING', TRUE, TRUE, TRUE, TRUE, 'MATCH', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('ranked-pot-1v1', 'pot', 'Ranked Pot 1v1', 'RANKED', 'ONE_VS_ONE', 'QUALITY_FIRST', TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, 'RANKED_STANDARD', TRUE, 'HUB', 'IN_MATCH,IN_QUEUE,TRANSFERRING,EDITING_LAYOUT,IN_PUBLIC_FFA,IN_PARTY_FFA,IN_EVENT,SPECTATING', TRUE, TRUE, TRUE, TRUE, 'MATCH', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('ranked-smp-1v1', 'smp', 'Ranked SMP 1v1', 'RANKED', 'ONE_VS_ONE', 'QUALITY_FIRST', TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, 'RANKED_STANDARD', TRUE, 'HUB', 'IN_MATCH,IN_QUEUE,TRANSFERRING,EDITING_LAYOUT,IN_PUBLIC_FFA,IN_PARTY_FFA,IN_EVENT,SPECTATING', TRUE, TRUE, TRUE, TRUE, 'MATCH', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('unranked-sword-1v1', 'sword', 'Unranked Sword 1v1', 'UNRANKED', 'ONE_VS_ONE', 'SPEED_FIRST', TRUE, TRUE, FALSE, TRUE, TRUE, TRUE, 'UNRANKED_STANDARD', FALSE, 'HUB', 'IN_MATCH,IN_QUEUE,TRANSFERRING,EDITING_LAYOUT,IN_PUBLIC_FFA,IN_PARTY_FFA,IN_EVENT,SPECTATING', FALSE, FALSE, FALSE, FALSE, 'MATCH', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('unranked-axe-1v1', 'axe', 'Unranked Axe 1v1', 'UNRANKED', 'ONE_VS_ONE', 'SPEED_FIRST', TRUE, TRUE, FALSE, TRUE, TRUE, TRUE, 'UNRANKED_STANDARD', FALSE, 'HUB', 'IN_MATCH,IN_QUEUE,TRANSFERRING,EDITING_LAYOUT,IN_PUBLIC_FFA,IN_PARTY_FFA,IN_EVENT,SPECTATING', FALSE, FALSE, FALSE, FALSE, 'MATCH', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('unranked-builduhc-1v1', 'builduhc', 'Unranked BuildUHC 1v1', 'UNRANKED', 'ONE_VS_ONE', 'SPEED_FIRST', TRUE, TRUE, FALSE, TRUE, TRUE, TRUE, 'UNRANKED_STANDARD', FALSE, 'HUB', 'IN_MATCH,IN_QUEUE,TRANSFERRING,EDITING_LAYOUT,IN_PUBLIC_FFA,IN_PARTY_FFA,IN_EVENT,SPECTATING', FALSE, FALSE, FALSE, FALSE, 'MATCH', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('unranked-crystalpvp-1v1', 'crystalpvp', 'Unranked CrystalPvP 1v1', 'UNRANKED', 'ONE_VS_ONE', 'SPEED_FIRST', TRUE, TRUE, FALSE, TRUE, TRUE, TRUE, 'UNRANKED_STANDARD', FALSE, 'HUB', 'IN_MATCH,IN_QUEUE,TRANSFERRING,EDITING_LAYOUT,IN_PUBLIC_FFA,IN_PARTY_FFA,IN_EVENT,SPECTATING', FALSE, FALSE, FALSE, FALSE, 'MATCH', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('unranked-pot-1v1', 'pot', 'Unranked Pot 1v1', 'UNRANKED', 'ONE_VS_ONE', 'SPEED_FIRST', TRUE, TRUE, FALSE, TRUE, TRUE, TRUE, 'UNRANKED_STANDARD', FALSE, 'HUB', 'IN_MATCH,IN_QUEUE,TRANSFERRING,EDITING_LAYOUT,IN_PUBLIC_FFA,IN_PARTY_FFA,IN_EVENT,SPECTATING', FALSE, FALSE, FALSE, FALSE, 'MATCH', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('unranked-smp-1v1', 'smp', 'Unranked SMP 1v1', 'UNRANKED', 'ONE_VS_ONE', 'SPEED_FIRST', TRUE, TRUE, FALSE, TRUE, TRUE, TRUE, 'UNRANKED_STANDARD', FALSE, 'HUB', 'IN_MATCH,IN_QUEUE,TRANSFERRING,EDITING_LAYOUT,IN_PUBLIC_FFA,IN_PARTY_FFA,IN_EVENT,SPECTATING', FALSE, FALSE, FALSE, FALSE, 'MATCH', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON DUPLICATE KEY UPDATE
    mode_id = VALUES(mode_id),
    display_name = VALUES(display_name),
    queue_type = VALUES(queue_type),
    player_type = VALUES(player_type),
    matchmaking_profile = VALUES(matchmaking_profile),
    enabled = VALUES(enabled),
    visible_in_menu = VALUES(visible_in_menu),
    rated = VALUES(rated),
    uses_skill_rating = VALUES(uses_skill_rating),
    uses_ping_range = VALUES(uses_ping_range),
    uses_region_selection = VALUES(uses_region_selection),
    search_expansion_strategy = VALUES(search_expansion_strategy),
    blocked_if_in_party = VALUES(blocked_if_in_party),
    allowed_states_csv = VALUES(allowed_states_csv),
    blocked_states_csv = VALUES(blocked_states_csv),
    affects_global_rating = VALUES(affects_global_rating),
    affects_visible_rank = VALUES(affects_visible_rank),
    affects_season_stats = VALUES(affects_season_stats),
    affects_leaderboards = VALUES(affects_leaderboards),
    target_runtime = VALUES(target_runtime),
    updated_at = VALUES(updated_at);
