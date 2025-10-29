CREATE TABLE prompt_templates (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    code VARCHAR(128) NOT NULL,
    channel ENUM('OLLAMA','HTTP_API') NOT NULL,
    model_name VARCHAR(128) NOT NULL,
    language VARCHAR(32) NOT NULL DEFAULT 'zh-CN',
    version VARCHAR(32) NOT NULL,
    description VARCHAR(255) NULL,
    content TEXT NOT NULL,
    variables JSON NULL,
    enabled TINYINT(1) NOT NULL DEFAULT 1,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE UNIQUE INDEX uniq_prompt_templates_code_version ON prompt_templates (code, version);
CREATE INDEX idx_prompt_templates_channel ON prompt_templates (channel);
