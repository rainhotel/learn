import dev.notifyflow.course.notifyflow.application.Clock;
import dev.notifyflow.course.notifyflow.application.CreateTaskCommand;
import dev.notifyflow.course.notifyflow.application.CreateTaskDraft;
import dev.notifyflow.course.notifyflow.application.CreateTaskResult;
import dev.notifyflow.course.notifyflow.application.CreateTaskService;
import dev.notifyflow.course.notifyflow.application.IdGenerator;
import dev.notifyflow.course.notifyflow.application.TaskCreationStatus;
import dev.notifyflow.course.notifyflow.application.TaskCreationStore;
import dev.notifyflow.course.notifyflow.domain.NotificationTask;
import dev.notifyflow.course.notifyflow.domain.TaskStatus;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

public final class CoreContractCheck {
    private static final Instant NOW = Instant.parse("2026-07-15T10:00:00Z");

    public static void main(String[] args) {
        stateMachineRejectsIllegalTransitions();
        canonicalFingerprintIgnoresMapOrder();
        storeOutcomeIsPreserved();
        System.out.println("NOTIFYFLOW_CORE_CONTRACT_CHECK_PASSED");
    }

    private static void stateMachineRejectsIllegalTransitions() {
        NotificationTask accepted = task(TaskStatus.ACCEPTED);
        NotificationTask sending = accepted.startAttempt(NOW.plusSeconds(1));
        require(sending.status() == TaskStatus.SENDING, "ACCEPTED must transition to SENDING");
        require(sending.currentAttemptNo() == 1, "starting an attempt increments attempt number");

        boolean rejected = false;
        try {
            accepted.transitionTo(TaskStatus.SUCCEEDED, NOW.plusSeconds(2));
        } catch (IllegalStateException expected) {
            rejected = true;
        }
        require(rejected, "ACCEPTED -> SUCCEEDED must be rejected");
    }

    private static void canonicalFingerprintIgnoresMapOrder() {
        CapturingStore store = new CapturingStore();
        CreateTaskService service = service(store);

        Map<String, String> first = new LinkedHashMap<>();
        first.put("b", "two");
        first.put("a", "one");
        Map<String, String> second = new LinkedHashMap<>();
        second.put("a", "one");
        second.put("b", "two");

        service.create(command(first));
        String firstFingerprint = store.lastDraft.requestFingerprint();
        service.create(command(second));
        String secondFingerprint = store.lastDraft.requestFingerprint();

        require(firstFingerprint.equals(secondFingerprint), "map order must not change fingerprint");
        require(firstFingerprint.length() == 64, "fingerprint must be SHA-256 hex");
    }

    private static void storeOutcomeIsPreserved() {
        CapturingStore store = new CapturingStore();
        CreateTaskService service = service(store);

        store.next = TaskCreationStore.OutcomeStatus.REPLAYED;
        CreateTaskResult replayed = service.create(command(Map.of("name", "fixture")));
        require(replayed.status() == TaskCreationStatus.REPLAYED, "replay status must be preserved");

        store.next = TaskCreationStore.OutcomeStatus.CONFLICT;
        CreateTaskResult conflict = service.create(command(Map.of("name", "different")));
        require(conflict.status() == TaskCreationStatus.CONFLICT, "conflict status must be preserved");
    }

    private static CreateTaskService service(CapturingStore store) {
        Clock clock = () -> NOW;
        IdGenerator ids = () -> "event-1";
        return new CreateTaskService(store, clock, ids);
    }

    private static CreateTaskCommand command(Map<String, String> variables) {
        return new CreateTaskCommand("tenant", "request", "course_stub", "recipient", "WELCOME", variables);
    }

    private static NotificationTask task(TaskStatus status) {
        return new NotificationTask(1, "tenant", "request", "a".repeat(64), "COURSE_STUB",
                "recipient", "WELCOME", "{}", status, 0, null, null, 0, NOW, NOW);
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static final class CapturingStore implements TaskCreationStore {
        private OutcomeStatus next = OutcomeStatus.CREATED;
        private CreateTaskDraft lastDraft;

        @Override
        public CreationOutcome create(CreateTaskDraft draft) {
            lastDraft = draft;
            NotificationTask task = new NotificationTask(1, draft.tenantId(), draft.requestId(),
                    draft.requestFingerprint(), draft.channel(), draft.recipientRef(), draft.templateCode(),
                    draft.variablesJson(), TaskStatus.ACCEPTED, 0, null, null, 0,
                    draft.createdAt(), draft.createdAt());
            return new CreationOutcome(next, task);
        }
    }
}
