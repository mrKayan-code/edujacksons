# Схема БД (PostgreSQL)

> Заполняется по фазам. Все изменения — через миграции Flyway (`V<n>__<desc>.sql`).
> Здесь — договорённости и обзор, не дубль SQL.

## Принципы
- UUID как первичные ключи (проще при будущем распиле на сервисы).
- Явные внешние ключи внутри модуля; между модулями — по возможности только по id, без жёстких FK.
- `created_at` / `updated_at` на всех таблицах.

## Обзор таблиц (черновик, уточняется по фазам)

### auth ✅ реализовано (Фаза 0, `V1__init_auth.sql`)
- `users(id UUID pk, email unique, password_hash, role ∈ {TEACHER,STUDENT}, display_name, created_at, updated_at)`
- Email хранится нормализованным (lower-case), уникальный индекс `ux_users_email`.
- Пароли — BCrypt. Роль — `varchar(32)` + CHECK-constraint.

### courses ✅ реализовано (Фаза 1, `V2__courses_and_groups.sql`)
- `courses(id UUID pk, owner_id UUID, title varchar(200), description text, created_at, updated_at)`
  - `owner_id` → `users.id` (учитель) **без FK** — межмодульно, ссылка по id. Индекс `ix_courses_owner`.
- `materials(id UUID pk, course_id UUID → courses(id) ON DELETE CASCADE, type varchar(32),
  title varchar(200), body text, order_index int, created_at, updated_at)`
  - `type` — CHECK `IN ('LECTURE')`, enum расширяемый (позже FILE/VIDEO/LINK). Индекс `ix_materials_course`.
  - `body` — markdown; слайды разделяются `---`, диаграммы — ```mermaid, допустим inline HTML/CSS.

### groups ✅ реализовано (Фаза 1, `V2__courses_and_groups.sql`)
- `groups(id UUID pk, owner_id UUID, title varchar(200), created_at, updated_at)`
  - `owner_id` → `users.id` без FK. Имя таблицы `groups` в PostgreSQL допустимо (non-reserved).
- `group_members(id UUID pk, group_id UUID → groups(id) ON DELETE CASCADE, student_id UUID,
  created_at, updated_at)` — UNIQUE `(group_id, student_id)`; `student_id` → `users.id` без FK.
  Суррогатный id (ради `BaseEntity` + `created_at`/`updated_at` везде); `created_at` = момент вступления.
- `group_courses(id UUID pk, group_id UUID → groups(id) ON DELETE CASCADE, course_id UUID,
  created_at, updated_at)` — UNIQUE `(group_id, course_id)`; `course_id` → `courses.id` без FK.
  Привязка курса к группе («назначение» Фазы 1). `created_at` = момент назначения.
- `assignments(id, group_id, problem_id | material_id, deadline, ...)` — дедлайны/назначения: **позже**
  (в Фазе 2 не понадобилось: доступ к задаче = доступ к её курсу, уже назначенному группе в Фазе 1).

### problems ✅ реализовано (Фаза 2, `V3__problems_and_submissions.sql`)
- `problems(id UUID pk, owner_id UUID, course_id UUID, title varchar(200), statement text,
  language varchar(32), time_limit_ms int, memory_limit_kb int, created_at, updated_at)`
  - `owner_id` → `users.id` (учитель), `course_id` → `courses.id` — оба **без FK** (межмодульно).
    Доступ ученика к задаче сводится к доступу к курсу (`CourseAccessQuery`). Индексы
    `ix_problems_owner`, `ix_problems_course`. `language` — CHECK `IN ('PYTHON')` (task.md §9).
- `test_cases(id UUID pk, problem_id UUID → problems(id) ON DELETE CASCADE, input text,
  expected_output text, is_sample bool, order_index int, created_at, updated_at)`
  - `is_sample=TRUE` — открытый пример (виден ученику), иначе скрытый. Индекс `ix_test_cases_problem`.

### submissions ✅ реализовано (Фаза 2, `V3__problems_and_submissions.sql`)
- `submissions(id UUID pk, problem_id UUID, student_id UUID, language varchar(32), source_code text,
  status varchar(32), verdict varchar(32) null, score int null, total_tests int null, created_at, updated_at)`
  - `problem_id`/`student_id` — без FK (межмодульно). `status` CHECK `IN ('QUEUED','FINISHED','FAILED')`,
    `verdict` CHECK NULL или `IN (ACCEPTED, WRONG_ANSWER, TIME_LIMIT_EXCEEDED, RUNTIME_ERROR,
    COMPILE_ERROR, INTERNAL_ERROR)`. Индексы `ix_submissions_problem`, `ix_submissions_student`.
- `submission_results(id UUID pk, submission_id UUID → submissions(id) ON DELETE CASCADE,
  test_case_id UUID, verdict varchar(32), time_ms int null, memory_kb int null, order_index int, created_at, updated_at)`
  - `test_case_id` → `test_cases.id` (модуль problems), **без FK**. Вход/выход теста НЕ храним
    (приватность скрытых тестов + размер) — только вердикт и метрики. Индекс `ix_submission_results_submission`.

### grading
- `reviews(id, submission_id | assignment_id, reviewer_id, grade, feedback, created_at, ...)`

### ege
- `ege_variants(id, title, source, ...)`
- `ege_tasks(id, variant_id, task_number, statement, answer_format, correct_answer, ...)`
- `ege_attempts(id, student_id, variant_id, ...)`

> Детализируем колонки в соответствующих фазах.
