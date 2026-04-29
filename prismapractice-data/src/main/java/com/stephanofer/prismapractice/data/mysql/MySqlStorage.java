package com.stephanofer.prismapractice.data.mysql;

import com.stephanofer.prismapractice.data.JdbcExecutor;
import com.stephanofer.prismapractice.data.TransactionRunner;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;

import java.util.Objects;

public final class MySqlStorage implements AutoCloseable {

    private final String runtimeName;
    private final MySqlStorageConfig config;
    private final HikariDataSource dataSource;
    private final JdbcExecutor jdbcExecutor;
    private final TransactionRunner transactionRunner;
    private final FlywayMigrationSummary migrationSummary;
    private volatile boolean closed;

    public MySqlStorage(String runtimeName, MySqlStorageConfig config, HikariDataSource dataSource, FlywayMigrationSummary migrationSummary) {
        this.runtimeName = Objects.requireNonNull(runtimeName, "runtimeName");
        this.config = Objects.requireNonNull(config, "config");
        this.dataSource = Objects.requireNonNull(dataSource, "dataSource");
        this.migrationSummary = Objects.requireNonNull(migrationSummary, "migrationSummary");
        this.jdbcExecutor = new JdbcExecutor(dataSource);
        this.transactionRunner = new TransactionRunner(dataSource);
    }

    public String runtimeName() {
        return runtimeName;
    }

    public MySqlStorageConfig config() {
        return config;
    }

    public HikariDataSource dataSource() {
        return dataSource;
    }

    public JdbcExecutor jdbcExecutor() {
        return jdbcExecutor;
    }

    public TransactionRunner transactionRunner() {
        return transactionRunner;
    }

    public FlywayMigrationSummary migrationSummary() {
        return migrationSummary;
    }

    public StorageHealthSnapshot healthSnapshot() {
        HikariPoolMXBean pool = dataSource.getHikariPoolMXBean();
        if (pool == null) {
            return new StorageHealthSnapshot(!closed, 0, 0, 0, 0);
        }

        return new StorageHealthSnapshot(
                !closed,
                pool.getActiveConnections(),
                pool.getIdleConnections(),
                pool.getTotalConnections(),
                pool.getThreadsAwaitingConnection()
        );
    }

    public boolean isClosed() {
        return closed;
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        dataSource.close();
    }
}
