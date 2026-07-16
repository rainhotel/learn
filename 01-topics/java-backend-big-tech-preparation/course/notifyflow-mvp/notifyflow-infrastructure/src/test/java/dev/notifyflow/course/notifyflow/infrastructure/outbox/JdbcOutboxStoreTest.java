package dev.notifyflow.course.notifyflow.infrastructure.outbox;

import dev.notifyflow.course.notifyflow.application.OutboxStore;
import dev.notifyflow.course.notifyflow.domain.OutboxMessage;
import dev.notifyflow.course.notifyflow.domain.OutboxStatus;
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
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringJUnitConfig(JdbcOutboxStoreTest.TestConfiguration.class)
class JdbcOutboxStoreTest {

    private static final Instant NOW = Instant.parse("2026-07-15T10:00:00Z");

    private final OutboxStore store;
    private final JdbcTemplate jdbcTemplate;

    JdbcOutboxStoreTest(OutboxStore store, JdbcTemplate jdbcTemplate) {
        this.store = store;
        this.jdbcTemplate = jdbcTemplate;
    }

    @BeforeEach
    void clearDatabase() {
        jdbcTemplate.update("DELETE FROM event_outbox");
    }

    @Test
    void claimsOnlyDueRowsAndPersistsPublishedOutcomeWithOptimisticLock() {
        insert("due", OutboxStatus.PENDING, NOW.minusSeconds(1), null, null);
        insert("future", OutboxStatus.PENDING, NOW.plusSeconds(60), null, null);
        insert("leased", OutboxStatus.RETRY, NOW.minusSeconds(1), "other", NOW.plusSeconds(60));

        List<OutboxMessage> claimed = store.claimDue(NOW, "publisher-a", NOW.plusSeconds(30), 10);

        assertThat(claimed).extracting(OutboxMessage::eventId).containsExactly("due");
        OutboxMessage expected = claimed.getFirst();
        assertThat(expected.leaseOwner()).isEqualTo("publisher-a");
        assertThat(expected.version()).isEqualTo(1);

        OutboxMessage published = expected.markPublished(NOW.plusSeconds(1));
        assertThat(store.save(expected, published)).isTrue();
        assertThat(store.save(expected, published)).isFalse();

        Row row = row("due");
        assertThat(row.status()).isEqualTo("PUBLISHED");
        assertThat(row.version()).isEqualTo(2);
        assertThat(row.leaseOwner()).isNull();
        assertThat(row.publishedAt()).isEqualTo(NOW.plusSeconds(1));
    }

    @Test
    void expiredLeaseCanBeReclaimedAndFailureCanBeScheduledForRetry() {
        insert("expired", OutboxStatus.PENDING, NOW.minusSeconds(10), "dead-publisher", NOW.minusSeconds(1));

        OutboxMessage expected = store.claimDue(NOW, "publisher-b", NOW.plusSeconds(30), 1).getFirst();
        OutboxMessage retry = expected.scheduleRetry(
                NOW.plusSeconds(20),
                "broker unavailable",
                NOW.plusSeconds(1)
        );

        assertThat(store.save(expected, retry)).isTrue();
        Row row = row("expired");
        assertThat(row.status()).isEqualTo("RETRY");
        assertThat(row.attemptCount()).isEqualTo(1);
        assertThat(row.nextAttemptAt()).isEqualTo(NOW.plusSeconds(20));
        assertThat(row.lastError()).isEqualTo("broker unavailable");
        assertThat(row.leaseOwner()).isNull();
    }

    @Test
    void competingClaimsNeverReturnTheSameLeaseVersion() {
        insert("one", OutboxStatus.PENDING, NOW, null, null);

        List<OutboxMessage> first = store.claimDue(NOW, "publisher-a", NOW.plusSeconds(30), 1);
        List<OutboxMessage> second = store.claimDue(NOW, "publisher-b", NOW.plusSeconds(30), 1);

        assertThat(first).hasSize(1);
        assertThat(second).isEmpty();
    }

    private void insert(
            String eventId,
            OutboxStatus status,
            Instant nextAttemptAt,
            String leaseOwner,
            Instant leaseUntil
    ) {
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO event_outbox (
                        event_id, aggregate_type, aggregate_id, event_type, event_version,
                        partition_key, payload, status, attempt_count, next_attempt_at,
                        lease_owner, lease_until, version, created_at, updated_at
                    ) VALUES (?, 'NotificationTask', '1', 'TaskAccepted', 1,
                              '1', CAST(? AS JSON), ?, 0, ?, ?, ?, 0, ?, ?)
                    """);
            statement.setString(1, eventId);
            statement.setBytes(2, "{}".getBytes(java.nio.charset.StandardCharsets.UTF_8));
            statement.setString(3, status.name());
            statement.setTimestamp(4, Timestamp.from(nextAttemptAt));
            statement.setString(5, leaseOwner);
            statement.setTimestamp(6, leaseUntil == null ? null : Timestamp.from(leaseUntil));
            statement.setTimestamp(7, Timestamp.from(NOW.minusSeconds(60)));
            statement.setTimestamp(8, Timestamp.from(NOW.minusSeconds(60)));
            return statement;
        });
    }

    private Row row(String eventId) {
        return jdbcTemplate.queryForObject("""
                        SELECT status, attempt_count, next_attempt_at, lease_owner,
                               published_at, last_error, version
                          FROM event_outbox
                         WHERE event_id = ?
                        """,
                (resultSet, rowNumber) -> new Row(
                        resultSet.getString("status"),
                        resultSet.getInt("attempt_count"),
                        resultSet.getTimestamp("next_attempt_at").toInstant(),
                        resultSet.getString("lease_owner"),
                        resultSet.getTimestamp("published_at") == null
                                ? null
                                : resultSet.getTimestamp("published_at").toInstant(),
                        resultSet.getString("last_error"),
                        resultSet.getLong("version")
                ),
                eventId
        );
    }

    private record Row(
            String status,
            int attemptCount,
            Instant nextAttemptAt,
            String leaseOwner,
            Instant publishedAt,
            String lastError,
            long version
    ) {
    }

    @Configuration(proxyBeanMethods = false)
    @EnableTransactionManagement(proxyTargetClass = true)
    static class TestConfiguration {

        @Bean
        DataSource dataSource() {
            JdbcDataSource dataSource = new JdbcDataSource();
            dataSource.setURL(
                    "jdbc:h2:mem:notifyflow_outbox_" + UUID.randomUUID()
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
        JdbcOutboxStore jdbcOutboxStore(JdbcTemplate jdbcTemplate) {
            return new JdbcOutboxStore(jdbcTemplate);
        }
    }
}
