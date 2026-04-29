package com.stephanofer.prismapractice.config;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;

public interface ConfigPlatform {

    Path dataDirectory();

    Optional<String> readBundledResource(String path) throws IOException;

    ConfigLogger logger();
}
