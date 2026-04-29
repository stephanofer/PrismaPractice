package com.stephanofer.prismapractice.data.mysql;

import com.stephanofer.prismapractice.config.ConfigManager;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MySqlStorageBootstrapTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldCreateStorageConfigAndRunBootstrapInCorrectOrder() throws Exception {
        HikariDataSource dataSource = Mockito.mock(HikariDataSource.class);
        MySqlPoolFactory poolFactory = Mockito.mock(MySqlPoolFactory.class);
        MySqlConnectionVerifier verifier = Mockito.mock(MySqlConnectionVerifier.class);
        FlywayMigrationService migrationService = Mockito.mock(FlywayMigrationService.class);
        when(poolFactory.create(any(), anyString())).thenReturn(dataSource);
        when(migrationService.migrate(any(), any(), any(), anyString())).thenReturn(new FlywayMigrationSummary(1, "1", List.of()));

        List<String> logs = new ArrayList<>();
        MySqlStorageBootstrap bootstrap = new MySqlStorageBootstrap(poolFactory, verifier, migrationService);

        StorageRuntime runtime = bootstrap.bootstrapRuntime(tempDir, getClass().getClassLoader(), logs::add, "hub");

        assertTrue(Files.exists(tempDir.resolve("storage.yml")));
        ConfigManager configManager = runtime.configManager();
        MySqlStorageConfig config = configManager.get("storage", MySqlStorageConfig.class);
        assertEquals("jdbc:mysql://127.0.0.1:3306/prismapractice?sslMode=PREFERRED&allowPublicKeyRetrieval=false&serverTimezone=UTC&characterEncoding=utf8&useUnicode=true", config.jdbcUrl());
        assertSame(dataSource, runtime.storage().dataSource());
        assertTrue(logs.stream().anyMatch(log -> log.contains("[storage-config] runtime=hub")));
        assertTrue(logs.stream().anyMatch(log -> log.contains("migrations-executed=1")));

        org.mockito.InOrder inOrder = inOrder(poolFactory, verifier, migrationService);
        inOrder.verify(poolFactory).create(any(), anyString());
        inOrder.verify(verifier).verify(any(), anyString());
        inOrder.verify(migrationService).migrate(any(), any(), any(), anyString());
    }

    @Test
    void shouldCloseDataSourceWhenMigrationFails() {
        HikariDataSource dataSource = Mockito.mock(HikariDataSource.class);
        MySqlPoolFactory poolFactory = Mockito.mock(MySqlPoolFactory.class);
        MySqlConnectionVerifier verifier = Mockito.mock(MySqlConnectionVerifier.class);
        FlywayMigrationService migrationService = Mockito.mock(FlywayMigrationService.class);
        when(poolFactory.create(any(), anyString())).thenReturn(dataSource);
        doThrow(new IllegalStateException("boom")).when(migrationService).migrate(any(), any(), any(), anyString());

        MySqlStorageBootstrap bootstrap = new MySqlStorageBootstrap(poolFactory, verifier, migrationService);

        assertThrows(StorageBootstrapException.class,
                () -> bootstrap.bootstrapRuntime(tempDir, getClass().getClassLoader(), message -> {
                }, "match"));

        verify(dataSource, times(1)).close();
    }
}
