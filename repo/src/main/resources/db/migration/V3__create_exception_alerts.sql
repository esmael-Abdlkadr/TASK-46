CREATE TABLE exception_alerts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    type VARCHAR(50) NOT NULL,
    severity VARCHAR(20) NOT NULL,
    message VARCHAR(500) NOT NULL,
    related_entity_id BIGINT,
    resolved BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME NOT NULL,
    INDEX idx_alert_resolved (resolved)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
