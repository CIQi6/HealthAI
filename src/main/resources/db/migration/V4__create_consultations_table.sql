CREATE TABLE consultations (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    doctor_id BIGINT NULL,
    symptom_description TEXT NOT NULL,
    ai_diagnosis TEXT NULL,
    doctor_opinion TEXT NULL,
    status ENUM('DRAFT','AI_REVIEWED','DOCTOR_REVIEWED','CLOSED','REJECTED','FAILED','CANCELLED') NOT NULL DEFAULT 'DRAFT',
    ai_model VARCHAR(128) NULL,
    ai_latency_ms INT NULL,
    ai_error_code VARCHAR(64) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    closed_at TIMESTAMP NULL,
    CONSTRAINT fk_consultations_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE RESTRICT,
    CONSTRAINT fk_consultations_doctor FOREIGN KEY (doctor_id) REFERENCES users (id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_consultations_user_status ON consultations (user_id, status);
CREATE INDEX idx_consultations_doctor_status ON consultations (doctor_id, status);
CREATE INDEX idx_consultations_created_at ON consultations (created_at DESC);
