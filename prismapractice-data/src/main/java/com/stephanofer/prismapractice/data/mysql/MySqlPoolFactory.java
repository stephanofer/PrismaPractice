package com.stephanofer.prismapractice.data.mysql;

import com.mysql.cj.jdbc.Driver;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.util.Map;
import java.util.Objects;

public final class MySqlPoolFactory {

    public HikariDataSource create(MySqlStorageConfig config, String poolName) {
        Objects.requireNonNull(config, "config");
        Objects.requireNonNull(poolName, "poolName");

        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setPoolName(poolName);
        hikariConfig.setDriverClassName(Driver.class.getName());
        hikariConfig.setJdbcUrl(config.jdbcUrl());
        hikariConfig.setUsername(config.username());
        hikariConfig.setPassword(config.password());
        hikariConfig.setMaximumPoolSize(config.pool().maximumPoolSize());
        hikariConfig.setMinimumIdle(config.pool().minimumIdle());
        hikariConfig.setConnectionTimeout(config.pool().connectionTimeoutMs());
        hikariConfig.setValidationTimeout(config.pool().validationTimeoutMs());
        hikariConfig.setIdleTimeout(config.pool().idleTimeoutMs());
        hikariConfig.setMaxLifetime(config.pool().maxLifetimeMs());
        hikariConfig.setKeepaliveTime(config.pool().keepaliveTimeMs());
        if (config.pool().leakDetectionThresholdMs() > 0L) {
            hikariConfig.setLeakDetectionThreshold(config.pool().leakDetectionThresholdMs());
        }

        hikariConfig.setAutoCommit(true);
        hikariConfig.setInitializationFailTimeout(-1L);

        addDefaultDriverProperties(hikariConfig);
        for (Map.Entry<String, String> entry : config.parameters().entrySet()) {
            hikariConfig.addDataSourceProperty(entry.getKey(), entry.getValue());
        }

        return new HikariDataSource(hikariConfig);
    }

    private void addDefaultDriverProperties(HikariConfig hikariConfig) {
        hikariConfig.addDataSourceProperty("cachePrepStmts", "true");
        hikariConfig.addDataSourceProperty("prepStmtCacheSize", "250");
        hikariConfig.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        hikariConfig.addDataSourceProperty("useServerPrepStmts", "true");
        hikariConfig.addDataSourceProperty("useLocalSessionState", "true");
        hikariConfig.addDataSourceProperty("rewriteBatchedStatements", "true");
        hikariConfig.addDataSourceProperty("cacheResultSetMetadata", "true");
        hikariConfig.addDataSourceProperty("cacheServerConfiguration", "true");
        hikariConfig.addDataSourceProperty("elideSetAutoCommits", "true");
        hikariConfig.addDataSourceProperty("maintainTimeStats", "false");
        hikariConfig.addDataSourceProperty("tcpKeepAlive", "true");
    }
}
