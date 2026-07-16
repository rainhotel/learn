package dev.notifyflow.course.notifyflow.infrastructure.delivery;

import dev.notifyflow.course.notifyflow.application.DeliveryCompletion;
import dev.notifyflow.course.notifyflow.application.DeliveryStore;
import dev.notifyflow.course.notifyflow.application.DeliveryWork;
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
import java.util.Objects;
import java.util.Optional;

/** JDBC adapter for the consumer claim and provider result transaction boundaries. */
@Repository
public class JdbcDeliveryStore implements DeliveryStore {

    private static final String START_TASK = """
            UPDATE notification_task
               SET status = 'SENDING',
                   current_attempt_no = current_attempt_no + 1,
                   last_error_category = NULL,
                   last_error_code = NULL,
                   version = version + 1,
                   updated_at = ?
             WHERE id = ?
               AND status = 'ACCEPTED'
            """;

    private static final String INSERT_ATTEMPT = """
            INSERT INTO delivery_attempt (
                attempt_id, task_id, attempt_no, provider_code, idempotency_key,
                status, deadline_at, version, started_at
            ) VALUES (?, ?, ?, ?, ?, 'SENDING', ?, 0, ?)
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

    private static final String INSERT_RECONCILIATION = """
            INSERT INTO reconciliation_case (
                case_id, task_id, attempt_id, status, query_count,
                next_query_at, deadline_at, last_provider_status,
                version, created_at, updated_at, resolved_at
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
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

    public JdbcDeliveryStore(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate, "jdbcTemplate");
    }

    @Override
    @Transactional
    public Optional<DeliveryWork> tryStart(
            long taskId,
            String attemptId,
            String providerCode,
            String idempotencyKey,
            Instant startedAt,
            Instant deadlineAt
    ) {
        if (taskId <= 0) {
            throw new IllegalArgumentException("taskId must be positive");
        }
        requireText(attemptId, "attemptId");
        requireText(providerCode, "providerCode");
        requireText(idempotencyKey, "idempotencyKey");
        Objects.requireNonNull(startedAt, "startedAt");
        Objects.requireNonNull(deadlineAt, "deadlineAt");
        if (!deadlineAt.isAfter(startedAt)) {
            throw new IllegalArgumentException("deadlineAt must be after startedAt");
        }

        int claimed = jdbcTemplate.update(START_TASK, Timestamp.from(startedAt), taskId);
        if (claimed == 0) {
            return Optional.empty();
        }

        NotificationTask task = findTask(taskId, false);
        DeliveryAttempt attempt = new DeliveryAttempt(
                attemptId,
                taskId,
                task.currentAttemptNo(),
                providerCode,
                idempotencyKey,
                null,
                AttemptStatus.SENDING,
                deadlineAt,
                null,
                null,
                0,
                startedAt,
                null
        );
        int inserted = jdbcTemplate.update(
                INSERT_ATTEMPT,
                attempt.attemptId(),
                attempt.taskId(),
                attempt.attemptNo(),
                attempt.providerCode(),
                attempt.idempotencyKey(),
                Timestamp.from(attempt.deadlineAt()),
                Timestamp.from(attempt.startedAt())
        );
        if (inserted != 1) {
            throw new IllegalStateException("Expected one delivery_attempt row, inserted=" + inserted);
        }
        return Optional.of(new DeliveryWork(task, attempt));
    }

    @Override
    @Transactional
    public boolean complete(DeliveryWork work, DeliveryCompletion completion) {
        validateCompletion(work, completion);

        // Lock and compare both aggregate versions before changing either row. This
        // permits a boolean stale-result contract without committing a half update.
        NotificationTask lockedTask = findTask(work.task().id(), true);
        if (!matches(work.task(), lockedTask)) {
            return false;
        }
        DeliveryAttempt lockedAttempt = findAttempt(work.attempt().attemptId(), true);
        if (!matches(work.attempt(), lockedAttempt)) {
            return false;
        }

        DeliveryAttempt attempt = completion.attempt();
        NotificationTask task = completion.task();
        int attemptUpdated = jdbcTemplate.update(
                UPDATE_ATTEMPT,
                attempt.providerRequestId(),
                attempt.status().name(),
                attempt.errorCategory(),
                attempt.errorCode(),
                attempt.version(),
                toNullableTimestamp(attempt.finishedAt()),
                work.attempt().attemptId(),
                work.attempt().status().name(),
                work.attempt().version()
        );
        int taskUpdated = jdbcTemplate.update(
                UPDATE_TASK,
                task.status().name(),
                task.currentAttemptNo(),
                task.lastErrorCategory(),
                task.lastErrorCode(),
                task.version(),
                Timestamp.from(task.updatedAt()),
                work.task().id(),
                work.task().status().name(),
                work.task().version()
        );
        if (attemptUpdated != 1 || taskUpdated != 1) {
            throw new IllegalStateException("Locked delivery rows changed unexpectedly");
        }

        if (completion.reconciliationCase() != null) {
            insertReconciliation(completion.reconciliationCase());
        }
        return true;
    }

