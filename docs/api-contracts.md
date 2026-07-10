# API-контракты (REST)

> API-first: фронт и любой будущий клиент (в т.ч. мобилка) работают с этими контрактами.
> Заполняется по фазам. Стиль: JSON, версия под префиксом `/api/v1`.

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

### Фаза 2 — problems / submissions
- `POST /api/v1/problems`, `GET /api/v1/problems/{id}`
- `POST /api/v1/problems/{id}/submissions` → `{ submissionId, status: "QUEUED" }`
- `GET  /api/v1/submissions/{id}` → статус + результаты по тестам

### Фаза 3 — grading
- `POST /api/v1/submissions/{id}/review` (TEACHER)

### Фаза 4 — ege
- `GET  /api/v1/ege/variants`, `GET /api/v1/ege/variants/{id}`
- `POST /api/v1/ege/variants/{id}/attempts`
- `POST /api/v1/ege/attempts/{id}/answers` → автосверка

> Точные схемы запросов/ответов детализируем в начале каждой фазы.
