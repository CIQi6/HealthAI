CREATE TABLE prescription_items (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    prescription_id BIGINT NOT NULL,
    medicine_id BIGINT NOT NULL,
    dosage_instruction VARCHAR(255) NOT NULL,
    frequency VARCHAR(64) NULL,
    day_supply SMALLINT NOT NULL,
    quantity DECIMAL(8,2) NULL,
    contra_result ENUM('PASS','WARN','FAIL') NOT NULL DEFAULT 'PASS',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uniq_prescription_item (prescription_id, medicine_id, dosage_instruction, frequency),
    CONSTRAINT fk_prescription_items_prescription FOREIGN KEY (prescription_id) REFERENCES prescriptions(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT fk_prescription_items_medicine FOREIGN KEY (medicine_id) REFERENCES medicines(id) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
