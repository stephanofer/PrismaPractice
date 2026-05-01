package com.stephanofer.prismapractice.match;

import com.stephanofer.prismapractice.config.ConfigManager;
import com.stephanofer.prismapractice.data.mysql.MySqlStorage;
import com.stephanofer.prismapractice.data.mysql.MySqlStorageBootstrap;
import com.stephanofer.prismapractice.data.mysql.StorageRuntime;
import com.stephanofer.prismapractice.data.redis.RedisStorage;
import com.stephanofer.prismapractice.debug.DebugController;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

class PrismaPracticeMatchPluginTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldDelegateToSharedStorageBootstrap() {
        MySqlStorageBootstrap bootstrap = Mockito.mock(MySqlStorageBootstrap.class);
        StorageRuntime runtime = new StorageRuntime(Mockito.mock(ConfigManager.class), Mockito.mock(MySqlStorage.class), Mockito.mock(RedisStorage.class), DebugController.noop());
        when(bootstrap.bootstrapRuntime(any(), any(), any(), anyString(), any())).thenReturn(runtime);

        StorageRuntime result = MatchStorageBootstrap.bootstrap(tempDir, getClass().getClassLoader(), message -> {
        }, bootstrap);

        assertNotNull(result.storage());
    }
}
