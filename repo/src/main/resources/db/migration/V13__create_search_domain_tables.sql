CREATE TABLE IF NOT EXISTS members (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    member_code VARCHAR(30) NOT NULL UNIQUE,
    full_name VARCHAR(200) NOT NULL,
    email VARCHAR(150),
    phone VARCHAR(30),
    membership_tier VARCHAR(30),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME NOT NULL,
    INDEX idx_member_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS enterprises (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    enterprise_code VARCHAR(30) NOT NULL UNIQUE,
    name VARCHAR(200) NOT NULL,
    industry VARCHAR(100),
    contact_email VARCHAR(150),
    location VARCHAR(200),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS resources (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    resource_code VARCHAR(30) NOT NULL UNIQUE,
    name VARCHAR(200) NOT NULL,
    resource_type VARCHAR(50),
    location VARCHAR(200),
    available BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS service_orders (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_code VARCHAR(30) NOT NULL UNIQUE,
    member_id BIGINT,
    enterprise_id BIGINT,
    description VARCHAR(200),
    amount DECIMAL(12,2),
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    created_at DATETIME NOT NULL,
    INDEX idx_order_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS redemption_records (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    redemption_code VARCHAR(30) NOT NULL UNIQUE,
    member_id BIGINT NOT NULL,
    item_description VARCHAR(300) NOT NULL,
    points_redeemed INT,
    redemption_value DECIMAL(12,2),
    status VARCHAR(30) NOT NULL DEFAULT 'COMPLETED',
    created_at DATETIME NOT NULL,
    INDEX idx_redemption_member (member_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
