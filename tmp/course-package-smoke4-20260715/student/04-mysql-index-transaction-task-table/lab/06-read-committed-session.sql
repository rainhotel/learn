USE notifyflow_course;
SET SESSION TRANSACTION ISOLATION LEVEL READ COMMITTED;
START TRANSACTION;
SELECT 'rc-first' AS observation, COUNT(*) AS task_count
FROM notification_task
WHERE tenant_id = 998;
DO SLEEP(3);
SELECT 'rc-second' AS observation, COUNT(*) AS task_count
FROM notification_task
WHERE tenant_id = 998;
COMMIT;

