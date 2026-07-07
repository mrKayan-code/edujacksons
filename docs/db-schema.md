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
- `assignments(id, group_id, problem_id | material_id, deadline, ...)` — задачи/дедлайны: **Фаза 2**.

### problems
- `problems(id, owner_id, title, statement, language, time_limit_ms, memory_limit_kb, ...)`
- `test_cases(id, problem_id, input, expected_output, is_sample, ...)`

### submissions
- `submissions(id, problem_id, student_id, source_code, language, status, verdict, score, created_at, ...)`
- `submission_results(id, submission_id, test_case_id, verdict, time_ms, memory_kb, ...)`

### grading
- `reviews(id, submission_id | assignment_id, reviewer_id, grade, feedback, created_at, ...)`

### ege
- `ege_variants(id, title, source, ...)`
- `ege_tasks(id, variant_id, task_number, statement, answer_format, correct_answer, ...)`
- `ege_attempts(id, student_id, variant_id, ...)`

> Детализируем колонки в соответствующих фазах.
