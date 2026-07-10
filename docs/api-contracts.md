# API-контракты (REST)

> API-first: фронт и любой будущий клиент (в т.ч. мобилка) работают с этими контрактами.
> Заполняется по фазам. Стиль: JSON, версия под префиксом `/api/v1` (в будущем).

## Общее
- Аутентификация: `Authorization: Bearer <JWT>`.
- Ошибки: единый формат `{ "error": { "code": ..., "message": ... } }`.
- Пагинация: `?page=&size=`.

## Черновик эндпоинтов по фазам

### Фаза 0 — auth
- `POST /api/auth/register`
- `POST /api/auth/login` → `{ accessToken, expiration, user }`
- `GET  /api/auth/me`
- `POST /api/auth/refresh` (планируется)

### Фаза 1 — courses / groups ✅ реализовано
> Путь без `/v1` — как у auth (`/api/...`); версионирование добавим позже единообразно.
> Роли: записи (POST/PATCH/DELETE) — только TEACHER-владелец; чтение курса — владелец или ученик
> с доступом через группу. Все group-эндпоинты — только TEACHER.

**courses**
- `POST   /api/courses` (TEACHER) — создать курс
- `GET    /api/courses` — TEACHER: свои; STUDENT: доступные через группы
- `GET    /api/courses/{id}` — курс + материалы (владелец или ученик с доступом)
- `PATCH  /api/courses/{id}` (TEACHER, владелец)
- `DELETE /api/courses/{id}` (TEACHER, владелец)
- `POST   /api/courses/{id}/materials` (TEACHER, владелец) — `{type,title,body,orderIndex?}`
- `GET    /api/courses/{id}/materials` — список (гейт доступа)
- `GET    /api/courses/{id}/materials/{materialId}` — одна лекция (слайд-вью)
- `PATCH  /api/courses/{id}/materials/{materialId}` (TEACHER, владелец)
- `DELETE /api/courses/{id}/materials/{materialId}` (TEACHER, владелец)

**groups** (все — TEACHER, owner-scoped)
- `POST   /api/groups` — создать группу
- `GET    /api/groups` — свои группы
- `GET    /api/groups/{id}` — детали: участники + привязанные курсы
- `POST   /api/groups/{id}/members` — `{email}` добавить ученика (по email зарегистрированного STUDENT)
- `DELETE /api/groups/{id}/members/{studentId}` — убрать ученика
- `POST   /api/groups/{id}/courses` — `{courseId}` привязать курс (идемпотентно)
- `DELETE /api/groups/{id}/courses/{courseId}` — отвязать курс

Коды ошибок (в поле `error`): `forbidden` (403), `course_not_found`/`group_not_found`/
`material_not_found`/`student_not_found` (404), `not_a_student`/`already_member` (409),
`validation_failed` (400).

### Фаза 2 — problems / submissions ✅ реализовано
> Путь без `/v1` — как у auth/courses (`/api/...`); версионирование добавим позже единообразно.
> Роли: записи задач (POST/PATCH/DELETE) — TEACHER-владелец курса; чтение задачи — владелец или
> ученик с доступом к курсу. Ученику в задаче видны только открытые (sample) тесты; скрытые — никогда.

**problems**
- `POST   /api/problems` (TEACHER) — `{courseId, title, statement, language, timeLimitMs, memoryLimitKb, tests?[]}`
  (курс должен принадлежать учителю; `tests[]` = `{input, expectedOutput, sample?, orderIndex?}`)
- `GET    /api/problems?courseId={id}` — задачи курса (гейт доступа к курсу)
- `GET    /api/problems/{id}` — условие + видимые тесты (владельцу все, ученику только sample)
- `PATCH  /api/problems/{id}` (TEACHER, владелец)
- `DELETE /api/problems/{id}` (TEACHER, владелец)
- `POST   /api/problems/{id}/tests` (TEACHER, владелец) — добавить тест `{input, expectedOutput, sample?, orderIndex?}`
- `GET    /api/problems/{id}/tests` (TEACHER, владелец) — все тесты
- `DELETE /api/problems/{id}/tests/{testId}` (TEACHER, владелец)

**submissions**
> Отправлять решение может любой аутентифицированный с доступом к задаче (гейт в сервисе через
> `ProblemDirectory`, владелец-учитель тоже может); чтение решения — автор или владелец задачи.
- `POST /api/problems/{id}/submissions` — `{language, sourceCode}`; проверка асинхронна →
  сразу `201` с `{ id, status: "QUEUED", ... }`
- `GET  /api/problems/{id}/submissions` — свои решения по задаче (история)
- `GET  /api/submissions/{id}` — статус/вердикт + результаты по тестам (автор или владелец задачи)

Статусы: `QUEUED → FINISHED | FAILED`. Вердикт (общий и по тесту): `ACCEPTED`, `WRONG_ANSWER`,
`TIME_LIMIT_EXCEEDED`, `RUNTIME_ERROR`, `COMPILE_ERROR`, `INTERNAL_ERROR`.
Коды ошибок: `problem_not_found`/`submission_not_found`/`test_case_not_found` (404),
`forbidden` (403), `invalid_language` (400), `validation_failed` (400).

### Фаза 3 — grading
- `POST /api/v1/submissions/{id}/review` (TEACHER)

### Фаза 4 — ege
- `GET  /api/v1/ege/variants`, `GET /api/v1/ege/variants/{id}`
- `POST /api/v1/ege/variants/{id}/attempts`
- `POST /api/v1/ege/attempts/{id}/answers` $\to$ автосверка
