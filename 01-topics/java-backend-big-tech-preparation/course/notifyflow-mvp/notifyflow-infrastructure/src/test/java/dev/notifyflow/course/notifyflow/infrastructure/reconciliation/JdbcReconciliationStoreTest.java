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
import org.flywaydb.core.Flyway;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringJUnitConfig(JdbcReconciliationStoreTest.TestConfiguration.class)
class JdbcReconciliationStoreTest {

    private static final Instant NOW = Instant.parse("2026-07-15T10:00:00Z");
    private static final long TASK_ID = 201L;
    private static final String ATTEMPT_ID = "attempt-unknown";
    private static final String CASE_ID = "case-unknown";

    private final ReconciliationStore store;
    private final JdbcTemplate jdbcTemplate;

    JdbcReconciliationStoreTest(ReconciliationStore store, JdbcTemplate jdbcTemplate) {
        this.store = store;
        this.jdbcTemplate = jdbcTemplate;
    }

    @BeforeEach
    void reset() {
        jdbcTemplate.update("DELETE FROM reconciliation_case");
        jdbcTemplate.update("DELETE FROM delivery_attempt");
        jdbcTemplate.update("DELETE FROM notification_task");
        insertUnknownTimeline();
    }

    @Test
    void claimsOneDueCaseByChangingOpenToQueryingAndIncrementingQueryCount() {
        ReconciliationWork work = store.claimDue(NOW).orElseThrow();

        assertThat(work.reconciliationCase().status()).isEqualTo(ReconciliationStatus.QUERYING);
        assertThat(work.reconciliationCase().queryCount()).isEqualTo(1);
        assertThat(work.reconciliationCase().version()).isEqualTo(1);
        assertThat(work.task().status()).isEqualTo(TaskStatus.UNKNOWN);
        assertThat(work.attempt().status()).isEqualTo(AttemptStatus.UNKNOWN);
        assertThat(store.claimDue(NOW)).isEmpty();
    }

    @Test
    void pendingFactReopensCaseWithoutChangingUnknownTaskOrAttempt() {
        ReconciliationWork work = store.claimDue(NOW).orElseThrow();
        ReconciliationCase reopened = work.reconciliationCase().reopen(
                NOW.plusSeconds(10), "PENDING", NOW.plusSeconds(1)
        );

        assertThat(store.complete(work, new ReconciliationCompletion(
                reopened, work.task(), work.attempt()
        ))).isTrue();
        assertThat(status("reconciliation_case", "case_id", CASE_ID)).isEqualTo("OPEN");
        assertThat(status("notification_task", "id", Long.toString(TASK_ID))).isEqualTo("UNKNOWN");
        assertThat(status("delivery_attempt", "attempt_id", ATTEMPT_ID)).isEqualTo("UNKNOWN");
        assertThat(store.claimDue(NOW.plusSeconds(9))).isEmpty();
        assertThat(store.claimDue(NOW.plusSeconds(10))).isPresent();
    }

    @Test
    void succeededProviderFactAtomicallyResolvesAllThreeRowsAndRejectsStaleWrite() {
        ReconciliationWork work = store.claimDue(NOW).orElseThrow();
        ReconciliationCase resolved = work.reconciliationCase().resolve("SUCCEEDED", NOW.plusSeconds(1));
        NotificationTask task = work.task().transitionTo(TaskStatus.SUCCEEDED, NOW.plusSeconds(1));
        DeliveryAttempt attempt = work.attempt().transitionTo(
                AttemptStatus.SUCCEEDED, NOW.plusSeconds(1), "provider-201", null, null
        );
        ReconciliationCompletion completion = new ReconciliationCompletion(resolved, task, attempt);

        assertThat(store.complete(work, completion)).isTrue();
        assertThat(store.complete(work, completion)).isFalse();
        assertThat(status("reconciliation_case", "case_id", CASE_ID)).isEqualTo("RESOLVED");
        assertThat(status("notification_task", "id", Long.toString(TASK_ID))).isEqualTo("SUCCEEDED");
        assertThat(status("delivery_attempt", "attempt_id", ATTEMPT_ID)).isEqualTo("SUCCEEDED");
    }

