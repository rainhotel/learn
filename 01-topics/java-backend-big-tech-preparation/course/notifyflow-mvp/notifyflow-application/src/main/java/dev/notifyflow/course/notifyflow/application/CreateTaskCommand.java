package dev.notifyflow.course.notifyflow.application;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/** Input boundary for the create-task use case. */
public record CreateTaskCommand(
        String tenantId,
        String requestId,
        String channel,
        String recipientRef,
        String templateCode,
        Map<String, String> variables) {

    public CreateTaskCommand {
        variables = variables == null
                ? Map.of()
                : Map.copyOf(new LinkedHashMap<>(variables));
    }
}
