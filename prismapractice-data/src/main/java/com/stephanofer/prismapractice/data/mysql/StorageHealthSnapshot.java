package com.stephanofer.prismapractice.data.mysql;

public record StorageHealthSnapshot(
        boolean available,
        int activeConnections,
        int idleConnections,
        int totalConnections,
        int threadsAwaitingConnection
) {
}