    @Test
    void expiredUncertainFactMovesTaskAndCaseToManualReviewButKeepsAttemptUnknown() {
        ReconciliationWork work = store.claimDue(NOW).orElseThrow();
        ReconciliationCase manual = work.reconciliationCase().moveToManualReview(
                "PENDING", NOW.plusSeconds(61)
        );
        NotificationTask task = work.task().transitionTo(TaskStatus.MANUAL_REVIEW, NOW.plusSeconds(61));

        assertThat(store.complete(work, new ReconciliationCompletion(
                manual, task, work.attempt()
        ))).isTrue();
        assertThat(status("reconciliation_case", "case_id", CASE_ID)).isEqualTo("MANUAL_REVIEW");
        assertThat(status("notification_task", "id", Long.toString(TASK_ID))).isEqualTo("MANUAL_REVIEW");
        assertThat(status("delivery_attempt", "attempt_id", ATTEMPT_ID)).isEqualTo("UNKNOWN");
    }

    private void insertUnknownTimeline() {
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO notification_task (
                        id, tenant_id, request_id, request_fingerprint, channel,
                        recipient_ref, template_code, variables_json, status,
                        current_attempt_no, last_error_category, last_error_code,
                        version, created_at, updated_at
                    ) VALUES (?, 'tenant-course', 'request-201', ?, 'COURSE_STUB',
                              'recipient-201', 'WELCOME_V1', CAST(? AS JSON), 'UNKNOWN',
                              1, 'UNKNOWN', 'READ_TIMEOUT', 2, ?, ?)
                    """);
            statement.setLong(1, TASK_ID);
            statement.setString(2, "b".repeat(64));
            statement.setBytes(3, "{}".getBytes(java.nio.charset.StandardCharsets.UTF_8));
            statement.setTimestamp(4, Timestamp.from(NOW.minusSeconds(60)));
            statement.setTimestamp(5, Timestamp.from(NOW.minusSeconds(5)));
            return statement;
        });
        jdbcTemplate.update("""
                INSERT INTO delivery_attempt (
                    attempt_id, task_id, attempt_no, provider_code, idempotency_key,
                    status, deadline_at, error_category, error_code, version,
                    started_at, finished_at
                ) VALUES (?, ?, 1, 'COURSE_STUB', 'idempotency-unknown',
                          'UNKNOWN', ?, 'UNKNOWN', 'READ_TIMEOUT', 1, ?, ?)
                """,
                ATTEMPT_ID,
                TASK_ID,
                Timestamp.from(NOW.minusSeconds(5)),
                Timestamp.from(NOW.minusSeconds(10)),
                Timestamp.from(NOW.minusSeconds(5))
        );
        jdbcTemplate.update("""
                INSERT INTO reconciliation_case (
                    case_id, task_id, attempt_id, status, query_count,
                    next_query_at, deadline_at, version, created_at, updated_at
                ) VALUES (?, ?, ?, 'OPEN', 0, ?, ?, 0, ?, ?)
                """,
                CASE_ID,
                TASK_ID,
                ATTEMPT_ID,
                Timestamp.from(NOW),
                Timestamp.from(NOW.plusSeconds(60)),
                Timestamp.from(NOW.minusSeconds(5)),
                Timestamp.from(NOW.minusSeconds(5))
        );
    }

    private String status(String table, String keyColumn, String key) {
        return jdbcTemplate.queryForObject(
                "SELECT status FROM " + table + " WHERE " + keyColumn + " = ?",
                String.class,
                key
        );
    }

    @Configuration(proxyBeanMethods = false)
    @EnableTransactionManagement(proxyTargetClass = true)
    static class TestConfiguration {

        @Bean
        DataSource dataSource() {
            JdbcDataSource dataSource = new JdbcDataSource();
            dataSource.setURL(
                    "jdbc:h2:mem:notifyflow_reconciliation_" + UUID.randomUUID()
                            + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1;INIT=SET TIME ZONE 'UTC'"
            );
            dataSource.setUser("sa");
            dataSource.setPassword("");
            Flyway.configure().dataSource(dataSource).load().migrate();
            return dataSource;
        }

        @Bean
        JdbcTemplate jdbcTemplate(DataSource dataSource) {
            return new JdbcTemplate(dataSource);
        }

        @Bean
        PlatformTransactionManager transactionManager(DataSource dataSource) {
            return new DataSourceTransactionManager(dataSource);
        }

        @Bean
        JdbcReconciliationStore jdbcReconciliationStore(JdbcTemplate jdbcTemplate) {
            return new JdbcReconciliationStore(jdbcTemplate);
        }
    }
}
