package dev.notifyflow.course.notifyflow.infrastructure.reconciliation;

import dev.notifyflow.course.notifyflow.application.ReconciliationCompletion;
import dev.notifyflow.course.notifyflow.application.ReconciliationStore;
import dev.notifyflow.course.notifyflow.application.ReconciliationWork;
import dev.notifyflow.course.notifyflow.domain.AttemptStatus;
import dev.notifyflow.course.notifyflow.domain.DeliveryAttempt;
import dev.notifyflow.course.notifyflow.domain.NotificationTask;
import dev.notifyflow.course.notifyflow.domain.ReconciliationCase;
import dev.notifyflow.course.notifyflow.domain.ReconciliationStatus;
import dev.notifyflow.course.notifyflow.domain.TaskStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** JDBC adapter that claims one due case and atomically persists its factual result. */
@Repository
public class JdbcReconciliationStore implements ReconciliationStore {

    private static final String SELECT_DUE = """
            SELECT case_id, version
              FROM reconciliation_case
             WHERE status = 'OPEN'
               AND next_query_at <= ?
             ORDER BY next_query_at, case_id
             LIMIT 16
            """;

    private static final String CLAIM = """
            UPDATE reconciliation_case
               SET status = 'QUERYING',
                   query_count = query_count + 1,
                   version = version + 1,
                   updated_at = ?
             WHERE case_id = ?
               AND status = 'OPEN'
               AND version = ?
               AND next_query_at <= ?
            """;

    private static final String UPDATE_CASE = """
            UPDATE reconciliation_case
               SET status = ?,
                   query_count = ?,
                   next_query_at = ?,
                   last_provider_status = ?,
                   version = ?,
                   updated_at = ?,
                   resolved_at = ?
             WHERE case_id = ?
               AND status = ?
               AND version = ?
            """;

    private static final String UPDATE_TASK = """
            UPDATE notification_task
               SET status = ?,
                   current_attempt_no = ?,
                   last_error_category = ?,
                   last_error_code = ?,
                   version = ?,
                   updated_at = ?
             WHERE id = ?
               AND status = ?
               AND version = ?
            """;

    private static final String UPDATE_ATTEMPT = """
            UPDATE delivery_attempt
               SET provider_request_id = ?,
                   status = ?,
                   error_category = ?,
                   error_code = ?,
                   version = ?,
                   finished_at = ?
             WHERE attempt_id = ?
               AND status = ?
               AND version = ?
            """;

    private static final String CASE_COLUMNS = """
            SELECT case_id, task_id, attempt_id, status, query_count,
                   next_query_at, deadline_at, last_provider_status, version,
                   created_at, updated_at, resolved_at
              FROM reconciliation_case
            """;

    private static final String TASK_COLUMNS = """
            SELECT id, tenant_id, request_id, request_fingerprint, channel,
                   recipient_ref, template_code, variables_json, status,
                   current_attempt_no, last_error_category, last_error_code,
                   version, created_at, updated_at
              FROM notification_task
            """;

    private static final String ATTEMPT_COLUMNS = """
            SELECT attempt_id, task_id, attempt_no, provider_code, idempotency_key,
                   provider_request_id, status, deadline_at, error_category,
                   error_code, version, started_at, finished_at
              FROM delivery_attempt
            """;

    private static final RowMapper<ReconciliationCase> CASE_ROW_MAPPER = (resultSet, rowNumber) ->
            new ReconciliationCase(
                    resultSet.getString("case_id"),
                    resultSet.getLong("task_id"),
                    resultSet.getString("attempt_id"),
                    ReconciliationStatus.valueOf(resultSet.getString("status")),
                    resultSet.getInt("query_count"),
                    toInstant(resultSet.getTimestamp("next_query_at")),
                    toInstant(resultSet.getTimestamp("deadline_at")),
                    resultSet.getString("last_provider_status"),
                    resultSet.getLong("version"),
                    toInstant(resultSet.getTimestamp("created_at")),
                    toInstant(resultSet.getTimestamp("updated_at")),
                    toNullableInstant(resultSet.getTimestamp("resolved_at"))
            );

