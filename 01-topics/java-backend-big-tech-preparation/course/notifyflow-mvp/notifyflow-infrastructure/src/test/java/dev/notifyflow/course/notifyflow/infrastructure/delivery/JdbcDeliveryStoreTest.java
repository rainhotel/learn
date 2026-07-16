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

@SpringJUnitConfig(JdbcDeliveryStoreTest.TestConfiguration.class)
class JdbcDeliveryStoreTest {

    private static final Instant NOW = Instant.parse("2026-07-15T10:00:00Z");
    private static final long TASK_ID = 101L;

    private final DeliveryStore store;
    private final JdbcTemplate jdbcTemplate;

    JdbcDeliveryStoreTest(DeliveryStore store, JdbcTemplate jdbcTemplate) {
        this.store = store;
        this.jdbcTemplate = jdbcTemplate;
    }

    @BeforeEach
    void reset() {
        jdbcTemplate.update("DELETE FROM reconciliation_case");
        jdbcTemplate.update("DELETE FROM delivery_attempt");
        jdbcTemplate.update("DELETE FROM notification_task");
        insertAcceptedTask();
    }

    @Test
    void onlyOneConsumerCanStartAcceptedTaskAndCreateUniqueAttempt() {
        DeliveryWork work = start("attempt-1");

        assertThat(work.task().status()).isEqualTo(TaskStatus.SENDING);
        assertThat(work.task().currentAttemptNo()).isEqualTo(1);
        assertThat(work.task().version()).isEqualTo(1);
        assertThat(work.attempt().status()).isEqualTo(AttemptStatus.SENDING);
        assertThat(store.tryStart(
                TASK_ID, "attempt-duplicate", "COURSE_STUB", "idempotency-duplicate",
                NOW, NOW.plusSeconds(5)
        )).isEmpty();
        assertThat(count("delivery_attempt")).isEqualTo(1);
    }

    @Test
    void successUpdatesTaskAndAttemptTogetherAndRejectsStaleCompletion() {
        DeliveryWork work = start("attempt-success");
        DeliveryAttempt attempt = work.attempt().transitionTo(
                AttemptStatus.SUCCEEDED, NOW.plusSeconds(1), "provider-1", null, null
        );
        NotificationTask task = work.task().transitionTo(TaskStatus.SUCCEEDED, NOW.plusSeconds(1));
        DeliveryCompletion completion = new DeliveryCompletion(task, attempt, null);

        assertThat(store.complete(work, completion)).isTrue();
        assertThat(store.complete(work, completion)).isFalse();
        assertThat(status("notification_task", "id", Long.toString(TASK_ID))).isEqualTo("SUCCEEDED");
        assertThat(status("delivery_attempt", "attempt_id", "attempt-success")).isEqualTo("SUCCEEDED");
        assertThat(count("reconciliation_case")).isZero();
    }

    @Test
    void timeoutAtomicallyWritesUnknownTaskAttemptAndOneOpenCase() {
        DeliveryWork work = start("attempt-timeout");
        DeliveryAttempt attempt = work.attempt().transitionTo(
                AttemptStatus.UNKNOWN, NOW.plusSeconds(5), null, "UNKNOWN", "READ_TIMEOUT"
        );
        NotificationTask task = work.task().transitionTo(
                TaskStatus.UNKNOWN, NOW.plusSeconds(5), "UNKNOWN", "READ_TIMEOUT"
        );
        ReconciliationCase reconciliationCase = new ReconciliationCase(
                "case-timeout", TASK_ID, attempt.attemptId(), ReconciliationStatus.OPEN,
                0, NOW.plusSeconds(6), NOW.plusSeconds(60), null, 0,
                NOW.plusSeconds(5), NOW.plusSeconds(5), null
        );

        assertThat(store.complete(work, new DeliveryCompletion(task, attempt, reconciliationCase))).isTrue();
        assertThat(status("notification_task", "id", Long.toString(TASK_ID))).isEqualTo("UNKNOWN");
        assertThat(status("delivery_attempt", "attempt_id", "attempt-timeout")).isEqualTo("UNKNOWN");
        assertThat(status("reconciliation_case", "case_id", "case-timeout")).isEqualTo("OPEN");
        assertThat(count("reconciliation_case")).isEqualTo(1);
    }

    @Test
    void permanentRejectionWritesBothTerminalStatesWithoutCase() {
        DeliveryWork work = start("attempt-rejected");
        DeliveryAttempt attempt = work.attempt().transitionTo(
                AttemptStatus.PERMANENT_FAILED, NOW.plusSeconds(1), "provider-2", "PROVIDER", "REJECTED"
        );
        NotificationTask task = work.task().transitionTo(
                TaskStatus.FAILED, NOW.plusSeconds(1), "PROVIDER", "REJECTED"
        );

        assertThat(store.complete(work, new DeliveryCompletion(task, attempt, null))).isTrue();
        assertThat(status("notification_task", "id", Long.toString(TASK_ID))).isEqualTo("FAILED");
        assertThat(status("delivery_attempt", "attempt_id", "attempt-rejected"))
                .isEqualTo("PERMANENT_FAILED");
        assertThat(count("reconciliation_case")).isZero();
    }

    private DeliveryWork start(String attemptId) {
        return store.tryStart(
                        TASK_ID, attemptId, "COURSE_STUB", "idempotency-" + attemptId,
                        NOW, NOW.plusSeconds(5)
                )
                .orElseThrow();
    }

    private void insertAcceptedTask() {
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO notification_task (
                        id, tenant_id, request_id, request_fingerprint, channel,
                        recipient_ref, template_code, variables_json, status,
                        current_attempt_no, version, created_at, updated_at
                    ) VALUES (?, 'tenant-course', 'request-101', ?, 'COURSE_STUB',
                              'recipient-101', 'WELCOME_V1', CAST(? AS JSON), 'ACCEPTED',
                              0, 0, ?, ?)
                    """);
            statement.setLong(1, TASK_ID);
            statement.setString(2, "a".repeat(64));
            statement.setBytes(3, "{}".getBytes(java.nio.charset.StandardCharsets.UTF_8));
            statement.setTimestamp(4, Timestamp.from(NOW.minusSeconds(60)));
            statement.setTimestamp(5, Timestamp.from(NOW.minusSeconds(60)));
            return statement;
        });
    }

    private int count(String table) {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + table, Integer.class);
        return count == null ? 0 : count;
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
                    "jdbc:h2:mem:notifyflow_delivery_" + UUID.randomUUID()
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
        JdbcDeliveryStore jdbcDeliveryStore(JdbcTemplate jdbcTemplate) {
            return new JdbcDeliveryStore(jdbcTemplate);
        }
    }
}
