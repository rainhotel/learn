USE notifyflow_course;
START TRANSACTION;
UPDATE deadlock_demo SET value = value + 1 WHERE id = 2;
DO SLEEP(1);
UPDATE deadlock_demo SET value = value + 1 WHERE id = 1;
COMMIT;
SELECT 'session-b-completed' AS result;

