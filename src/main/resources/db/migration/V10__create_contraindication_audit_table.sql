CREATE TABLE contraindication_audit (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    prescription_id BIGINT NOT NULL,
    prescription_item_id BIGINT NULL,
    check_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    checker VARCHAR(32) NOT NULL,
    patient_snapshot JSON NULL,
    violations JSON NULL,
    result ENUM('PASS','WARN','FAIL') NOT NULL,
    message TEXT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_contra_audit_prescription FOREIGN KEY (prescription_id) REFERENCES prescriptions(id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_contra_audit_prescription_item FOREIGN KEY (prescription_item_id) REFERENCES prescription_items(id) ON DELETE SET NULL ON UPDATE CASCADE,
    KEY idx_contra_audit_prescription (prescription_id),
    KEY idx_contra_audit_result (result)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
