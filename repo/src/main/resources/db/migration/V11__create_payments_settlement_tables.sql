-- Payment transactions with idempotency key
CREATE TABLE payment_transactions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    idempotency_key VARCHAR(64) NOT NULL,
    reference_number VARCHAR(50) NOT NULL,
    amount DECIMAL(12,2) NOT NULL,
    refunded_amount DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    channel VARCHAR(20) NOT NULL,
    status VARCHAR(25) NOT NULL DEFAULT 'RECORDED',
    location VARCHAR(200),
    payer_name VARCHAR(200),
    description VARCHAR(500),
    check_number VARCHAR(30),
    card_last_four VARCHAR(4),
    recorded_by BIGINT NOT NULL,
    recorded_by_username VARCHAR(50),
    transaction_date DATETIME NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME,
    CONSTRAINT uk_idempotency_key UNIQUE (idempotency_key),
    INDEX idx_pay_status (status),
    INDEX idx_pay_channel (channel),
    INDEX idx_pay_location (location),
    INDEX idx_pay_date (transaction_date),
    INDEX idx_pay_reference (reference_number),
    FOREIGN KEY (recorded_by) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Refunds
CREATE TABLE refunds (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    payment_id BIGINT NOT NULL,
    amount DECIMAL(12,2) NOT NULL,
    reason VARCHAR(500) NOT NULL,
    processed_by BIGINT NOT NULL,
    processed_by_username VARCHAR(50),
    created_at DATETIME NOT NULL,
    INDEX idx_refund_payment (payment_id),
    FOREIGN KEY (payment_id) REFERENCES payment_transactions(id),
    FOREIGN KEY (processed_by) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Bank file imports (idempotent via SHA-256 hash)
CREATE TABLE bank_file_imports (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    file_name VARCHAR(200) NOT NULL,
    file_hash VARCHAR(64) NOT NULL,
    total_entries INT,
    imported_by BIGINT NOT NULL,
    imported_by_username VARCHAR(50),
    created_at DATETIME NOT NULL,
    CONSTRAINT uk_bank_file_hash UNIQUE (file_hash),
    FOREIGN KEY (imported_by) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Bank entries (from imported files)
CREATE TABLE bank_entries (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    import_id BIGINT NOT NULL,
    bank_reference VARCHAR(50) NOT NULL,
    amount DECIMAL(12,2) NOT NULL,
    entry_date DATE NOT NULL,
    description VARCHAR(500),
    matched_payment_id BIGINT,
    matched BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME NOT NULL,
    INDEX idx_bank_ref (bank_reference),
    INDEX idx_bank_date (entry_date),
    INDEX idx_bank_import (import_id),
    FOREIGN KEY (import_id) REFERENCES bank_file_imports(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Reconciliation exceptions queue
CREATE TABLE reconciliation_exceptions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    status VARCHAR(30) NOT NULL,
    payment_id BIGINT,
    bank_entry_id BIGINT,
    payment_amount DECIMAL(12,2),
    bank_amount DECIMAL(12,2),
    discrepancy_amount DECIMAL(12,2),
    description VARCHAR(500),
    resolution_notes VARCHAR(1000),
    resolved_by VARCHAR(50),
    resolved_at DATETIME,
    created_at DATETIME NOT NULL,
    INDEX idx_recon_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
