-- Metric definitions (base and derived)
CREATE TABLE metric_definitions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    slug VARCHAR(100) NOT NULL,
    name VARCHAR(200) NOT NULL,
    description VARCHAR(1000),
    data_type VARCHAR(20) NOT NULL,
    aggregation_type VARCHAR(20) NOT NULL,
    source_table VARCHAR(100),
    source_column VARCHAR(100),
    filter_expression VARCHAR(500),
    unit VARCHAR(20),
    derived BOOLEAN NOT NULL DEFAULT FALSE,
    formula VARCHAR(500),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    owner_username VARCHAR(50),
    created_at DATETIME NOT NULL,
    updated_at DATETIME,
    CONSTRAINT uk_metric_slug UNIQUE (slug)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Dimension definitions
CREATE TABLE dimension_definitions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    slug VARCHAR(100) NOT NULL,
    name VARCHAR(200) NOT NULL,
    description VARCHAR(1000),
    source_table VARCHAR(100),
    source_column VARCHAR(100),
    hierarchy_level VARCHAR(50),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME NOT NULL,
    updated_at DATETIME,
    CONSTRAINT uk_dim_slug UNIQUE (slug)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Metric versions (full versioning with publish governance)
CREATE TABLE metric_versions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    metric_id BIGINT NOT NULL,
    version_number INT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    definition_snapshot TEXT NOT NULL,
    change_description VARCHAR(1000),
    published_by VARCHAR(50),
    published_at DATETIME,
    created_at DATETIME NOT NULL,
    CONSTRAINT uk_metric_version UNIQUE (metric_id, version_number),
    INDEX idx_mv_metric (metric_id),
    INDEX idx_mv_status (status),
    FOREIGN KEY (metric_id) REFERENCES metric_definitions(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Metric-dimension links (which dimensions apply to which metrics)
CREATE TABLE metric_dimension_links (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    metric_id BIGINT NOT NULL,
    dimension_id BIGINT NOT NULL,
    join_expression VARCHAR(200),
    CONSTRAINT uk_metric_dim UNIQUE (metric_id, dimension_id),
    FOREIGN KEY (metric_id) REFERENCES metric_definitions(id) ON DELETE CASCADE,
    FOREIGN KEY (dimension_id) REFERENCES dimension_definitions(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Metric window calculations (rolling/calendar windows)
CREATE TABLE metric_window_calculations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    metric_id BIGINT NOT NULL,
    window_type VARCHAR(30) NOT NULL,
    window_aggregation VARCHAR(20) NOT NULL,
    custom_window_days INT,
    label VARCHAR(200),
    FOREIGN KEY (metric_id) REFERENCES metric_definitions(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Metric lineage (derived-to-source relationships)
CREATE TABLE metric_lineage (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    derived_metric_id BIGINT NOT NULL,
    source_metric_id BIGINT NOT NULL,
    relationship_type VARCHAR(30) NOT NULL,
    contribution_description VARCHAR(500),
    created_at DATETIME NOT NULL,
    CONSTRAINT uk_lineage_pair UNIQUE (derived_metric_id, source_metric_id),
    INDEX idx_lineage_derived (derived_metric_id),
    INDEX idx_lineage_source (source_metric_id),
    FOREIGN KEY (derived_metric_id) REFERENCES metric_definitions(id) ON DELETE CASCADE,
    FOREIGN KEY (source_metric_id) REFERENCES metric_definitions(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
