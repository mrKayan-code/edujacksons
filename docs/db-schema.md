# Схема БД (PostgreSQL)

> Заполняется по фазам. Все изменения — через миграции Flyway (`V<n>__<desc>.sql`).
> Здесь — договорённости и обзор, не дубль SQL.

## Принципы
- UUID как первичные ключи (проще при будущем распиле на сервисы).
- Явные внешние ключи внутри модуля; между модулями — по возможности только по id, без жёстких FK.
- `created_at` / `updated_at` на всех таблицах.

## Обзор таблиц (черновик, уточняется по фазам)

### auth
- `users(id, email, password_hash, role, display_name, created_at, ...)`

### courses
- `courses(id, owner_id, title, description, ...)`
- `materials(id, course_id, type, title, body, order_index, ...)`

### groups
- `groups(id, owner_id, title, ...)`
- `group_members(group_id, student_id, joined_at)`
- `assignments(id, group_id, problem_id | material_id, deadline, ...)`

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
