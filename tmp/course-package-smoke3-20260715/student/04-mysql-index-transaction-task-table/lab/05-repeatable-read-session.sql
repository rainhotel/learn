USE notifyflow_course;
SET SESSION TRANSACTION ISOLATION LEVEL REPEATABLE READ;
START TRANSACTION;
SELECT 'rr-first' AS observation, COUNT(*) AS task_count
FROM notification_task
WHERE tenant_id = 999;
DO SLEEP(3);
SELECT 'rr-second' AS observation, COUNT(*) AS task_count
FROM notification_task
WHERE tenant_id = 999;
COMMIT;

