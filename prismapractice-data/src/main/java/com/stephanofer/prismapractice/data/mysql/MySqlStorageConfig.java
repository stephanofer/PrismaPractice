package com.stephanofer.prismapractice.data.mysql;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.Collections;

public record MySqlStorageConfig(
        String host,
        int port,
        String database,
        String username,
        String password,
        Map<String, String> parameters,
        MySqlPoolConfig pool,
        MySqlMigrationConfig migrations,
        MySqlStartupConfig startup
) {

    public MySqlStorageConfig {
        Objects.requireNonNull(host, "host");
        Objects.requireNonNull(database, "database");
        Objects.requireNonNull(username, "username");
        Objects.requireNonNull(password, "password");
        parameters = Collections.unmodifiableMap(new LinkedHashMap<>(Objects.requireNonNull(parameters, "parameters")));
        Objects.requireNonNull(pool, "pool");
        Objects.requireNonNull(migrations, "migrations");
        Objects.requireNonNull(startup, "startup");
    }

    public String jdbcUrl() {
        StringBuilder builder = new StringBuilder("jdbc:mysql://")
                .append(host)
                .append(':')
                .append(port)
                .append('/')
                .append(database);

        if (!parameters.isEmpty()) {
            StringJoiner joiner = new StringJoiner("&");
            for (Map.Entry<String, String> entry : parameters.entrySet()) {
                joiner.add(entry.getKey() + "=" + entry.getValue());
            }
            builder.append('?').append(joiner);
        }

        return builder.toString();
    }

    public String safeJdbcUrl() {
        return "jdbc:mysql://" + host + ':' + port + '/' + database;
    }

    public record MySqlPoolConfig(
            int maximumPoolSize,
            int minimumIdle,
            long connectionTimeoutMs,
            long validationTimeoutMs,
            long idleTimeoutMs,
            long maxLifetimeMs,
            long keepaliveTimeMs,
            long leakDetectionThresholdMs
    ) {
    }

    public record MySqlMigrationConfig(
            List<String> locations,
            String table,
            boolean baselineOnMigrate,
            boolean validateOnMigrate,
            boolean cleanDisabled,
            boolean failOnMissingLocations
    ) {
        public MySqlMigrationConfig {
            locations = List.copyOf(Objects.requireNonNull(locations, "locations"));
            Objects.requireNonNull(table, "table");
        }
    }

    public record MySqlStartupConfig(String testQuery) {
        public MySqlStartupConfig {
            Objects.requireNonNull(testQuery, "testQuery");
        }
    }
}
