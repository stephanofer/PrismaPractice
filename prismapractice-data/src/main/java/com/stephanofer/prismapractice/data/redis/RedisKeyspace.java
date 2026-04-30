package com.stephanofer.prismapractice.data.redis;

import java.util.Objects;

public final class RedisKeyspace {

    private final String rootPrefix;

    public RedisKeyspace(RedisStorageConfig.RedisKeyspaceConfig config) {
        Objects.requireNonNull(config, "config");

        StringBuilder builder = new StringBuilder(config.prefix());
        if (!config.namespace().isBlank()) {
            builder.append(':').append(config.namespace());
        }
        this.rootPrefix = builder.toString();
    }

    public String rootPrefix() {
        return rootPrefix;
    }

    public String playerPresence(String playerId) {
        return key("player", playerId, "presence");
    }

    public String playerState(String playerId) {
        return key("player", playerId, "state");
    }

    public String queueEntries(String queueId) {
        return key("queue", queueId, "entries");
    }

    public String playerQueue(String playerId) {
        return key("player", playerId, "queue");
    }

    public String queueSearch(String queueId, String playerId) {
        return key("queue", queueId, "search", playerId);
    }

    public String matchmakingSnapshot(String queueId, String playerId) {
        return key("queue", queueId, "snapshot", playerId);
    }

    public String activeMatch(String matchId) {
        return key("match", matchId);
    }

    public String playerActiveMatch(String playerId) {
        return key("player", playerId, "active-match");
    }

    public String arenaState(String arenaId) {
        return key("arena", arenaId, "state");
    }

    public String arenaLock(String arenaId) {
        return key("arena", arenaId, "lock");
    }

    public String party(String partyId) {
        return key("party", partyId);
    }

    public String playerParty(String playerId) {
        return key("player", playerId, "party");
    }

    public String friendRequest(String senderId, String targetId) {
        return key("social", "friend-request", senderId, targetId);
    }

    public String duelRequest(String senderId, String targetId) {
        return key("social", "duel-request", senderId, targetId);
    }

    public String partyInvite(String partyId, String targetId) {
        return key("social", "party-invite", partyId, targetId);
    }

    public String cooldown(String type, String actorId, String targetId) {
        return key("cooldown", type, actorId, targetId);
    }

    public String transitionLock(String playerId) {
        return key("lock", "transition", playerId);
    }

    public String matchmakingLock(String queueId, String playerId) {
        return key("lock", "matchmaking", queueId, playerId);
    }

    public String event(String eventId) {
        return key("event", eventId);
    }

    public String eventParticipants(String eventId) {
        return key("event", eventId, "participants");
    }

    public String counterHub() {
        return key("counter", "hub");
    }

    public String counterQueue() {
        return key("counter", "queue");
    }

    public String counterMatch() {
        return key("counter", "match");
    }

    public String counterFfa() {
        return key("counter", "ffa");
    }

    public String counterEvent() {
        return key("counter", "event");
    }

    public String leaderboardGlobal() {
        return key("leaderboard", "global");
    }

    public String leaderboardSeasonGlobal(String seasonId) {
        return key("leaderboard", "season", seasonId, "global");
    }

    public String leaderboardMode(String modeId) {
        return key("leaderboard", "mode", modeId);
    }

    public String leaderboardSeasonMode(String seasonId, String modeId) {
        return key("leaderboard", "season", seasonId, "mode", modeId);
    }

    public String leaderboardEntry(String scopeKey, String playerId) {
        return key("leaderboard", "entry", scopeKey, playerId);
    }

    private String key(String... segments) {
        StringBuilder builder = new StringBuilder(rootPrefix);
        for (String segment : segments) {
            builder.append(':').append(requireSegment(segment));
        }
        return builder.toString();
    }

    private String requireSegment(String value) {
        Objects.requireNonNull(value, "segment");
        if (value.isBlank()) {
            throw new IllegalArgumentException("Redis key segment must not be blank");
        }
        return value;
    }
}
