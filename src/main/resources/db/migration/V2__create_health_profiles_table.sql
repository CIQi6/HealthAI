CREATE TABLE health_profiles (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    birth_date DATE NULL,
    blood_type ENUM('A','B','AB','O','UNKNOWN') DEFAULT 'UNKNOWN',
    chronic_diseases TEXT NULL,
    allergy_history TEXT NULL,
    genetic_risk TEXT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_health_profiles_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE UNIQUE INDEX uniq_health_profiles_user_id ON health_profiles (user_id);
