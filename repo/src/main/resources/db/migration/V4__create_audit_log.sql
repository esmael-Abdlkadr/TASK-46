CREATE TABLE audit_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT,
    username VARCHAR(50),
    action VARCHAR(20) NOT NULL,
    resource VARCHAR(100) NOT NULL,
    resource_id BIGINT,
    detail VARCHAR(500),
    workstation_id VARCHAR(100),
    timestamp DATETIME NOT NULL,
    INDEX idx_audit_user_id (user_id),
    INDEX idx_audit_action (action),
    INDEX idx_audit_timestamp (timestamp),
    INDEX idx_audit_workstation (workstation_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
