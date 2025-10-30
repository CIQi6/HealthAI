CREATE TABLE prescriptions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    consultation_id BIGINT NOT NULL,
    patient_id BIGINT NOT NULL,
    doctor_id BIGINT NULL,
    status ENUM('DRAFT','ISSUED','CANCELLED') NOT NULL DEFAULT 'DRAFT',
    notes TEXT NULL,
    contra_check_status ENUM('PASS','WARN','FAIL') NOT NULL DEFAULT 'PASS',
    contra_fail_reason TEXT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_prescriptions_consultation FOREIGN KEY (consultation_id) REFERENCES consultations(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT fk_prescriptions_patient FOREIGN KEY (patient_id) REFERENCES users(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT fk_prescriptions_doctor FOREIGN KEY (doctor_id) REFERENCES users(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    KEY idx_prescriptions_consultation (consultation_id),
    KEY idx_prescriptions_patient (patient_id),
    KEY idx_prescriptions_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
