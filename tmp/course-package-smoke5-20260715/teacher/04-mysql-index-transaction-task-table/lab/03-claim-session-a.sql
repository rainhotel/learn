USE notifyflow_course;
SET SESSION TRANSACTION ISOLATION LEVEL READ COMMITTED;
START TRANSACTION;
SELECT 'session-a' AS worker, id
FROM notification_task
WHERE status = 'PENDING'
  AND next_attempt_at <= NOW(6)
ORDER BY next_attempt_at, id
LIMIT 5
FOR UPDATE SKIP LOCKED;
DO SLEEP(3);
COMMIT;

