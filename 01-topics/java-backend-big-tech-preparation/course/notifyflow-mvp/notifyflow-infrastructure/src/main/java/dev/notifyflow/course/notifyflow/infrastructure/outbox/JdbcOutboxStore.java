package dev.notifyflow.course.notifyflow.infrastructure.outbox;

import dev.notifyflow.course.notifyflow.application.OutboxStore;
import dev.notifyflow.course.notifyflow.domain.OutboxMessage;
import dev.notifyflow.course.notifyflow.domain.OutboxStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * JDBC outbox lease store.
 *
 * <p>The lease is deliberately represented by owner/until columns instead of a
 * separate status. A crashed publisher therefore leaves a row recoverable after
 * {@code lease_until}, while status remains the durable publication outcome.</p>
 */
@Repository
public class JdbcOutboxStore implements OutboxStore {

    private static final String SELECT_DUE_CANDIDATES = """
            SELECT id, version
              FROM event_outbox
             WHERE status IN ('PENDING', 'RETRY')
               AND next_attempt_at <= ?
               AND (lease_until IS NULL OR lease_until <= ?)
             ORDER BY next_attempt_at, id
             LIMIT ?
            """;

    private static final String CLAIM_CANDIDATE = """
            UPDATE event_outbox
               SET lease_owner = ?,
                   lease_until = ?,
                   version = version + 1,
                   updated_at = ?
             WHERE id = ?
               AND version = ?
               AND status IN ('PENDING', 'RETRY')
               AND next_attempt_at <= ?
               AND (lease_until IS NULL OR lease_until <= ?)
            """;

    private static final String SELECT_COLUMNS = """
            SELECT id,
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
                   lease_owner,
                   lease_until,
                   published_at,
                   last_error,
                   version,
                   created_at,
                   updated_at
              FROM event_outbox
            """;

    private static final String SAVE_OUTCOME = """
            UPDATE event_outbox
               SET status = ?,
                   attempt_count = ?,
                   next_attempt_at = ?,
                   lease_owner = ?,
                   lease_until = ?,
                   published_at = ?,
                   last_error = ?,
                   version = ?,
                   updated_at = ?
             WHERE id = ?
               AND status = ?
               AND version = ?
               AND lease_owner = ?
            """;

    private static final RowMapper<OutboxMessage> MESSAGE_ROW_MAPPER = (resultSet, rowNumber) ->
            new OutboxMessage(
                    resultSet.getLong("id"),
                    resultSet.getString("event_id"),
                    resultSet.getString("aggregate_type"),
                    resultSet.getString("aggregate_id"),
                    resultSet.getString("event_type"),
                    resultSet.getInt("event_version"),
                    resultSet.getString("partition_key"),
                    resultSet.getString("payload"),
                    OutboxStatus.valueOf(resultSet.getString("status")),
                    resultSet.getInt("attempt_count"),
                    toInstant(resultSet.getTimestamp("next_attempt_at")),
                    resultSet.getString("lease_owner"),
                    toNullableInstant(resultSet.getTimestamp("lease_until")),
                    toNullableInstant(resultSet.getTimestamp("published_at")),
                    resultSet.getString("last_error"),
                    resultSet.getLong("version"),
                    toInstant(resultSet.getTimestamp("created_at")),
                    toInstant(resultSet.getTimestamp("updated_at"))
            );

    private final JdbcTemplate jdbcTemplate;

    public JdbcOutboxStore(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate, "jdbcTemplate");
    }

    @Override
    @Transactional
    public List<OutboxMessage> claimDue(
            Instant now,
            String leaseOwner,
            Instant leaseUntil,
            int limit
    ) {
        Objects.requireNonNull(now, "now");
        requireText(leaseOwner, "leaseOwner");
        Objects.requireNonNull(leaseUntil, "leaseUntil");
        if (!leaseUntil.isAfter(now)) {
            throw new IllegalArgumentException("leaseUntil must be after now");
        }
        if (limit <= 0) {
            throw new IllegalArgumentException("limit must be positive");
        }

        List<Candidate> candidates = jdbcTemplate.query(
                SELECT_DUE_CANDIDATES,
                (resultSet, rowNumber) -> new Candidate(
                        resultSet.getLong("id"),
                        resultSet.getLong("version")
                ),
                Timestamp.from(now),
                Timestamp.from(now),
                limit
        );

        List<OutboxMessage> claimed = new ArrayList<>(candidates.size());
        for (Candidate candidate : candidates) {
            int updated = jdbcTemplate.update(
                    CLAIM_CANDIDATE,
                    leaseOwner,
                    Timestamp.from(leaseUntil),
                    Timestamp.from(now),
                    candidate.id(),
                    candidate.version(),
                    Timestamp.from(now),
                    Timestamp.from(now)
            );
            if (updated == 1) {
                claimed.add(findRequired(candidate.id()));
            }
        }
        return List.copyOf(claimed);
    }

    @Override
    @Transactional
    public boolean save(OutboxMessage expected, OutboxMessage updated) {
        validateUpdate(expected, updated);

        int affected = jdbcTemplate.update(
                SAVE_OUTCOME,
                updated.status().name(),
                updated.attemptCount(),
                Timestamp.from(updated.nextAttemptAt()),
                updated.leaseOwner(),
                toNullableTimestamp(updated.leaseUntil()),
                toNullableTimestamp(updated.publishedAt()),
                updated.lastError(),
                updated.version(),
                Timestamp.from(updated.updatedAt()),
                expected.id(),
                expected.status().name(),
                expected.version(),
                expected.leaseOwner()
        );
        return affected == 1;
    }

    private OutboxMessage findRequired(long id) {
        return jdbcTemplate.query(SELECT_COLUMNS + " WHERE id = ?", MESSAGE_ROW_MAPPER, id)
                .stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Claimed outbox row is missing: " + id));
    }

    private static void validateUpdate(OutboxMessage expected, OutboxMessage updated) {
        Objects.requireNonNull(expected, "expected");
        Objects.requireNonNull(updated, "updated");
        if (expected.id() != updated.id()) {
            throw new IllegalArgumentException("Outbox id cannot change");
        }
        if (expected.leaseOwner() == null || expected.leaseOwner().isBlank()) {
            throw new IllegalArgumentException("Expected outbox message must hold a lease");
        }
        if (updated.version() != expected.version() + 1) {
            throw new IllegalArgumentException("Updated outbox version must increment exactly once");
        }
        boolean retryAgain = expected.status() == OutboxStatus.RETRY
                && updated.status() == OutboxStatus.RETRY;
        if (!retryAgain && !expected.status().canTransitionTo(updated.status())) {
            throw new IllegalArgumentException(
                    "Illegal outbox persistence transition: " + expected.status() + " -> " + updated.status()
            );
        }
        if (updated.leaseOwner() != null || updated.leaseUntil() != null) {
            throw new IllegalArgumentException("Completed publication outcome must release its lease");
        }
        if (!sameIdentity(expected, updated)) {
            throw new IllegalArgumentException("Outbox event identity cannot change");
        }
    }

    private static boolean sameIdentity(OutboxMessage left, OutboxMessage right) {
        return left.eventId().equals(right.eventId())
                && left.aggregateType().equals(right.aggregateType())
                && left.aggregateId().equals(right.aggregateId())
                && left.eventType().equals(right.eventType())
                && left.eventVersion() == right.eventVersion()
                && left.partitionKey().equals(right.partitionKey())
                && left.payload().equals(right.payload())
                && left.createdAt().equals(right.createdAt());
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

    private record Candidate(long id, long version) {
    }
}
