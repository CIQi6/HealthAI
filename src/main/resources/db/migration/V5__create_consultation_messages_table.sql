CREATE TABLE consultation_messages (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    consultation_id BIGINT NOT NULL,
    role ENUM('PATIENT','AI','DOCTOR','SYSTEM') NOT NULL,
    sequence_no INT NOT NULL,
    content TEXT NOT NULL,
    token_usage INT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_consultation_messages_consultation FOREIGN KEY (consultation_id) REFERENCES consultations (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_consultation_messages_consultation_seq ON consultation_messages (consultation_id, sequence_no);
