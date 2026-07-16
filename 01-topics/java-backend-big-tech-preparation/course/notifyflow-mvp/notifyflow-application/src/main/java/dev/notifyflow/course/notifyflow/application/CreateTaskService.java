package dev.notifyflow.course.notifyflow.application;

import dev.notifyflow.course.notifyflow.domain.NotificationTask;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.Normalizer;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Application use case for atomic, idempotent task creation. It has no Spring or
 * persistence implementation dependency; the store decides whether a unique-key
 * race is a create, replay, or payload conflict.
 */
public final class CreateTaskService {
    private final TaskCreationStore taskCreationStore;
    private final Clock clock;
    private final IdGenerator idGenerator;

    public CreateTaskService(TaskCreationStore taskCreationStore, Clock clock, IdGenerator idGenerator) {
        this.taskCreationStore = Objects.requireNonNull(taskCreationStore, "taskCreationStore");
        this.clock = Objects.requireNonNull(clock, "clock");
        this.idGenerator = Objects.requireNonNull(idGenerator, "idGenerator");
    }

    public CreateTaskResult create(CreateTaskCommand command) {
        NormalizedRequest normalized = normalize(command);
        CreateTaskDraft draft = new CreateTaskDraft(
                normalized.tenantId(),
                normalized.requestId(),
                normalized.fingerprint(),
                normalized.channel(),
                normalized.recipientRef(),
                normalized.templateCode(),
                normalized.variablesJson(),
                requiredGeneratedId(idGenerator.nextId()),
                Objects.requireNonNull(clock.now(), "clock.now()"));

        TaskCreationStore.CreationOutcome outcome = taskCreationStore.create(draft);
        TaskCreationStatus status = TaskCreationStatus.valueOf(outcome.status().name());
        return new CreateTaskResult(status, outcome.task(), normalized.fingerprint());
    }

    private static NormalizedRequest normalize(CreateTaskCommand command) {
        Objects.requireNonNull(command, "command");
        String tenantId = normalizeText(command.tenantId(), "tenantId");
        String requestId = normalizeText(command.requestId(), "requestId");
        String channel = normalizeText(command.channel(), "channel").toUpperCase(java.util.Locale.ROOT);
        String recipientRef = normalizeText(command.recipientRef(), "recipientRef");
        String templateCode = normalizeText(command.templateCode(), "templateCode");

        Map<String, String> variables = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : command.variables().entrySet()) {
            String key = normalizeText(entry.getKey(), "variable key");
            String value = normalizeText(entry.getValue(), "variable " + key);
            if (variables.put(key, value) != null) {
                throw new IllegalArgumentException("duplicate variable key after normalization: " + key);
            }
        }

        String variablesJson = canonicalObject(variables);
        String canonicalPayload = "{"
                + "\"channel\":" + quoteJson(channel) + ","
                + "\"recipientRef\":" + quoteJson(recipientRef) + ","
                + "\"templateCode\":" + quoteJson(templateCode) + ","
                + "\"tenantId\":" + quoteJson(tenantId) + ","
                + "\"variables\":" + variablesJson
                + "}";
        return new NormalizedRequest(tenantId, requestId, channel, recipientRef, templateCode,
                variablesJson, sha256(canonicalPayload));
    }

    private static String normalizeText(String value, String name) {
        Objects.requireNonNull(value, name);
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFC).trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return normalized;
    }

    private static String canonicalObject(Map<String, String> values) {
        List<Map.Entry<String, String>> entries = new ArrayList<>(values.entrySet());
        entries.sort(Comparator.comparing(Map.Entry::getKey));
        StringBuilder json = new StringBuilder("{");
        for (int index = 0; index < entries.size(); index++) {
            if (index > 0) {
                json.append(',');
            }
            Map.Entry<String, String> entry = entries.get(index);
            json.append(quoteJson(entry.getKey())).append(':').append(quoteJson(entry.getValue()));
        }
        return json.append('}').toString();
    }

    private static String quoteJson(String value) {
        StringBuilder escaped = new StringBuilder(value.length() + 2).append('"');
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            switch (character) {
                case '"' -> escaped.append("\\\"");
                case '\\' -> escaped.append("\\\\");
                case '\b' -> escaped.append("\\b");
                case '\f' -> escaped.append("\\f");
                case '\n' -> escaped.append("\\n");
                case '\r' -> escaped.append("\\r");
                case '\t' -> escaped.append("\\t");
                default -> {
                    if (character < 0x20) {
                        escaped.append(String.format("\\u%04x", (int) character));
                    } else {
                        escaped.append(character);
                    }
                }
            }
        }
        return escaped.append('"').toString();
    }

    private static String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(digest.length * 2);
            for (byte current : digest) {
                hex.append(String.format("%02x", current & 0xff));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 is unavailable", impossible);
        }
    }

    private static String requiredGeneratedId(String generated) {
        if (generated == null || generated.isBlank()) {
            throw new IllegalStateException("idGenerator returned a blank id");
        }
        return generated;
    }

    private record NormalizedRequest(
            String tenantId,
            String requestId,
            String channel,
            String recipientRef,
            String templateCode,
            String variablesJson,
            String fingerprint) {
    }
}
