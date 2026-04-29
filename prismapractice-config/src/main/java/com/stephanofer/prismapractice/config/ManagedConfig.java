package com.stephanofer.prismapractice.config;

import java.nio.file.Path;
import java.util.Map;

public record ManagedConfig<T>(ConfigDescriptor<T> descriptor, Path file, Map<String, Object> rawRoot, T value) {
}
