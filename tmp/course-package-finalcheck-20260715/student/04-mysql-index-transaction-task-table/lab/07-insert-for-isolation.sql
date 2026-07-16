USE notifyflow_course;
INSERT INTO notification_task (
    tenant_id, request_id, status, priority,
    scheduled_at, next_attempt_at, payload,
    version, created_at, updated_at
) VALUES (
    @tenant_for_insert,
    CONCAT('isolation-', @tenant_for_insert, '-', UUID()),
    'PENDING',
    1,
    NOW(6), NOW(6), JSON_OBJECT('source', 'isolation-lab'),
    0, NOW(6), NOW(6)
);

