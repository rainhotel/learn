CREATE TABLE notification_task (
    id BIGINT NOT NULL AUTO_INCREMENT,
    tenant_id VARCHAR(64) NOT NULL,
    request_id VARCHAR(64) NOT NULL,
    request_fingerprint CHAR(64) NOT NULL,
    channel VARCHAR(32) NOT NULL,
    recipient_ref VARCHAR(128) NOT NULL,
    template_code VARCHAR(64) NOT NULL,
    variables_json JSON NOT NULL,
    status VARCHAR(32) NOT NULL,
    current_attempt_no INT NOT NULL DEFAULT 0,
    last_error_category VARCHAR(32) NULL,
    last_error_code VARCHAR(64) NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT pk_notification_task PRIMARY KEY (id),
    CONSTRAINT uk_task_request UNIQUE (tenant_id, request_id)
);

CREATE INDEX idx_task_status_updated
    ON notification_task (status, updated_at, id);

CREATE TABLE delivery_attempt (
    attempt_id VARCHAR(64) NOT NULL,
    task_id BIGINT NOT NULL,
    attempt_no INT NOT NULL,
    provider_code VARCHAR(64) NOT NULL,
    idempotency_key VARCHAR(128) NOT NULL,
    provider_request_id VARCHAR(128) NULL,
    status VARCHAR(32) NOT NULL,
    deadline_at DATETIME(6) NOT NULL,
    error_category VARCHAR(32) NULL,
    error_code VARCHAR(64) NULL,
    version BIGINT NOT NULL DEFAULT 0,
    started_at DATETIME(6) NOT NULL,
    finished_at DATETIME(6) NULL,
    CONSTRAINT pk_delivery_attempt PRIMARY KEY (attempt_id),
    CONSTRAINT uk_task_attempt UNIQUE (task_id, attempt_no),
    CONSTRAINT uk_provider_idempotency UNIQUE (provider_code, idempotency_key),
    CONSTRAINT fk_attempt_task FOREIGN KEY (task_id) REFERENCES notification_task (id)
);

CREATE INDEX idx_attempt_recovery
    ON delivery_attempt (status, deadline_at, task_id);

CREATE TABLE event_outbox (
    id BIGINT NOT NULL AUTO_INCREMENT,
    event_id VARCHAR(64) NOT NULL,
    aggregate_type VARCHAR(64) NOT NULL,
    aggregate_id VARCHAR(64) NOT NULL,
    event_type VARCHAR(128) NOT NULL,
    event_version INT NOT NULL,
    partition_key VARCHAR(128) NOT NULL,
    payload JSON NOT NULL,
    status VARCHAR(16) NOT NULL,
    attempt_count INT NOT NULL DEFAULT 0,
    next_attempt_at DATETIME(6) NOT NULL,
    lease_owner VARCHAR(128) NULL,
    lease_until DATETIME(6) NULL,
    published_at DATETIME(6) NULL,
    last_error VARCHAR(512) NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT pk_event_outbox PRIMARY KEY (id),
    CONSTRAINT uk_outbox_event UNIQUE (event_id)
);

CREATE INDEX idx_outbox_publish
    ON event_outbox (status, next_attempt_at, id);

CREATE INDEX idx_outbox_lease
    ON event_outbox (lease_until, id);

CREATE TABLE reconciliation_case (
    case_id VARCHAR(64) NOT NULL,
    task_id BIGINT NOT NULL,
    attempt_id VARCHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL,
    query_count INT NOT NULL DEFAULT 0,
    next_query_at DATETIME(6) NOT NULL,
    deadline_at DATETIME(6) NOT NULL,
    last_provider_status VARCHAR(32) NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    resolved_at DATETIME(6) NULL,
    CONSTRAINT pk_reconciliation_case PRIMARY KEY (case_id),
    CONSTRAINT uk_reconciliation_attempt UNIQUE (attempt_id),
    CONSTRAINT fk_case_task FOREIGN KEY (task_id) REFERENCES notification_task (id),
    CONSTRAINT fk_case_attempt FOREIGN KEY (attempt_id) REFERENCES delivery_attempt (attempt_id)
);

CREATE INDEX idx_reconciliation_due
    ON reconciliation_case (status, next_query_at, case_id);