    private static final RowMapper<NotificationTask> TASK_ROW_MAPPER = (resultSet, rowNumber) ->
            new NotificationTask(
                    resultSet.getLong("id"),
                    resultSet.getString("tenant_id"),
                    resultSet.getString("request_id"),
                    resultSet.getString("request_fingerprint"),
                    resultSet.getString("channel"),
                    resultSet.getString("recipient_ref"),
                    resultSet.getString("template_code"),
                    resultSet.getString("variables_json"),
                    TaskStatus.valueOf(resultSet.getString("status")),
                    resultSet.getInt("current_attempt_no"),
                    resultSet.getString("last_error_category"),
                    resultSet.getString("last_error_code"),
                    resultSet.getLong("version"),
                    toInstant(resultSet.getTimestamp("created_at")),
                    toInstant(resultSet.getTimestamp("updated_at"))
            );

    private static final RowMapper<DeliveryAttempt> ATTEMPT_ROW_MAPPER = (resultSet, rowNumber) ->
            new DeliveryAttempt(
                    resultSet.getString("attempt_id"),
                    resultSet.getLong("task_id"),
                    resultSet.getInt("attempt_no"),
                    resultSet.getString("provider_code"),
                    resultSet.getString("idempotency_key"),
                    resultSet.getString("provider_request_id"),
                    AttemptStatus.valueOf(resultSet.getString("status")),
                    toInstant(resultSet.getTimestamp("deadline_at")),
                    resultSet.getString("error_category"),
                    resultSet.getString("error_code"),
                    resultSet.getLong("version"),
                    toInstant(resultSet.getTimestamp("started_at")),
                    toNullableInstant(resultSet.getTimestamp("finished_at"))
            );

    private final JdbcTemplate jdbcTemplate;

