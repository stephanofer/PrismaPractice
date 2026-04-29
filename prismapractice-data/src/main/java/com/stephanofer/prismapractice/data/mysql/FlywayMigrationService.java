package com.stephanofer.prismapractice.data.mysql;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.output.MigrateResult;

import javax.sql.DataSource;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public final class FlywayMigrationService {

    public FlywayMigrationSummary migrate(ClassLoader classLoader, DataSource dataSource, MySqlStorageConfig config, String installedBy) {
        Objects.requireNonNull(classLoader, "classLoader");
        Objects.requireNonNull(dataSource, "dataSource");
        Objects.requireNonNull(config, "config");

        Flyway flyway = Flyway.configure(classLoader)
                .dataSource(dataSource)
                .locations(config.migrations().locations().toArray(String[]::new))
                .table(config.migrations().table())
                .baselineOnMigrate(config.migrations().baselineOnMigrate())
                .validateOnMigrate(config.migrations().validateOnMigrate())
                .validateMigrationNaming(true)
                .cleanDisabled(config.migrations().cleanDisabled())
                .failOnMissingLocations(config.migrations().failOnMissingLocations())
                .callbacks(new String[0])
                .skipDefaultCallbacks(true)
                .installedBy(installedBy)
                .load();

        MigrateResult result = flyway.migrate();
        String targetVersion = result.targetSchemaVersion == null ? "none" : result.targetSchemaVersion.toString();
        List<String> warnings = result.warnings == null ? Collections.emptyList() : List.copyOf(result.warnings);
        return new FlywayMigrationSummary(result.migrationsExecuted, targetVersion, warnings);
    }
}
