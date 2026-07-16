package dev.notifyflow.course.notifyflow.infrastructure.jdbc;

import dev.notifyflow.course.notifyflow.application.CreateTaskDraft;
import dev.notifyflow.course.notifyflow.application.TaskCreationStore;
import dev.notifyflow.course.notifyflow.application.TaskQueryStore;
import dev.notifyflow.course.notifyflow.domain.NotificationTask;
import dev.notifyflow.course.notifyflow.domain.TaskStatus;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

@Repository
public class JdbcTaskStore implements TaskCreationStore, TaskQueryStore {

    private static final String INSERT_TASK = """
            INSERT INTO notification_task (
                tenant_id,
                request_id,
                request_fingerprint,
                channel,
                recipient_ref,
                template_code,
                variables_json,
                status,
                current_attempt_no,
                version,
                created_at,
                updated_at
            ) VALUES (?, ?, ?, ?, ?, ?, CAST(? AS JSON), ?, 0, 0, ?, ?)
            """;

    private static final String INSERT_OUTBOX = """
            INSERT INTO event_outbox (
                event_id,
                aggregate_type,
                aggregate_id,
                event_type,
                event_version,
                partition_key,
                payload,
                status,
                attempt_count,
                next_attempt_at,
                version,
                created_at,
                updated_at
            ) VALUES (?, 'NotificationTask', ?, 'TaskAccepted', 1, ?, CAST(? AS JSON), 'PENDING', 0, ?, 0, ?, ?)
            """;

    private static final String SELECT_TASK_COLUMNS = """
            SELECT id,
                   tenant_id,
                   request_id,
                   request_fingerprint,
                   channel,
                   recipient_ref,
                   template_code,
                   variables_json,
                   status,
                   current_attempt_no,
                   last_error_category,
                   last_error_code,
                   version,
                   created_at,
                   updated_at
              FROM notification_task
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

    private final JdbcTemplate jdbcTemplate;

    public JdbcTaskStore(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate, "jdbcTemplate");
    }

    @Override
    @Transactional
    public CreationOutcome create(CreateTaskDraft draft) {
        Objects.requireNonNull(draft, "draft");

        final long taskId;
        try {
            taskId = insertTask(draft);
        } catch (DuplicateKeyException duplicateRequest) {
            return classifyDuplicate(draft, duplicateRequest);
        }

        insertOutbox(draft, taskId);
        NotificationTask created = findRequired(taskId);
        return new CreationOutcome(OutcomeStatus.CREATED, created);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<NotificationTask> findById(long taskId) {
        return jdbcTemplate.query(
                        SELECT_TASK_COLUMNS + " WHERE id = ?",
                        TASK_ROW_MAPPER,
                        taskId
                )
                .stream()
                .findFirst();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<NotificationTask> findByTenantAndRequestId(String tenantId, String requestId) {
        return jdbcTemplate.query(
                        SELECT_TASK_COLUMNS + " WHERE tenant_id = ? AND request_id = ?",
                        TASK_ROW_MAPPER,
                        tenantId,
                        requestId
                )
                .stream()
                .findFirst();
    }

    private long insertTask(CreateTaskDraft draft) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        int inserted = jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(INSERT_TASK, Statement.RETURN_GENERATED_KEYS);
            statement.setString(1, draft.tenantId());
            statement.setString(2, draft.requestId());
            statement.setString(3, draft.requestFingerprint());
            statement.setString(4, draft.channel());
            statement.setString(5, draft.recipientRef());
            statement.setString(6, draft.templateCode());
            bindJson(statement, 7, draft.variablesJson());
            statement.setString(8, TaskStatus.ACCEPTED.name());
            statement.setTimestamp(9, Timestamp.from(draft.createdAt()));
            statement.setTimestamp(10, Timestamp.from(draft.createdAt()));
            return statement;
        }, keyHolder);

        if (inserted != 1) {
            throw new IllegalStateException("Expected one notification_task row, inserted=" + inserted);
        }
        Number key = keyHolder.getKey();
        if (key == null) {
            throw new IllegalStateException("Database did not return notification_task generated key");
        }
        return key.longValue();
    }

    private void insertOutbox(CreateTaskDraft draft, long taskId) {
        String aggregateId = Long.toString(taskId);
        String payload = taskAcceptedPayload(draft, taskId);
        Timestamp occurredAt = Timestamp.from(draft.createdAt());

        int inserted = jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(INSERT_OUTBOX);
            statement.setString(1, draft.eventId());
            statement.setString(2, aggregateId);
            statement.setString(3, aggregateId);
            bindJson(statement, 4, payload);
            statement.setTimestamp(5, occurredAt);
            statement.setTimestamp(6, occurredAt);
            statement.setTimestamp(7, occurredAt);
            return statement;
        });
        if (inserted != 1) {
            throw new IllegalStateException("Expected one event_outbox row, inserted=" + inserted);
        }
    }

    private CreationOutcome classifyDuplicate(CreateTaskDraft draft, DuplicateKeyException originalFailure) {
        NotificationTask existing = findByTenantAndRequestId(draft.tenantId(), draft.requestId())
                .orElseThrow(() -> new IllegalStateException(
                        "Unique request key failed but the existing notification task was not visible",
                        originalFailure
                ));

        OutcomeStatus status = fingerprintsEqual(existing.requestFingerprint(), draft.requestFingerprint())
                ? OutcomeStatus.REPLAYED
                : OutcomeStatus.CONFLICT;
        return new CreationOutcome(status, existing);
    }

    private NotificationTask findRequired(long taskId) {
        return findById(taskId)
                .orElseThrow(() -> new IllegalStateException("Created notification task is missing: " + taskId));
    }

    private static boolean fingerprintsEqual(String left, String right) {
        return java.security.MessageDigest.isEqual(
                left.getBytes(StandardCharsets.US_ASCII),
                right.getBytes(StandardCharsets.US_ASCII)
        );
    }

    private static void bindJson(PreparedStatement statement, int parameterIndex, String json) throws SQLException {
        // H2 converts a bound String to a JSON string literal instead of parsing the
        // document. MySQL correctly parses a String parameter. This is the only
        // dialect-specific binding needed by the shared schema and is covered by the
        // H2 integration test; MySQL remains a separate runtime verification gate.
        String database = statement.getConnection().getMetaData().getDatabaseProductName();
        if ("H2".equalsIgnoreCase(database)) {
            statement.setBytes(parameterIndex, json.getBytes(StandardCharsets.UTF_8));
        } else {
            statement.setString(parameterIndex, json);
        }
    }

    private static String taskAcceptedPayload(CreateTaskDraft draft, long taskId) {
        return "{" +
                "\"eventId\":" + quoteJson(draft.eventId()) + "," +
                "\"eventType\":\"TaskAccepted\"," +
                "\"eventVersion\":1," +
                "\"occurredAt\":" + quoteJson(draft.createdAt().toString()) + "," +
                "\"taskId\":" + taskId + "," +
                "\"tenantId\":" + quoteJson(draft.tenantId()) + "," +
                "\"channel\":" + quoteJson(draft.channel()) +
                "}";
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

    private static Instant toInstant(Timestamp timestamp) {
        return timestamp.toInstant();
    }
}