    public JdbcReconciliationStore(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate, "jdbcTemplate");
    }

    @Override
    @Transactional
    public Optional<ReconciliationWork> claimDue(Instant now) {
        Objects.requireNonNull(now, "now");
        List<Candidate> candidates = jdbcTemplate.query(
                SELECT_DUE,
                (resultSet, rowNumber) -> new Candidate(
                        resultSet.getString("case_id"),
                        resultSet.getLong("version")
                ),
                Timestamp.from(now)
        );
        for (Candidate candidate : candidates) {
            int claimed = jdbcTemplate.update(
                    CLAIM,
                    Timestamp.from(now),
                    candidate.caseId(),
                    candidate.version(),
                    Timestamp.from(now)
            );
            if (claimed == 1) {
                ReconciliationCase reconciliationCase = findCase(candidate.caseId(), false);
                NotificationTask task = findTask(reconciliationCase.taskId(), false);
                DeliveryAttempt attempt = findAttempt(reconciliationCase.attemptId(), false);
                return Optional.of(new ReconciliationWork(reconciliationCase, task, attempt));
            }
        }
        return Optional.empty();
    }

    @Override
    @Transactional
    public boolean complete(ReconciliationWork work, ReconciliationCompletion completion) {
        validateCompletion(work, completion);

        // Use one lock order everywhere: task -> attempt -> case. All expected
        // versions are checked before the first mutation, so stale facts cannot
        // produce a partially committed convergence.
        NotificationTask lockedTask = findTask(work.task().id(), true);
        if (!matches(work.task(), lockedTask)) {
            return false;
        }
        DeliveryAttempt lockedAttempt = findAttempt(work.attempt().attemptId(), true);
        if (!matches(work.attempt(), lockedAttempt)) {
            return false;
        }
        ReconciliationCase lockedCase = findCase(work.reconciliationCase().caseId(), true);
        if (!matches(work.reconciliationCase(), lockedCase)) {
            return false;
        }

        updateTaskIfChanged(work.task(), completion.task());
        updateAttemptIfChanged(work.attempt(), completion.attempt());
        updateCase(work.reconciliationCase(), completion.reconciliationCase());
        return true;
    }

    private void updateTaskIfChanged(NotificationTask expected, NotificationTask updated) {
        if (expected.equals(updated)) {
            return;
        }
        if (expected.status() == updated.status() || updated.version() != expected.version() + 1) {
            throw new IllegalArgumentException("Changed reconciliation task must transition and increment version");
        }
        int affected = jdbcTemplate.update(
                UPDATE_TASK,
                updated.status().name(),
                updated.currentAttemptNo(),
                updated.lastErrorCategory(),
                updated.lastErrorCode(),
                updated.version(),
                Timestamp.from(updated.updatedAt()),
                expected.id(),
                expected.status().name(),
                expected.version()
        );
        requireOne(affected, "notification_task");
    }

    private void updateAttemptIfChanged(DeliveryAttempt expected, DeliveryAttempt updated) {
        if (expected.equals(updated)) {
            return;
        }
        if (expected.status() == updated.status() || updated.version() != expected.version() + 1) {
            throw new IllegalArgumentException("Changed reconciliation attempt must transition and increment version");
        }
        int affected = jdbcTemplate.update(
                UPDATE_ATTEMPT,
                updated.providerRequestId(),
                updated.status().name(),
                updated.errorCategory(),
                updated.errorCode(),
                updated.version(),
                toNullableTimestamp(updated.finishedAt()),
                expected.attemptId(),
                expected.status().name(),
                expected.version()
        );
        requireOne(affected, "delivery_attempt");
    }

    private void updateCase(ReconciliationCase expected, ReconciliationCase updated) {
        int affected = jdbcTemplate.update(
                UPDATE_CASE,
                updated.status().name(),
                updated.queryCount(),
                Timestamp.from(updated.nextQueryAt()),
                updated.lastProviderStatus(),
                updated.version(),
                Timestamp.from(updated.updatedAt()),
                toNullableTimestamp(updated.resolvedAt()),
                expected.caseId(),
                expected.status().name(),
                expected.version()
        );
        requireOne(affected, "reconciliation_case");
    }

    private NotificationTask findTask(long taskId, boolean forUpdate) {
        String suffix = forUpdate ? " WHERE id = ? FOR UPDATE" : " WHERE id = ?";
        return jdbcTemplate.query(TASK_COLUMNS + suffix, TASK_ROW_MAPPER, taskId)
                .stream().findFirst()
                .orElseThrow(() -> new IllegalStateException("Notification task is missing: " + taskId));
    }

    private DeliveryAttempt findAttempt(String attemptId, boolean forUpdate) {
        String suffix = forUpdate ? " WHERE attempt_id = ? FOR UPDATE" : " WHERE attempt_id = ?";
        return jdbcTemplate.query(ATTEMPT_COLUMNS + suffix, ATTEMPT_ROW_MAPPER, attemptId)
                .stream().findFirst()
                .orElseThrow(() -> new IllegalStateException("Delivery attempt is missing: " + attemptId));
    }

    private ReconciliationCase findCase(String caseId, boolean forUpdate) {
        String suffix = forUpdate ? " WHERE case_id = ? FOR UPDATE" : " WHERE case_id = ?";
        return jdbcTemplate.query(CASE_COLUMNS + suffix, CASE_ROW_MAPPER, caseId)
                .stream().findFirst()
                .orElseThrow(() -> new IllegalStateException("Reconciliation case is missing: " + caseId));
    }

    private static void validateCompletion(ReconciliationWork work, ReconciliationCompletion completion) {
        Objects.requireNonNull(work, "work");
        Objects.requireNonNull(completion, "completion");
        ReconciliationCase expectedCase = work.reconciliationCase();
        NotificationTask expectedTask = work.task();
        DeliveryAttempt expectedAttempt = work.attempt();
        ReconciliationCase reconciliationCase = completion.reconciliationCase();
        NotificationTask task = completion.task();
        DeliveryAttempt attempt = completion.attempt();

        if (expectedCase.status() != ReconciliationStatus.QUERYING
                || expectedTask.status() != TaskStatus.UNKNOWN
                || expectedAttempt.status() != AttemptStatus.UNKNOWN) {
            throw new IllegalArgumentException("Reconciliation work must contain QUERYING/UNKNOWN facts");
        }
        if (!expectedCase.caseId().equals(reconciliationCase.caseId())
                || expectedTask.id() != task.id()
                || !expectedAttempt.attemptId().equals(attempt.attemptId())) {
            throw new IllegalArgumentException("Reconciliation completion identity does not match work");
        }
        if (reconciliationCase.version() != expectedCase.version() + 1) {
            throw new IllegalArgumentException("Reconciliation case version must increment exactly once");
        }

        switch (reconciliationCase.status()) {
            case OPEN -> requireUnchangedUnknown(expectedTask, task, expectedAttempt, attempt);
            case RESOLVED -> requireResolved(expectedTask, task, expectedAttempt, attempt);
            case MANUAL_REVIEW -> requireManualReview(expectedTask, task, expectedAttempt, attempt);
            case QUERYING -> throw new IllegalArgumentException("Completion cannot leave case QUERYING");
        }
    }

    private static void requireUnchangedUnknown(
            NotificationTask expectedTask,
            NotificationTask task,
            DeliveryAttempt expectedAttempt,
            DeliveryAttempt attempt
    ) {
        if (task.status() != TaskStatus.UNKNOWN || attempt.status() != AttemptStatus.UNKNOWN
                || task.version() != expectedTask.version()
                || attempt.version() != expectedAttempt.version()) {
            throw new IllegalArgumentException("Reopened case must keep task and attempt UNKNOWN");
        }
    }

    private static void requireResolved(
            NotificationTask expectedTask,
            NotificationTask task,
            DeliveryAttempt expectedAttempt,
            DeliveryAttempt attempt
    ) {
        boolean succeeded = task.status() == TaskStatus.SUCCEEDED
                && attempt.status() == AttemptStatus.SUCCEEDED;
        boolean rejected = task.status() == TaskStatus.FAILED
                && attempt.status() == AttemptStatus.PERMANENT_FAILED;
        if (!(succeeded || rejected)
                || task.version() != expectedTask.version() + 1
                || attempt.version() != expectedAttempt.version() + 1) {
            throw new IllegalArgumentException("Resolved case requires matching terminal task and attempt");
        }
    }

    private static void requireManualReview(
            NotificationTask expectedTask,
            NotificationTask task,
            DeliveryAttempt expectedAttempt,
            DeliveryAttempt attempt
    ) {
        if (task.status() != TaskStatus.MANUAL_REVIEW
                || task.version() != expectedTask.version() + 1
                || attempt.status() != AttemptStatus.UNKNOWN
                || attempt.version() != expectedAttempt.version()) {
            throw new IllegalArgumentException("Manual review keeps attempt UNKNOWN and closes the task");
        }
    }

    private static boolean matches(NotificationTask expected, NotificationTask actual) {
        return expected.id() == actual.id()
                && expected.status() == actual.status()
                && expected.version() == actual.version();
    }

    private static boolean matches(DeliveryAttempt expected, DeliveryAttempt actual) {
        return expected.attemptId().equals(actual.attemptId())
                && expected.status() == actual.status()
                && expected.version() == actual.version();
    }

    private static boolean matches(ReconciliationCase expected, ReconciliationCase actual) {
        return expected.caseId().equals(actual.caseId())
                && expected.status() == actual.status()
                && expected.version() == actual.version();
    }

    private static void requireOne(int affected, String table) {
        if (affected != 1) {
            throw new IllegalStateException("Locked " + table + " row changed unexpectedly");
        }
    }

    private static Instant toInstant(Timestamp timestamp) {
        return timestamp.toInstant();
    }

    private static Instant toNullableInstant(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toInstant();
    }

    private static Timestamp toNullableTimestamp(Instant instant) {
        return instant == null ? null : Timestamp.from(instant);
    }

    private record Candidate(String caseId, long version) {
    }
}
