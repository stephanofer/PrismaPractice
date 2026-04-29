package com.stephanofer.prismapractice.config;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public final class YamlConfigHelper {

    private YamlConfigHelper() {
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> section(Map<String, Object> root, String key) {
        Object value = root.get(key);
        if (value == null) {
            return new LinkedHashMap<>();
        }
        if (!(value instanceof Map<?, ?> map)) {
            throw new ConfigException("Expected section '" + key + "' to be a map");
        }

        return (Map<String, Object>) map;
    }

    public static String string(Map<String, Object> root, String key) {
        Object value = root.get(key);
        if (!(value instanceof String stringValue)) {
            throw new ConfigException("Expected string at key '" + key + "'");
        }
        return stringValue;
    }

    public static int integer(Map<String, Object> root, String key) {
        Object value = root.get(key);
        if (value instanceof Number number) {
            return number.intValue();
        }
        throw new ConfigException("Expected integer at key '" + key + "'");
    }

    public static boolean bool(Map<String, Object> root, String key) {
        Object value = root.get(key);
        if (value instanceof Boolean booleanValue) {
            return booleanValue;
        }
        throw new ConfigException("Expected boolean at key '" + key + "'");
    }

    @SuppressWarnings("unchecked")
    public static void move(Map<String, Object> root, String fromPath, String toPath) {
        Objects.requireNonNull(root, "root");
        Object value = remove(root, fromPath);
        if (value != null && get(root, toPath) == null) {
            set(root, toPath, value);
        }
    }

    @SuppressWarnings("unchecked")
    public static Object get(Map<String, Object> root, String path) {
        String[] parts = path.split("\\.");
        Object current = root;
        for (String part : parts) {
            if (!(current instanceof Map<?, ?> map)) {
                return null;
            }
            current = ((Map<String, Object>) map).get(part);
            if (current == null) {
                return null;
            }
        }
        return current;
    }

    @SuppressWarnings("unchecked")
    public static void set(Map<String, Object> root, String path, Object value) {
        String[] parts = path.split("\\.");
        Map<String, Object> current = root;
        for (int index = 0; index < parts.length - 1; index++) {
            Object existing = current.get(parts[index]);
            if (!(existing instanceof Map<?, ?> map)) {
                LinkedHashMap<String, Object> created = new LinkedHashMap<>();
                current.put(parts[index], created);
                current = created;
                continue;
            }
            current = (Map<String, Object>) map;
        }
        current.put(parts[parts.length - 1], value);
    }

    @SuppressWarnings("unchecked")
    public static Object remove(Map<String, Object> root, String path) {
        String[] parts = path.split("\\.");
        Map<String, Object> current = root;
        for (int index = 0; index < parts.length - 1; index++) {
            Object existing = current.get(parts[index]);
            if (!(existing instanceof Map<?, ?> map)) {
                return null;
            }
            current = (Map<String, Object>) map;
        }
        return current.remove(parts[parts.length - 1]);
    }
}
