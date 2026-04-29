package com.stephanofer.prismapractice.config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ConfigBootstrapResult {

    private final List<String> createdFiles = new ArrayList<>();
    private final List<String> updatedFiles = new ArrayList<>();
    private final List<String> migratedFiles = new ArrayList<>();
    private final List<String> recoveredFiles = new ArrayList<>();
    private final List<String> warnings = new ArrayList<>();

    void created(String path) {
        createdFiles.add(path);
    }

    void updated(String path) {
        updatedFiles.add(path);
    }

    void migrated(String path) {
        migratedFiles.add(path);
    }

    void recovered(String path) {
        recoveredFiles.add(path);
    }

    void warning(String warning) {
        warnings.add(warning);
    }

    public List<String> createdFiles() {
        return Collections.unmodifiableList(createdFiles);
    }

    public List<String> updatedFiles() {
        return Collections.unmodifiableList(updatedFiles);
    }

    public List<String> migratedFiles() {
        return Collections.unmodifiableList(migratedFiles);
    }

    public List<String> recoveredFiles() {
        return Collections.unmodifiableList(recoveredFiles);
    }

    public List<String> warnings() {
        return Collections.unmodifiableList(warnings);
    }
}
