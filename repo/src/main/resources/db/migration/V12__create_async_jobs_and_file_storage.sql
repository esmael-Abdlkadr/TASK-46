-- Async job queue
CREATE TABLE async_jobs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    job_type VARCHAR(30) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'QUEUED',
    description VARCHAR(200),
    input_data TEXT,
    result_data TEXT,
    error_message VARCHAR(1000),
    progress_percent INT DEFAULT 0,
    submitted_by BIGINT NOT NULL,
    submitted_by_username VARCHAR(50),
    created_at DATETIME NOT NULL,
    started_at DATETIME,
    completed_at DATETIME,
    INDEX idx_ajob_status (status),
    INDEX idx_ajob_type (job_type),
    INDEX idx_ajob_created (created_at),
    FOREIGN KEY (submitted_by) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Stored files with SHA-256 fingerprint deduplication
CREATE TABLE stored_files (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    original_name VARCHAR(255) NOT NULL,
    stored_path VARCHAR(500) NOT NULL,
    fingerprint VARCHAR(64) NOT NULL,
    file_size BIGINT,
    content_type VARCHAR(100),
    category VARCHAR(50),
    uploaded_by BIGINT,
    created_at DATETIME NOT NULL,
    CONSTRAINT uk_file_fingerprint UNIQUE (fingerprint),
    INDEX idx_sf_category (category)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
