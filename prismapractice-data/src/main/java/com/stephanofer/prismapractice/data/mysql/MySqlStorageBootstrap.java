package com.stephanofer.prismapractice.data.mysql;

import com.zaxxer.hikari.HikariDataSource;
import com.stephanofer.prismapractice.config.ConfigBootstrapResult;
import com.stephanofer.prismapractice.config.ConfigManager;
import com.stephanofer.prismapractice.config.ConfigPlatforms;

import java.nio.file.Path;
import java.util.Objects;
import java.util.function.Consumer;

public final class MySqlStorageBootstrap {

    private final MySqlPoolFactory poolFactory;
    private final MySqlConnectionVerifier connectionVerifier;
    private final FlywayMigrationService migrationService;

    public MySqlStorageBootstrap() {
        this(new MySqlPoolFactory(), new MySqlConnectionVerifier(), new FlywayMigrationService());
    }

    MySqlStorageBootstrap(MySqlPoolFactory poolFactory, MySqlConnectionVerifier connectionVerifier, FlywayMigrationService migrationService) {
        this.poolFactory = Objects.requireNonNull(poolFactory, "poolFactory");
        this.connectionVerifier = Objects.requireNonNull(connectionVerifier, "connectionVerifier");
        this.migrationService = Objects.requireNonNull(migrationService, "migrationService");
    }

    public static StorageRuntime bootstrap(Path dataDirectory, ClassLoader classLoader, Consumer<String> logger, String runtimeName) {
        return new MySqlStorageBootstrap().bootstrapRuntime(dataDirectory, classLoader, logger, runtimeName);
    }

    public StorageRuntime bootstrapRuntime(Path dataDirectory, ClassLoader classLoader, Consumer<String> logger, String runtimeName) {
        Objects.requireNonNull(dataDirectory, "dataDirectory");
        Objects.requireNonNull(classLoader, "classLoader");
        Objects.requireNonNull(logger, "logger");
        Objects.requireNonNull(runtimeName, "runtimeName");

        ConfigManager configManager = new ConfigManager(
                ConfigPlatforms.fromClassLoader(dataDirectory, classLoader, logger::accept),
                java.util.List.of(StorageConfigDescriptorFactory.storageDescriptor())
        );

        ConfigBootstrapResult bootstrapResult = configManager.loadAll();
        logger.accept("[storage-config] runtime=" + runtimeName + ", created=" + bootstrapResult.createdFiles()
                + ", updated=" + bootstrapResult.updatedFiles() + ", migrated=" + bootstrapResult.migratedFiles()
                + ", recovered=" + bootstrapResult.recoveredFiles() + ", warnings=" + bootstrapResult.warnings());

        MySqlStorageConfig config = configManager.get("storage", MySqlStorageConfig.class);
        HikariDataSource dataSource = null;
        try {
            dataSource = poolFactory.create(config, "prismapractice-" + runtimeName + "-mysql");
            connectionVerifier.verify(dataSource, config.startup().testQuery());
            FlywayMigrationSummary migrationSummary = migrationService.migrate(classLoader, dataSource, config, "prismapractice-" + runtimeName);
            logger.accept("[storage] runtime=" + runtimeName + ", jdbc-url=" + config.safeJdbcUrl()
                    + ", pool-max=" + config.pool().maximumPoolSize() + ", migrations-executed=" + migrationSummary.migrationsExecuted()
                    + ", target-version=" + migrationSummary.targetVersion() + ", migration-warnings=" + migrationSummary.warnings());
            return new StorageRuntime(configManager, new MySqlStorage(runtimeName, config, dataSource, migrationSummary));
        } catch (RuntimeException exception) {
            closeQuietly(dataSource);
            throw new StorageBootstrapException("Failed to bootstrap MySQL storage for runtime '" + runtimeName + "'", exception);
        }
    }

    private void closeQuietly(HikariDataSource dataSource) {
        if (dataSource == null) {
            return;
        }
        try {
            dataSource.close();
        } catch (RuntimeException ignored) {
        }
    }
}
