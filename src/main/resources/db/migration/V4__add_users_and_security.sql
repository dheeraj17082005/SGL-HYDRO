-- Sabarmati Gas Limited (SGL) Smart CNG Station Database Schema V4
-- Create users table for authentication and RBAC

CREATE TABLE IF NOT EXISTS sgl_users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_user_username ON sgl_users(username);
