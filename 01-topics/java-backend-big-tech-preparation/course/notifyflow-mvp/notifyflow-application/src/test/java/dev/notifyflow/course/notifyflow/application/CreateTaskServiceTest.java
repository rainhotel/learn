package dev.notifyflow.course.notifyflow.application;

import dev.notifyflow.course.notifyflow.domain.NotificationTask;
import dev.notifyflow.course.notifyflow.domain.TaskStatus;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CreateTaskServiceTest {
    private static final Instant NOW = Instant.parse("2026-07-15T10:00:00Z");

    @Test
    void normalizesRequestAndBuildsStableSha256Fingerprint() {
        CapturingStore store = new CapturingStore();
        CreateTaskService service = new CreateTaskService(store, () -> NOW, () -> "event-1");
        Map<String, String> variables = new LinkedHashMap<>();
        variables.put(" z ", " value ");
        variables.put("a", "first");
        CreateTaskCommand command = new CreateTaskCommand(
                " tenant ", " request-1 ", "course_stub", " recipient ", " template ", variables);

        CreateTaskResult result = service.create(command);

        CreateTaskDraft draft = store.drafts.getFirst();
        assertEquals("tenant", draft.tenantId());
        assertEquals("request-1", draft.requestId());
        assertEquals("COURSE_STUB", draft.channel());
        assertEquals("{\"a\":\"first\",\"z\":\"value\"}", draft.variablesJson());
        assertEquals(64, draft.requestFingerprint().length());
        assertEquals(draft.requestFingerprint(), result.requestFingerprint());
        assertEquals(NOW, draft.createdAt());
    }

    @Test
    void sameNormalizedPayloadHasSameFingerprintRegardlessOfMapOrder() {
        CapturingStore store = new CapturingStore();
        CreateTaskService service = new CreateTaskService(store, () -> NOW, new SequenceIdGenerator());
        Map<String, String> first = new LinkedHashMap<>();
        first.put("b", "two");
        first.put("a", "one");
        Map<String, String> second = new LinkedHashMap<>();
        second.put("a", "one");
        second.put("b", "two");

        service.create(command(first));
        service.create(command(second));

        assertEquals(store.drafts.get(0).requestFingerprint(), store.drafts.get(1).requestFingerprint());
    }

    @Test
    void returnsStoreReplayAndConflictWithoutInventingOutcome() {
        NotificationTask existing = task();
        CapturingStore store = new CapturingStore();
        store.outcome = new TaskCreationStore.CreationOutcome(TaskCreationStore.OutcomeStatus.REPLAYED, existing);
        CreateTaskService service = new CreateTaskService(store, () -> NOW, () -> "event-replay");

        CreateTaskResult replay = service.create(command(Map.of("name", "fixture")));

        assertEquals(TaskCreationStatus.REPLAYED, replay.status());
        assertEquals(existing, replay.task());
        assertEquals(1, store.drafts.size());
    }

    @Test
    void rejectsBlankInputAndBlankGeneratedId() {
        CreateTaskService service = new CreateTaskService(draft -> new TaskCreationStore.CreationOutcome(
                TaskCreationStore.OutcomeStatus.CREATED, task()), () -> NOW, () -> "event");
        assertThrows(IllegalArgumentException.class,
                () -> service.create(new CreateTaskCommand(" ", "request", "channel",
                        "recipient", "template", Map.of("name", "fixture"))));

        CreateTaskService badIdService = new CreateTaskService(draft -> new TaskCreationStore.CreationOutcome(
                TaskCreationStore.OutcomeStatus.CREATED, task()), () -> NOW, () -> " ");
        assertThrows(IllegalStateException.class,
                () -> badIdService.create(new CreateTaskCommand("tenant", "request", "channel",
                        "recipient", "template", Map.of("name", "fixture"))));
    }

    private static CreateTaskCommand command(Map<String, String> variables) {
        return new CreateTaskCommand("tenant", "request", "COURSE_STUB", "recipient",
                "WELCOME_V1", variables);
    }

    private static NotificationTask task() {
        return new NotificationTask(1L, "tenant", "request", "a".repeat(64), "COURSE_STUB",
                "recipient", "WELCOME_V1", "{\"name\":\"fixture\"}", TaskStatus.ACCEPTED,
                0, null, null, 0L, NOW, NOW);
    }

    private static final class CapturingStore implements TaskCreationStore {
        private final List<CreateTaskDraft> drafts = new ArrayList<>();
        private CreationOutcome outcome = new CreationOutcome(OutcomeStatus.CREATED, task());

        @Override
        public CreationOutcome create(CreateTaskDraft draft) {
            drafts.add(draft);
            return outcome;
        }
    }

    private static final class SequenceIdGenerator implements IdGenerator {
        private int count;

        @Override
        public String nextId() {
            return "event-" + (++count);
        }
    }
}