    private NotificationTask findTask(long taskId, boolean forUpdate) {
        String suffix = forUpdate ? " WHERE id = ? FOR UPDATE" : " WHERE id = ?";
        return jdbcTemplate.query(TASK_COLUMNS + suffix, TASK_ROW_MAPPER, taskId)
                .stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Notification task is missing: " + taskId));
    }

    private DeliveryAttempt findAttempt(String attemptId, boolean forUpdate) {
        String suffix = forUpdate ? " WHERE attempt_id = ? FOR UPDATE" : " WHERE attempt_id = ?";
        return jdbcTemplate.query(ATTEMPT_COLUMNS + suffix, ATTEMPT_ROW_MAPPER, attemptId)
                .stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Delivery attempt is missing: " + attemptId));
    }

    private void insertReconciliation(ReconciliationCase reconciliationCase) {
        int inserted = jdbcTemplate.update(
                INSERT_RECONCILIATION,
                reconciliationCase.caseId(),
                reconciliationCase.taskId(),
                reconciliationCase.attemptId(),
                reconciliationCase.status().name(),
                reconciliationCase.queryCount(),
                Timestamp.from(reconciliationCase.nextQueryAt()),
                Timestamp.from(reconciliationCase.deadlineAt()),
                reconciliationCase.lastProviderStatus(),
                reconciliationCase.version(),
                Timestamp.from(reconciliationCase.createdAt()),
                Timestamp.from(reconciliationCase.updatedAt()),
                toNullableTimestamp(reconciliationCase.resolvedAt())
        );
        if (inserted != 1) {
            throw new IllegalStateException("Expected one reconciliation_case row, inserted=" + inserted);
        }
    }

    private static void validateCompletion(DeliveryWork work, DeliveryCompletion completion) {
        Objects.requireNonNull(work, "work");
        Objects.requireNonNull(completion, "completion");
        NotificationTask expectedTask = work.task();
        DeliveryAttempt expectedAttempt = work.attempt();
        NotificationTask task = completion.task();
        DeliveryAttempt attempt = completion.attempt();

        if (expectedTask.status() != TaskStatus.SENDING || expectedAttempt.status() != AttemptStatus.SENDING) {
            throw new IllegalArgumentException("Delivery work must contain SENDING task and attempt");
        }
        if (task.id() != expectedTask.id() || !attempt.attemptId().equals(expectedAttempt.attemptId())) {
            throw new IllegalArgumentException("Delivery completion identity does not match work");
        }
        if (task.version() != expectedTask.version() + 1
                || attempt.version() != expectedAttempt.version() + 1) {
            throw new IllegalArgumentException("Delivery completion must increment both versions exactly once");
        }

        boolean succeeded = task.status() == TaskStatus.SUCCEEDED
                && attempt.status() == AttemptStatus.SUCCEEDED;
        boolean rejected = task.status() == TaskStatus.FAILED
                && attempt.status() == AttemptStatus.PERMANENT_FAILED;
        boolean unknown = task.status() == TaskStatus.UNKNOWN
                && attempt.status() == AttemptStatus.UNKNOWN;
        if (!(succeeded || rejected || unknown)) {
            throw new IllegalArgumentException("Task and attempt completion statuses are inconsistent");
        }

        ReconciliationCase reconciliationCase = completion.reconciliationCase();
        if (unknown) {
            if (reconciliationCase == null
                    || reconciliationCase.status() != ReconciliationStatus.OPEN
                    || reconciliationCase.taskId() != task.id()
                    || !reconciliationCase.attemptId().equals(attempt.attemptId())) {
                throw new IllegalArgumentException("UNKNOWN completion requires one matching OPEN case");
            }
        } else if (reconciliationCase != null) {
            throw new IllegalArgumentException("Terminal delivery completion must not create reconciliation");
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

    private static void requireText(String value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
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
}
