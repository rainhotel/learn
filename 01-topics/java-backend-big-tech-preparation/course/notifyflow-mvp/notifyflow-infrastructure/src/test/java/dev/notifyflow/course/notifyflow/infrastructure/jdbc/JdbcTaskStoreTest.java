package dev.notifyflow.course.notifyflow.infrastructure.jdbc;

import dev.notifyflow.course.notifyflow.application.CreateTaskDraft;
import dev.notifyflow.course.notifyflow.application.TaskCreationStore;
import dev.notifyflow.course.notifyflow.application.TaskQueryStore;
import dev.notifyflow.course.notifyflow.domain.NotificationTask;
import dev.notifyflow.course.notifyflow.domain.TaskStatus;
import org.flywaydb.core.Flyway;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static dev.notifyflow.course.notifyflow.application.TaskCreationStore.OutcomeStatus.CONFLICT;
import static dev.notifyflow.course.notifyflow.application.TaskCreationStore.OutcomeStatus.CREATED;
import static dev.notifyflow.course.notifyflow.application.TaskCreationStore.OutcomeStatus.REPLAYED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringJUnitConfig(JdbcTaskStoreTest.TestConfiguration.class)
class JdbcTaskStoreTest {

    private static final Instant CREATED_AT = Instant.parse("2026-07-15T10:00:00Z");
    private static final String FINGERPRINT_A = "a".repeat(64);
    private static final String FINGERPRINT_B = "b".repeat(64);

    private final TaskCreationStore creationStore;
    private final TaskQueryStore queryStore;
    private final JdbcTemplate jdbcTemplate;

    @Autowired
    JdbcTaskStoreTest(
            TaskCreationStore creationStore,
            TaskQueryStore queryStore,
            JdbcTemplate jdbcTemplate
    ) {
        this.creationStore = creationStore;
        this.queryStore = queryStore;
        this.jdbcTemplate = jdbcTemplate;
    }

    @BeforeEach
    void clearDatabase() {
        jdbcTemplate.update("DELETE FROM reconciliation_case");
        jdbcTemplate.update("DELETE FROM delivery_attempt");
        jdbcTemplate.update("DELETE FROM event_outbox");
        jdbcTemplate.update("DELETE FROM notification_task");
    }

    @Test
    void migrationCreatesAllPhaseOneAndFutureRecoveryTables() {
        List<String> tableNames = jdbcTemplate.queryForList("""
                SELECT table_name
                  FROM information_schema.tables
                 WHERE table_schema = 'public'
                   AND table_name IN (
                       'notification_task',
                       'delivery_attempt',
                       'event_outbox',
                       'reconciliation_case'
                   )
                 ORDER BY table_name
                """, String.class);

        assertThat(tableNames).containsExactly(
                "delivery_attempt",
                "event_outbox",
                "notification_task",
                "reconciliation_case"
        );
    }

    @Test
    void createsTaskAndOutboxInOneTransactionAndSupportsBothQueries() {
        TaskCreationStore.CreationOutcome outcome = creationStore.create(
                draft("tenant-course", "request-1", FINGERPRINT_A, "event-1")
        );

        assertThat(outcome.status()).isEqualTo(CREATED);
        NotificationTask task = outcome.task();
        assertThat(task.id()).isPositive();
        assertThat(task.status()).isEqualTo(TaskStatus.ACCEPTED);
        assertThat(task.currentAttemptNo()).isZero();
        assertThat(task.version()).isZero();
        assertThat(task.variablesJson()).isEqualTo("{\"name\":\"fixture-user\"}");

        assertThat(queryStore.findById(task.id())).contains(task);
        assertThat(queryStore.findByTenantAndRequestId("tenant-course", "request-1")).contains(task);
        assertThat(count("notification_task")).isEqualTo(1);
        assertThat(count("event_outbox")).isEqualTo(1);

        String payload = jdbcTemplate.queryForObject(
                "SELECT payload FROM event_outbox WHERE event_id = ?",
                String.class,
                "event-1"
        );
        assertThat(payload)
                .contains("\"eventType\":\"TaskAccepted\"")
                .contains("\"taskId\":" + task.id())
                .contains("\"tenantId\":\"tenant-course\"");
    }

