-- HealthAi database schema
-- Generated on 2025-10-27 15:42 CST

CREATE TABLE IF NOT EXISTS `users` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `username` VARCHAR(64) NOT NULL,
    `password_hash` VARCHAR(255) NOT NULL,
    `full_name` VARCHAR(100) NOT NULL,
    `gender` ENUM('male','female','unknown') DEFAULT 'unknown',
    `phone` VARCHAR(32) UNIQUE,
    `email` VARCHAR(128) UNIQUE,
    `user_type` ENUM('patient','doctor','admin') NOT NULL,
    `registered_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `last_login_at` TIMESTAMP NULL DEFAULT NULL,
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY `uk_users_username` (`username`),
    KEY `idx_users_user_type` (`user_type`),
    KEY `idx_users_last_login_at` (`last_login_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `health_profiles` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL,
    `birth_date` DATE NULL,
    `blood_type` ENUM('A','B','AB','O','UNKNOWN') DEFAULT 'UNKNOWN',
    `chronic_diseases` TEXT NULL,
    `allergy_history` TEXT NULL,
    `genetic_risk` TEXT NULL,
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY `uk_health_profiles_user` (`user_id`),
    CONSTRAINT `fk_health_profiles_user` FOREIGN KEY (`user_id`) REFERENCES `users`(`id`) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `medicines` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `generic_name` VARCHAR(255) NOT NULL,
    `brand_name` VARCHAR(255) NULL,
    `indications` TEXT NULL,
    `contraindications` TEXT NULL,
    `dosage_guideline` TEXT NULL,
    `drug_interactions` TEXT NULL,
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY `idx_medicines_generic_name` (`generic_name`),
    KEY `idx_medicines_brand_name` (`brand_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `consultations` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL,
    `doctor_id` BIGINT NULL,
    `symptom_description` TEXT NOT NULL,
    `ai_diagnosis` TEXT NULL,
    `doctor_opinion` TEXT NULL,
    `status` ENUM('draft','ai_reviewed','doctor_reviewed','closed','rejected') DEFAULT 'draft',
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `closed_at` TIMESTAMP NULL DEFAULT NULL,
    KEY `idx_consult_user_status` (`user_id`,`status`),
    KEY `idx_consult_doctor_status` (`doctor_id`,`status`),
    CONSTRAINT `fk_consultations_user` FOREIGN KEY (`user_id`) REFERENCES `users`(`id`) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT `fk_consultations_doctor` FOREIGN KEY (`doctor_id`) REFERENCES `users`(`id`) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `vital_signs` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL,
    `measured_at` TIMESTAMP NOT NULL,
    `heart_rate` SMALLINT NULL,
    `systolic_bp` SMALLINT NULL,
    `diastolic_bp` SMALLINT NULL,
    `body_temperature` DECIMAL(4,1) NULL,
    `oxygen_saturation` TINYINT NULL,
    `weight` DECIMAL(5,2) NULL,
    `measurement_source` ENUM('manual','device','synced') DEFAULT 'manual',
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY `idx_vital_user_time` (`user_id`,`measured_at` DESC),
    CONSTRAINT `fk_vital_signs_user` FOREIGN KEY (`user_id`) REFERENCES `users`(`id`) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `prescriptions` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `consultation_id` BIGINT NOT NULL,
    `doctor_id` BIGINT NULL,
    `notes` TEXT NULL,
    `status` ENUM('draft','issued','cancelled') DEFAULT 'draft',
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT `fk_prescriptions_consultation` FOREIGN KEY (`consultation_id`) REFERENCES `consultations`(`id`) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT `fk_prescriptions_doctor` FOREIGN KEY (`doctor_id`) REFERENCES `users`(`id`) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `prescription_items` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `prescription_id` BIGINT NOT NULL,
    `medicine_id` BIGINT NOT NULL,
    `dosage_instruction` VARCHAR(255) NOT NULL,
    `frequency` VARCHAR(64) NULL,
    `day_supply` SMALLINT NOT NULL,
    `quantity` DECIMAL(6,2) NULL,
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY `uniq_prescription_item` (`prescription_id`,`medicine_id`,`dosage_instruction`,`frequency`),
    CONSTRAINT `fk_prescription_items_prescription` FOREIGN KEY (`prescription_id`) REFERENCES `prescriptions`(`id`) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT `fk_prescription_items_medicine` FOREIGN KEY (`medicine_id`) REFERENCES `medicines`(`id`) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
