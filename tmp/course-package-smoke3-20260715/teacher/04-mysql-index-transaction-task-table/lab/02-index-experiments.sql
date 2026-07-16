USE notifyflow_course;

SELECT 'A. before composite index' AS experiment;
EXPLAIN ANALYZE
SELECT id, status, created_at
FROM notification_task
WHERE tenant_id = 1
  AND status = 'PENDING'
  AND created_at >= NOW() - INTERVAL 60 DAY
ORDER BY created_at DESC
LIMIT 20;

CREATE INDEX idx_task_tenant_status_created
    ON notification_task (tenant_id, status, created_at DESC, id);

SELECT 'B. after composite index: covering query' AS experiment;
EXPLAIN ANALYZE
SELECT id, status, created_at
FROM notification_task
WHERE tenant_id = 1
  AND status = 'PENDING'
  AND created_at >= NOW() - INTERVAL 60 DAY
ORDER BY created_at DESC
LIMIT 20;

SELECT 'C. same predicate but payload requires row lookup' AS experiment;
EXPLAIN ANALYZE
SELECT id, status, created_at, payload
FROM notification_task
WHERE tenant_id = 1
  AND status = 'PENDING'
  AND created_at >= NOW() - INTERVAL 60 DAY
ORDER BY created_at DESC
LIMIT 20;

SELECT 'D. missing leftmost tenant_id' AS experiment;
EXPLAIN ANALYZE
SELECT id, status, created_at
FROM notification_task
WHERE status = 'PENDING'
ORDER BY created_at DESC
LIMIT 20;

CREATE INDEX idx_task_claim
    ON notification_task (status, next_attempt_at, id);

SELECT 'E. claim query index' AS experiment;
EXPLAIN ANALYZE
SELECT id, status, next_attempt_at
FROM notification_task
WHERE status = 'PENDING'
  AND next_attempt_at <= NOW(6)
ORDER BY next_attempt_at, id
LIMIT 10;

SHOW INDEX FROM notification_task;
