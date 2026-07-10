-- Фаза 1: модули courses и groups.
-- Соглашения проекта: UUID-ключи, created_at/updated_at на всех таблицах.
-- Меж-модульные ссылки (owner_id, student_id, course_id) — по id, без FK на чужой модуль.
-- Внутри модуля FK явные.

-- ── courses ──────────────────────────────────────────────────────────────────

CREATE TABLE courses (
    id          UUID         PRIMARY KEY,
    owner_id    UUID         NOT NULL,   -- users.id (учитель); без FK — межмодульно
    title       VARCHAR(200) NOT NULL,
    description TEXT,
    created_at  TIMESTAMPTZ  NOT NULL,
    updated_at  TIMESTAMPTZ  NOT NULL
);

CREATE INDEX ix_courses_owner ON courses (owner_id);

CREATE TABLE materials (
    id          UUID         PRIMARY KEY,
    course_id   UUID         NOT NULL REFERENCES courses (id) ON DELETE CASCADE,
    type        VARCHAR(32)  NOT NULL,
    title       VARCHAR(200) NOT NULL,
    body        TEXT         NOT NULL,   -- markdown; слайды разделяются '---'
    order_index INT          NOT NULL,
    created_at  TIMESTAMPTZ  NOT NULL,
    updated_at  TIMESTAMPTZ  NOT NULL,
    CONSTRAINT materials_type_check CHECK (type IN ('LECTURE'))
);

CREATE INDEX ix_materials_course ON materials (course_id);

-- ── groups ───────────────────────────────────────────────────────────────────
-- Имя таблицы groups в PostgreSQL допустимо (non-reserved).

CREATE TABLE groups (
    id         UUID         PRIMARY KEY,
    owner_id   UUID         NOT NULL,    -- users.id (учитель); без FK — межмодульно
    title      VARCHAR(200) NOT NULL,
    created_at TIMESTAMPTZ  NOT NULL,
    updated_at TIMESTAMPTZ  NOT NULL
);

CREATE INDEX ix_groups_owner ON groups (owner_id);

-- Участники группы. Суррогатный id (ради BaseEntity + created_at/updated_at везде),
-- пара (group_id, student_id) уникальна. created_at = момент присоединения.
CREATE TABLE group_members (
    id         UUID        PRIMARY KEY,
    group_id   UUID        NOT NULL REFERENCES groups (id) ON DELETE CASCADE,
    student_id UUID        NOT NULL,     -- users.id (ученик); без FK — межмодульно
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT ux_group_members_group_student UNIQUE (group_id, student_id)
);

CREATE INDEX ix_group_members_student ON group_members (student_id);

-- Привязка курса к группе («назначение» Фазы 1). created_at = момент назначения.
CREATE TABLE group_courses (
    id         UUID        PRIMARY KEY,
    group_id   UUID        NOT NULL REFERENCES groups (id) ON DELETE CASCADE,
    course_id  UUID        NOT NULL,     -- courses.id; без FK — межмодульно
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT ux_group_courses_group_course UNIQUE (group_id, course_id)
);

CREATE INDEX ix_group_courses_course ON group_courses (course_id);
