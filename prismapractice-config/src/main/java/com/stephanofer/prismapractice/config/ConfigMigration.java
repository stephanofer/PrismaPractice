package com.stephanofer.prismapractice.config;

import java.util.Map;

@FunctionalInterface
public interface ConfigMigration {

    void migrate(Map<String, Object> root);
}
