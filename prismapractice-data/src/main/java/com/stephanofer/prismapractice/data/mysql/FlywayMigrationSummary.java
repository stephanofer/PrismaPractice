package com.stephanofer.prismapractice.data.mysql;

import java.util.List;

public record FlywayMigrationSummary(int migrationsExecuted, String targetVersion, List<String> warnings) {
}
