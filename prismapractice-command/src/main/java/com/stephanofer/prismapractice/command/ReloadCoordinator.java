package com.stephanofer.prismapractice.command;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.jspecify.annotations.Nullable;

/**
 * Centraliza reload operativo del runtime.
 *
 * <p>IMPORTANTE: este coordinador es para contenido/configuración hot-reloadable. No debe usarse para
 * re-registrar comandos, listeners, pools o wiring estructural del plugin. Si un cambio reemplaza
 * bootstrap o identidad de servicios ya capturados por listeners, corresponde restart completo.</p>
 */
public final class ReloadCoordinator {

    public static final String ALL_SCOPE = "all";

    private final Map<String, RegisteredParticipant> participants;

    public ReloadCoordinator() {
        this.participants = new LinkedHashMap<>();
    }

    public ReloadCoordinator register(String scope, String description, ReloadParticipant participant) {
        return register(scope, description, List.of(), participant);
    }

    public ReloadCoordinator register(String scope, String description, List<String> dependencies, ReloadParticipant participant) {
        String normalizedScope = normalizeScope(scope);
        if (ALL_SCOPE.equals(normalizedScope)) {
            throw new IllegalArgumentException("'all' is reserved for aggregate reload execution");
        }
        if (participants.containsKey(normalizedScope)) {
            throw new IllegalArgumentException("Duplicate reload scope: " + normalizedScope);
        }
        participants.put(normalizedScope, new RegisteredParticipant(
                normalizedScope,
                requireText(description, "description"),
                normalizeDependencies(normalizedScope, dependencies),
                Objects.requireNonNull(participant, "participant")
        ));
        return this;
    }

    public ReloadReport reload(@Nullable String requestedScope) {
        String normalizedScope = requestedScope == null || requestedScope.isBlank() ? ALL_SCOPE : normalizeScope(requestedScope);
        List<RegisteredParticipant> targets = resolveTargets(normalizedScope);
        long startedAt = System.nanoTime();
        List<ReloadReport.Entry> entries = new ArrayList<>(targets.size());

        for (RegisteredParticipant target : targets) {
            long entryStartedAt = System.nanoTime();
            try {
                ReloadResult result = Objects.requireNonNull(target.participant().reload(), "reloadResult");
                entries.add(new ReloadReport.Entry(target.scope(), target.description(), elapsedMillis(entryStartedAt), true, result.message()));
            } catch (RuntimeException exception) {
                String failureMessage = exception.getMessage() == null || exception.getMessage().isBlank()
                        ? exception.getClass().getSimpleName()
                        : exception.getMessage();
                entries.add(new ReloadReport.Entry(target.scope(), target.description(), elapsedMillis(entryStartedAt), false, failureMessage));
                return new ReloadReport(
                        normalizedScope,
                        targets.stream().map(RegisteredParticipant::scope).toList(),
                        entries,
                        elapsedMillis(startedAt),
                        false,
                        target.scope(),
                        failureMessage
                );
            }
        }

        return new ReloadReport(
                normalizedScope,
                targets.stream().map(RegisteredParticipant::scope).toList(),
                entries,
                elapsedMillis(startedAt),
                true,
                null,
                null
        );
    }

    public List<String> scopes() {
        List<String> values = new ArrayList<>(participants.size() + 1);
        values.add(ALL_SCOPE);
        values.addAll(participants.keySet());
        return List.copyOf(values);
    }

    private List<RegisteredParticipant> resolveTargets(String requestedScope) {
        if (ALL_SCOPE.equals(requestedScope)) {
            return List.copyOf(participants.values());
        }

        RegisteredParticipant participant = participants.get(requestedScope);
        if (participant == null) {
            throw new IllegalArgumentException("Unknown reload scope '" + requestedScope + "'. Available: " + String.join(", ", scopes()));
        }

        List<RegisteredParticipant> resolved = new ArrayList<>();
        resolveParticipant(participant.scope(), resolved, new HashSet<>(), new HashSet<>());
        return List.copyOf(resolved);
    }

    private void resolveParticipant(String scope, List<RegisteredParticipant> resolved, Set<String> visiting, Set<String> visited) {
        if (!visiting.add(scope)) {
            throw new IllegalStateException("Reload dependency cycle detected at scope '" + scope + "'");
        }

        RegisteredParticipant participant = participants.get(scope);
        if (participant == null) {
            throw new IllegalStateException("Reload scope '" + scope + "' depends on unknown scope");
        }

        for (String dependency : participant.dependencies()) {
            resolveParticipant(dependency, resolved, visiting, visited);
        }

        visiting.remove(scope);
        if (visited.add(scope)) {
            resolved.add(participant);
        }
    }

    private static String normalizeScope(String scope) {
        return requireText(scope, "scope").toLowerCase(Locale.ROOT);
    }

    private static String requireText(String value, String field) {
        Objects.requireNonNull(value, field);
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException(field + " cannot be blank");
        }
        return trimmed;
    }

    private static List<String> normalizeDependencies(String scope, List<String> dependencies) {
        Objects.requireNonNull(dependencies, "dependencies");
        List<String> normalized = new ArrayList<>(dependencies.size());
        Set<String> seen = new HashSet<>();
        for (String dependency : dependencies) {
            String normalizedDependency = normalizeScope(dependency);
            if (ALL_SCOPE.equals(normalizedDependency)) {
                throw new IllegalArgumentException("'all' cannot be used as a reload dependency");
            }
            if (scope.equals(normalizedDependency)) {
                throw new IllegalArgumentException("Reload scope '" + scope + "' cannot depend on itself");
            }
            if (seen.add(normalizedDependency)) {
                normalized.add(normalizedDependency);
            }
        }
        return List.copyOf(normalized);
    }

    private static long elapsedMillis(long startedAt) {
        return (System.nanoTime() - startedAt) / 1_000_000L;
    }

    private record RegisteredParticipant(String scope, String description, List<String> dependencies, ReloadParticipant participant) {
    }
}
