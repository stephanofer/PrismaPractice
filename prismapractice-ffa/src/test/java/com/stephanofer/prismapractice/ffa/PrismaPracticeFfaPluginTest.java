package com.stephanofer.prismapractice.ffa;

import com.stephanofer.prismapractice.config.ConfigManager;
import com.stephanofer.prismapractice.data.mysql.MySqlStorage;
import com.stephanofer.prismapractice.data.mysql.MySqlStorageBootstrap;
import com.stephanofer.prismapractice.data.mysql.StorageRuntime;
import com.stephanofer.prismapractice.data.redis.RedisStorage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

class PrismaPracticeFfaPluginTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldDelegateToSharedStorageBootstrap() {
        MySqlStorageBootstrap bootstrap = Mockito.mock(MySqlStorageBootstrap.class);
        StorageRuntime runtime = new StorageRuntime(Mockito.mock(ConfigManager.class), Mockito.mock(MySqlStorage.class), Mockito.mock(RedisStorage.class));
        when(bootstrap.bootstrapRuntime(any(), any(), any(), anyString())).thenReturn(runtime);

        StorageRuntime result = FfaStorageBootstrap.bootstrap(tempDir, getClass().getClassLoader(), message -> {
        }, bootstrap);

        assertNotNull(result.storage());
    }
}
