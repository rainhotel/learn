package dev.notifyflow.course.notifyflow.application;

import dev.notifyflow.course.notifyflow.domain.NotificationTask;

import java.util.Optional;

/** Read-only persistence port used by HTTP queries and idempotency classification. */
public interface TaskQueryStore {
    Optional<NotificationTask> findById(long taskId);

    Optional<NotificationTask> findByTenantAndRequestId(String tenantId, String requestId);
}
