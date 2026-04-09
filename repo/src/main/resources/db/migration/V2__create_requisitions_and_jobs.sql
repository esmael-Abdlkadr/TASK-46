CREATE TABLE requisitions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    description VARCHAR(1000),
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    created_by BIGINT,
    assigned_to BIGINT,
    created_at DATETIME NOT NULL,
    updated_at DATETIME,
    INDEX idx_req_status (status),
    FOREIGN KEY (created_by) REFERENCES users(id),
    FOREIGN KEY (assigned_to) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE jobs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    requisition_id BIGINT,
    title VARCHAR(200),
    location VARCHAR(200),
    status VARCHAR(20) NOT NULL DEFAULT 'UNASSIGNED',
    assigned_to BIGINT,
    scheduled_date DATE,
    created_at DATETIME NOT NULL,
    updated_at DATETIME,
    INDEX idx_job_status (status),
    FOREIGN KEY (requisition_id) REFERENCES requisitions(id),
    FOREIGN KEY (assigned_to) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
