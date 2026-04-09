-- Collector profiles
CREATE TABLE collector_profiles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    employee_id VARCHAR(30) UNIQUE,
    phone VARCHAR(30),
    email VARCHAR(150),
    zone VARCHAR(100),
    skills VARCHAR(500),
    status VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE',
    max_concurrent_jobs INT DEFAULT 1,
    created_at DATETIME NOT NULL,
    updated_at DATETIME,
    INDEX idx_collector_status (status),
    INDEX idx_collector_zone (zone)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Site profiles
CREATE TABLE site_profiles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    address VARCHAR(500),
    zone VARCHAR(100),
    capacity_limit INT NOT NULL DEFAULT 10,
    current_occupancy INT NOT NULL DEFAULT 0,
    dispatch_mode VARCHAR(20) NOT NULL DEFAULT 'ASSIGNED_ORDER',
    contact_name VARCHAR(100),
    contact_phone VARCHAR(30),
    notes VARCHAR(1000),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME NOT NULL,
    updated_at DATETIME,
    INDEX idx_site_zone (zone),
    INDEX idx_site_active (active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Work shifts (15-minute increments)
CREATE TABLE work_shifts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    collector_id BIGINT NOT NULL,
    day_of_week VARCHAR(10) NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    site_id BIGINT,
    INDEX idx_shift_collector (collector_id),
    INDEX idx_shift_day (day_of_week),
    FOREIGN KEY (collector_id) REFERENCES collector_profiles(id) ON DELETE CASCADE,
    FOREIGN KEY (site_id) REFERENCES site_profiles(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Dispatch assignments
CREATE TABLE dispatch_assignments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    site_id BIGINT NOT NULL,
    collector_id BIGINT,
    title VARCHAR(200) NOT NULL,
    description VARCHAR(1000),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    dispatch_mode VARCHAR(20) NOT NULL,
    scheduled_start DATETIME,
    scheduled_end DATETIME,
    acceptance_expires_at DATETIME,
    offer_count INT NOT NULL DEFAULT 0,
    assigned_by BIGINT,
    accepted_at DATETIME,
    completed_at DATETIME,
    created_at DATETIME NOT NULL,
    updated_at DATETIME,
    INDEX idx_dispatch_status (status),
    INDEX idx_dispatch_collector (collector_id),
    INDEX idx_dispatch_site (site_id),
    INDEX idx_dispatch_expires (acceptance_expires_at),
    FOREIGN KEY (site_id) REFERENCES site_profiles(id),
    FOREIGN KEY (collector_id) REFERENCES collector_profiles(id),
    FOREIGN KEY (assigned_by) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Dispatch offer log (tracks each offer/timeout/decline)
CREATE TABLE dispatch_offer_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    assignment_id BIGINT NOT NULL,
    collector_id BIGINT NOT NULL,
    collector_name VARCHAR(200),
    outcome VARCHAR(20) NOT NULL,
    offered_at DATETIME NOT NULL,
    responded_at DATETIME,
    INDEX idx_offer_assignment (assignment_id),
    INDEX idx_offer_collector (collector_id),
    FOREIGN KEY (assignment_id) REFERENCES dispatch_assignments(id),
    FOREIGN KEY (collector_id) REFERENCES collector_profiles(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
