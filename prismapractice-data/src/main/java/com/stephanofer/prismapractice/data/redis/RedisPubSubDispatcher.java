package com.stephanofer.prismapractice.data.redis;

import io.lettuce.core.RedisFuture;
import io.lettuce.core.pubsub.RedisPubSubListener;
import io.lettuce.core.pubsub.api.async.RedisPubSubAsyncCommands;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

final class RedisPubSubDispatcher implements RedisPubSubListener<String, String> {

    private final Consumer<String> logger;
    private final RedisPubSubAsyncCommands<String, String> commands;
    private final Map<String, CopyOnWriteArrayList<Consumer<RedisChannelMessage>>> handlers;

    RedisPubSubDispatcher(Consumer<String> logger, RedisPubSubAsyncCommands<String, String> commands) {
        this.logger = Objects.requireNonNull(logger, "logger");
        this.commands = Objects.requireNonNull(commands, "commands");
        this.handlers = new ConcurrentHashMap<>();
    }

    CompletableFuture<Void> subscribe(String channel, Consumer<RedisChannelMessage> handler) {
        Objects.requireNonNull(channel, "channel");
        Objects.requireNonNull(handler, "handler");

        CopyOnWriteArrayList<Consumer<RedisChannelMessage>> channelHandlers = handlers.computeIfAbsent(channel, ignored -> new CopyOnWriteArrayList<>());
        boolean firstHandler = channelHandlers.isEmpty();
        channelHandlers.add(handler);
        if (!firstHandler) {
            return CompletableFuture.completedFuture(null);
        }
        return toVoid(commands.subscribe(channel));
    }

    CompletableFuture<Void> unsubscribe(String channel, Consumer<RedisChannelMessage> handler) {
        Objects.requireNonNull(channel, "channel");
        Objects.requireNonNull(handler, "handler");

        CopyOnWriteArrayList<Consumer<RedisChannelMessage>> channelHandlers = handlers.get(channel);
        if (channelHandlers == null) {
            return CompletableFuture.completedFuture(null);
        }

        channelHandlers.remove(handler);
        if (!channelHandlers.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }

        handlers.remove(channel, channelHandlers);
        return toVoid(commands.unsubscribe(channel));
    }

    int registeredChannelHandlers() {
        return handlers.values().stream().mapToInt(List::size).sum();
    }

    @Override
    public void message(String channel, String message) {
        RedisChannelMessage payload = new RedisChannelMessage(channel, message);
        for (Consumer<RedisChannelMessage> handler : handlers.getOrDefault(channel, new CopyOnWriteArrayList<>())) {
            try {
                handler.accept(payload);
            } catch (RuntimeException exception) {
                logger.accept("[redis-pubsub] handler failure on channel='" + channel + "': " + exception.getMessage());
            }
        }
    }

    @Override
    public void message(String pattern, String channel, String message) {
        message(channel, message);
    }

    @Override
    public void subscribed(String channel, long count) {
    }

    @Override
    public void psubscribed(String pattern, long count) {
    }

    @Override
    public void unsubscribed(String channel, long count) {
    }

    @Override
    public void punsubscribed(String pattern, long count) {
    }

    private CompletableFuture<Void> toVoid(RedisFuture<?> future) {
        return future.toCompletableFuture().thenApply(ignored -> null);
    }
}
