-- Модуль auth: пользователи платформы (учителя и ученики).
-- Соглашения проекта: UUID-ключи, created_at/updated_at на всех таблицах.

CREATE TABLE users (
    id            UUID         PRIMARY KEY,
    email         VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role          VARCHAR(32)  NOT NULL,
    display_name  VARCHAR(100) NOT NULL,
    created_at    TIMESTAMPTZ  NOT NULL,
    updated_at    TIMESTAMPTZ  NOT NULL,
    CONSTRAINT users_role_check CHECK (role IN ('TEACHER', 'STUDENT'))
);

-- Email уникален без учёта регистра (в приложении храним нормализованный lower-case).
CREATE UNIQUE INDEX ux_users_email ON users (email);
