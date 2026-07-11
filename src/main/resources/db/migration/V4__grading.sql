-- Фаза 3: ручная проверка учителем (grading).
-- reviews.submission_id/problem_id/student_id/reviewer_id — межмодульные ссылки по id, без FK.
CREATE TABLE reviews (
    id            UUID         PRIMARY KEY,
    submission_id UUID         NOT NULL,   -- submissions.id; без FK — межмодульно
    problem_id    UUID         NOT NULL,   -- problems.id; денормализовано из решения
    student_id    UUID         NOT NULL,   -- users.id (автор решения); без FK
    reviewer_id   UUID         NOT NULL,   -- users.id (учитель); без FK
    status        VARCHAR(32)  NOT NULL,
    score         INT,                     -- 0..100; NULL = без числовой оценки
    feedback      TEXT,                    -- markdown-комментарий учителя
    published_at  TIMESTAMPTZ,             -- момент первой публикации; NULL пока DRAFT
    created_at    TIMESTAMPTZ  NOT NULL,
    updated_at    TIMESTAMPTZ  NOT NULL,
    CONSTRAINT reviews_submission_uk UNIQUE (submission_id),
    CONSTRAINT reviews_status_check  CHECK (status IN ('DRAFT', 'PUBLISHED')),
    CONSTRAINT reviews_score_check   CHECK (score IS NULL OR (score BETWEEN 0 AND 100))
);

CREATE INDEX ix_reviews_problem ON reviews (problem_id);
CREATE INDEX ix_reviews_student ON reviews (student_id);
