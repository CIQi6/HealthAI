CREATE TABLE IF NOT EXISTS audit_events (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    occurred_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    actor_id BIGINT NULL,
    actor_type VARCHAR(32) NULL,
    action VARCHAR(128) NOT NULL,
    resource_type VARCHAR(64) NOT NULL,
    resource_id VARCHAR(128) NULL,
    source_ip VARCHAR(64) NULL,
    metadata TEXT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_audit_action_time ON audit_events (action, occurred_at DESC);
CREATE INDEX idx_audit_resource ON audit_events (resource_type, resource_id);
