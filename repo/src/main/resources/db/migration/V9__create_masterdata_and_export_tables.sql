-- Departments
CREATE TABLE departments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(20) NOT NULL,
    name VARCHAR(200) NOT NULL,
    parent_id BIGINT,
    head_name VARCHAR(100),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME NOT NULL,
    updated_at DATETIME,
    CONSTRAINT uk_dept_code UNIQUE (code),
    FOREIGN KEY (parent_id) REFERENCES departments(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Staffing classes
CREATE TABLE staffing_classes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(20) NOT NULL,
    name VARCHAR(200) NOT NULL,
    department_id BIGINT,
    max_headcount INT,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME NOT NULL,
    updated_at DATETIME,
    CONSTRAINT uk_class_code UNIQUE (code),
    FOREIGN KEY (department_id) REFERENCES departments(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Training courses
CREATE TABLE training_courses (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(20) NOT NULL,
    name VARCHAR(200) NOT NULL,
    description VARCHAR(1000),
    department_id BIGINT,
    credit_hours INT,
    mandatory BOOLEAN NOT NULL DEFAULT FALSE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME NOT NULL,
    updated_at DATETIME,
    CONSTRAINT uk_course_code UNIQUE (code),
    FOREIGN KEY (department_id) REFERENCES departments(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Semesters / training periods
CREATE TABLE semesters (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(20) NOT NULL,
    name VARCHAR(100) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME NOT NULL,
    CONSTRAINT uk_semester_code UNIQUE (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Export jobs (async queue)
CREATE TABLE export_jobs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    export_type VARCHAR(50) NOT NULL,
    file_format VARCHAR(10) NOT NULL,
    search_criteria_json TEXT,
    status VARCHAR(30) NOT NULL DEFAULT 'QUEUED',
    file_path VARCHAR(500),
    file_name VARCHAR(200),
    record_count INT,
    error_message VARCHAR(1000),
    created_by BIGINT NOT NULL,
    created_at DATETIME NOT NULL,
    started_at DATETIME,
    completed_at DATETIME,
    INDEX idx_export_status (status),
    INDEX idx_export_user (created_by),
    FOREIGN KEY (created_by) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Import jobs with SHA-256 fingerprint
CREATE TABLE import_jobs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    original_file_name VARCHAR(200) NOT NULL,
    import_type VARCHAR(50) NOT NULL,
    file_fingerprint VARCHAR(64) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'QUEUED',
    total_rows INT,
    success_rows INT,
    error_rows INT,
    error_report TEXT,
    file_path VARCHAR(500),
    created_by BIGINT NOT NULL,
    created_at DATETIME NOT NULL,
    completed_at DATETIME,
    INDEX idx_import_status (status),
    INDEX idx_import_fingerprint (file_fingerprint),
    FOREIGN KEY (created_by) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