    @Test
    void classifiesSameFingerprintAsReplayAndDifferentFingerprintAsConflict() {
        TaskCreationStore.CreationOutcome created = creationStore.create(
                draft("tenant-course", "request-1", FINGERPRINT_A, "event-1")
        );
        TaskCreationStore.CreationOutcome replayed = creationStore.create(
                draft("tenant-course", "request-1", FINGERPRINT_A, "event-not-written")
        );
        TaskCreationStore.CreationOutcome conflict = creationStore.create(
                draft("tenant-course", "request-1", FINGERPRINT_B, "event-also-not-written")
        );

        assertThat(replayed.status()).isEqualTo(REPLAYED);
        assertThat(replayed.task().id()).isEqualTo(created.task().id());
        assertThat(conflict.status()).isEqualTo(CONFLICT);
        assertThat(conflict.task().id()).isEqualTo(created.task().id());
        assertThat(count("notification_task")).isEqualTo(1);
        assertThat(count("event_outbox")).isEqualTo(1);
    }

    @Test
    void rollsBackTaskWhenOutboxInsertFails() {
        creationStore.create(draft("tenant-course", "request-1", FINGERPRINT_A, "duplicate-event"));

        assertThatThrownBy(() -> creationStore.create(
                draft("tenant-course", "request-2", FINGERPRINT_A, "duplicate-event")
        )).isInstanceOf(DuplicateKeyException.class);

        assertThat(queryStore.findByTenantAndRequestId("tenant-course", "request-2")).isEmpty();
        assertThat(count("notification_task")).isEqualTo(1);
        assertThat(count("event_outbox")).isEqualTo(1);
    }

    @Test
    void concurrentSameKeyCreationProducesOneTaskAndOneOutbox() throws Exception {
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            Future<TaskCreationStore.CreationOutcome> first = executor.submit(
                    () -> createAfterBarrier(ready, start, "event-concurrent-1")
            );
            Future<TaskCreationStore.CreationOutcome> second = executor.submit(
                    () -> createAfterBarrier(ready, start, "event-concurrent-2")
            );

            ready.await();
            start.countDown();
            List<TaskCreationStore.CreationOutcome> outcomes = List.of(first.get(), second.get());

            assertThat(outcomes).extracting(TaskCreationStore.CreationOutcome::status)
                    .containsExactlyInAnyOrder(CREATED, REPLAYED);
            assertThat(outcomes).extracting(outcome -> outcome.task().id())
                    .containsOnly(outcomes.getFirst().task().id());
        }

        assertThat(count("notification_task")).isEqualTo(1);
        assertThat(count("event_outbox")).isEqualTo(1);
    }

    private TaskCreationStore.CreationOutcome createAfterBarrier(
            CountDownLatch ready,
            CountDownLatch start,
            String eventId
    ) throws InterruptedException {
        ready.countDown();
        start.await();
        return creationStore.create(draft("tenant-course", "request-concurrent", FINGERPRINT_A, eventId));
    }

    private int count(String table) {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + table, Integer.class);
        return count == null ? 0 : count;
    }

    private static CreateTaskDraft draft(
            String tenantId,
            String requestId,
            String requestFingerprint,
            String eventId
    ) {
        return new CreateTaskDraft(
                tenantId,
                requestId,
                requestFingerprint,
                "COURSE_STUB",
                "recipient-fixture-001",
                "WELCOME_V1",
                "{\"name\":\"fixture-user\"}",
                eventId,
                CREATED_AT
        );
    }

    @Configuration(proxyBeanMethods = false)
    @EnableTransactionManagement(proxyTargetClass = true)
    static class TestConfiguration {

        @Bean
        DataSource dataSource() {
            JdbcDataSource dataSource = new JdbcDataSource();
            dataSource.setURL(
                    "jdbc:h2:mem:notifyflow_" + UUID.randomUUID() +
                            ";MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1;INIT=SET TIME ZONE 'UTC'"
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
        JdbcTaskStore jdbcTaskStore(JdbcTemplate jdbcTemplate) {
            return new JdbcTaskStore(jdbcTemplate);
        }
    }
}
