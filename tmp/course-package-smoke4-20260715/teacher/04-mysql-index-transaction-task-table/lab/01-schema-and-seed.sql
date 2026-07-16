DROP DATABASE IF EXISTS notifyflow_course;
CREATE DATABASE notifyflow_course
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_0900_ai_ci;

USE notifyflow_course;

CREATE TABLE notification_task (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    tenant_id BIGINT UNSIGNED NOT NULL,
    request_id VARCHAR(64) NOT NULL,
    status VARCHAR(16) NOT NULL,
    priority TINYINT UNSIGNED NOT NULL DEFAULT 0,
    scheduled_at DATETIME(6) NOT NULL,
    next_attempt_at DATETIME(6) NOT NULL,
    worker_id VARCHAR(64) NULL,
    lease_until DATETIME(6) NULL,
    attempt_count INT UNSIGNED NOT NULL DEFAULT 0,
    payload JSON NOT NULL,
    version INT UNSIGNED NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_task_tenant_request (tenant_id, request_id),
    CONSTRAINT chk_task_status CHECK (status IN ('PENDING', 'SENDING', 'SUCCESS', 'FAILED'))
) ENGINE = InnoDB;

CREATE TABLE digit (
    n TINYINT UNSIGNED NOT NULL PRIMARY KEY
) ENGINE = InnoDB;

INSERT INTO digit(n) VALUES (0),(1),(2),(3),(4),(5),(6),(7),(8),(9);

INSERT INTO notification_task (
    tenant_id,
    request_id,
    status,
    priority,
    scheduled_at,
    next_attempt_at,
    payload,
    version,
    created_at,
    updated_at
)
SELECT
    MOD(seq, 50) + 1,
    CONCAT('req-', LPAD(seq, 5, '0')),
    CASE
        WHEN MOD(seq, 10) < 6 THEN 'PENDING'
        WHEN MOD(seq, 10) < 8 THEN 'SUCCESS'
        WHEN MOD(seq, 10) = 8 THEN 'FAILED'
        ELSE 'SENDING'
    END,
    MOD(seq, 5),
    NOW(6) - INTERVAL MOD(seq, 30) DAY,
    NOW(6) - INTERVAL MOD(seq, 300) SECOND,
    JSON_OBJECT('recipient', CONCAT('user-', seq), 'template', 'course-demo'),
    0,
    NOW(6) - INTERVAL MOD(seq, 60) DAY,
    NOW(6) - INTERVAL MOD(seq, 60) DAY
FROM (
    SELECT
        a.n + b.n * 10 + c.n * 100 + d.n * 1000 + e.n * 10000 AS seq
    FROM digit a
    CROSS JOIN digit b
    CROSS JOIN digit c
    CROSS JOIN digit d
    CROSS JOIN digit e
) numbers
WHERE seq < 50000;

DROP TABLE digit;

SELECT VERSION() AS mysql_version,
       @@transaction_isolation AS default_isolation,
       COUNT(*) AS seeded_tasks
FROM notification_task;
