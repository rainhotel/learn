package dev.notifyflow.course.notifyflow.application;

import dev.notifyflow.course.notifyflow.domain.OutboxMessage;

import java.time.Instant;
import java.util.List;

/** Persistence boundary for leased, optimistic-lock-protected outbox publishing. */
public interface OutboxStore {
    List<OutboxMessage> claimDue(Instant now, String leaseOwner, Instant leaseUntil, int limit);

    boolean save(OutboxMessage expected, OutboxMessage updated);
}
