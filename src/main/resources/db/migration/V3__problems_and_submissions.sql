-- Фаза 2: задачи по кодингу (problems), приём решений и автопроверка (submissions).
-- Соглашения проекта: UUID-ключи, created_at/updated_at на всех таблицах.
-- Меж-модульные ссылки (owner_id, course_id, student_id, problem_id, test_case_id) — по id, без FK.
-- FK — только внутри модуля.

-- ── problems ─────────────────────────────────────────────────────────────────

CREATE TABLE problems (
    id              UUID         PRIMARY KEY,
    owner_id        UUID         NOT NULL,   -- users.id (учитель); без FK — межмодульно
    course_id       UUID         NOT NULL,   -- courses.id; доступ ученика = доступ к курсу; без FK
    title           VARCHAR(200) NOT NULL,
    statement       TEXT         NOT NULL,   -- markdown-условие
    language        VARCHAR(32)  NOT NULL,   -- язык решения; на старте только PYTHON
    time_limit_ms   INT          NOT NULL,
    memory_limit_kb INT          NOT NULL,
    created_at      TIMESTAMPTZ  NOT NULL,
    updated_at      TIMESTAMPTZ  NOT NULL,
    CONSTRAINT problems_language_check CHECK (language IN ('PYTHON'))
);

CREATE INDEX ix_problems_owner  ON problems (owner_id);
CREATE INDEX ix_problems_course ON problems (course_id);

-- Набор тестов задачи. is_sample=TRUE — открытый пример (виден ученику в условии),
-- иначе скрытый тест. order_index — порядок показа/прогона.
CREATE TABLE test_cases (
    id              UUID        PRIMARY KEY,
    problem_id      UUID        NOT NULL REFERENCES problems (id) ON DELETE CASCADE,
    input           TEXT        NOT NULL,   -- stdin
    expected_output TEXT        NOT NULL,   -- ожидаемый stdout
    is_sample       BOOLEAN     NOT NULL DEFAULT FALSE,
    order_index     INT         NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL,
    updated_at      TIMESTAMPTZ NOT NULL
);

CREATE INDEX ix_test_cases_problem ON test_cases (problem_id);

-- ── submissions ────────────────────────────────────────────────────────────────

-- Решение ученика. status — жизненный цикл (QUEUED → FINISHED/FAILED),
-- verdict — итог проверки (NULL пока не проверено). score/total_tests — прошедшие/всего.
CREATE TABLE submissions (
    id          UUID         PRIMARY KEY,
    problem_id  UUID         NOT NULL,   -- problems.id; без FK — межмодульно
    student_id  UUID         NOT NULL,   -- users.id; без FK — межмодульно
    language    VARCHAR(32)  NOT NULL,
    source_code TEXT         NOT NULL,
    status      VARCHAR(32)  NOT NULL,
    verdict     VARCHAR(32),
    score       INT,
    total_tests INT,
    created_at  TIMESTAMPTZ  NOT NULL,
    updated_at  TIMESTAMPTZ  NOT NULL,
    CONSTRAINT submissions_status_check  CHECK (status IN ('QUEUED', 'FINISHED', 'FAILED')),
    CONSTRAINT submissions_verdict_check CHECK (verdict IS NULL OR verdict IN (
        'ACCEPTED', 'WRONG_ANSWER', 'TIME_LIMIT_EXCEEDED',
        'RUNTIME_ERROR', 'COMPILE_ERROR', 'INTERNAL_ERROR'))
);

CREATE INDEX ix_submissions_problem ON submissions (problem_id);
CREATE INDEX ix_submissions_student ON submissions (student_id);

-- Результат по одному тесту. test_case_id → test_cases.id (модуль problems), без FK — межмодульно.
-- Вход/выход теста тут НЕ храним (приватность скрытых тестов + размер): только вердикт и метрики.
CREATE TABLE submission_results (
    id            UUID        PRIMARY KEY,
    submission_id UUID        NOT NULL REFERENCES submissions (id) ON DELETE CASCADE,
    test_case_id  UUID        NOT NULL,   -- test_cases.id; без FK — межмодульно
    verdict       VARCHAR(32) NOT NULL,
    time_ms       INT,
    memory_kb     INT,
    order_index   INT         NOT NULL,
    created_at    TIMESTAMPTZ NOT NULL,
    updated_at    TIMESTAMPTZ NOT NULL,
    CONSTRAINT submission_results_verdict_check CHECK (verdict IN (
        'ACCEPTED', 'WRONG_ANSWER', 'TIME_LIMIT_EXCEEDED',
        'RUNTIME_ERROR', 'COMPILE_ERROR', 'INTERNAL_ERROR'))
);

CREATE INDEX ix_submission_results_submission ON submission_results (submission_id);
