-- Candidate profiles
CREATE TABLE candidate_profiles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    email VARCHAR(150),
    phone VARCHAR(30),
    location VARCHAR(200),
    current_title VARCHAR(200),
    current_employer VARCHAR(200),
    years_of_experience INT,
    education_level VARCHAR(100),
    summary VARCHAR(2000),
    pipeline_stage VARCHAR(30) NOT NULL DEFAULT 'SOURCED',
    tags VARCHAR(500),
    created_at DATETIME NOT NULL,
    updated_at DATETIME,
    INDEX idx_candidate_name (first_name, last_name),
    INDEX idx_candidate_stage (pipeline_stage),
    INDEX idx_candidate_location (location)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Candidate skills
CREATE TABLE candidate_skills (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    candidate_id BIGINT NOT NULL,
    skill_name VARCHAR(100) NOT NULL,
    years_experience INT,
    proficiency VARCHAR(20),
    INDEX idx_cskill_name (skill_name),
    INDEX idx_cskill_candidate (candidate_id),
    FOREIGN KEY (candidate_id) REFERENCES candidate_profiles(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Job profiles
CREATE TABLE job_profiles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    department VARCHAR(200),
    location VARCHAR(200),
    description VARCHAR(2000),
    min_years_experience INT,
    education_requirement VARCHAR(100),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME NOT NULL,
    updated_at DATETIME
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Job profile criteria (skills with weights)
CREATE TABLE job_profile_criteria (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    job_profile_id BIGINT NOT NULL,
    skill_name VARCHAR(100) NOT NULL,
    min_years INT,
    criterion_type VARCHAR(20) NOT NULL DEFAULT 'REQUIRED',
    weight DOUBLE NOT NULL DEFAULT 1.0,
    FOREIGN KEY (job_profile_id) REFERENCES job_profiles(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Talent pools
CREATE TABLE talent_pools (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    description VARCHAR(500),
    created_by BIGINT NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME,
    FOREIGN KEY (created_by) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Talent pool candidates (join table)
CREATE TABLE talent_pool_candidates (
    pool_id BIGINT NOT NULL,
    candidate_id BIGINT NOT NULL,
    PRIMARY KEY (pool_id, candidate_id),
    FOREIGN KEY (pool_id) REFERENCES talent_pools(id) ON DELETE CASCADE,
    FOREIGN KEY (candidate_id) REFERENCES candidate_profiles(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Saved searches
CREATE TABLE saved_searches (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    created_by BIGINT NOT NULL,
    search_criteria_json TEXT NOT NULL,
    created_at DATETIME NOT NULL,
    FOREIGN KEY (created_by) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Search snapshots (frozen results with rationale)
CREATE TABLE search_snapshots (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    created_by BIGINT NOT NULL,
    search_criteria_json TEXT NOT NULL,
    job_profile_id BIGINT,
    created_at DATETIME NOT NULL,
    FOREIGN KEY (created_by) REFERENCES users(id),
    FOREIGN KEY (job_profile_id) REFERENCES job_profiles(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Snapshot results (frozen per-candidate match data)
CREATE TABLE search_snapshot_results (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    snapshot_id BIGINT NOT NULL,
    candidate_id BIGINT NOT NULL,
    candidate_name VARCHAR(200) NOT NULL,
    match_score DOUBLE NOT NULL,
    match_rationale TEXT NOT NULL,
    candidate_snapshot_json TEXT,
    FOREIGN KEY (snapshot_id) REFERENCES search_snapshots(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Pipeline stage transitions (for batch move + undo)
CREATE TABLE pipeline_stage_transitions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    candidate_id BIGINT NOT NULL,
    from_stage VARCHAR(30) NOT NULL,
    to_stage VARCHAR(30) NOT NULL,
    batch_id VARCHAR(36) NOT NULL,
    performed_by BIGINT NOT NULL,
    undone BOOLEAN NOT NULL DEFAULT FALSE,
    undo_deadline DATETIME NOT NULL,
    created_at DATETIME NOT NULL,
    INDEX idx_transition_batch (batch_id),
    INDEX idx_transition_undo (undone, undo_deadline),
    FOREIGN KEY (candidate_id) REFERENCES candidate_profiles(id),
    FOREIGN KEY (performed_by) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
