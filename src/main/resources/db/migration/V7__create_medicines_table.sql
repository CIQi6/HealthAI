CREATE TABLE medicines (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    generic_name VARCHAR(255) NOT NULL,
    brand_name VARCHAR(255) NULL,
    indications TEXT NULL,
    contraindications JSON NULL,
    dosage_guideline JSON NULL,
    drug_interactions JSON NULL,
    tags JSON NULL,
    version INT NOT NULL DEFAULT 1,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uniq_medicines_generic_name (generic_name),
    KEY idx_medicines_brand_name (brand_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
