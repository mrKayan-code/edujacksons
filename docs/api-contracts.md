# API-контракты (REST)

> API-first: фронт и любой будущий клиент (в т.ч. мобилка) работают с этими контрактами.
> Заполняется по фазам. Стиль: JSON, версия под префиксом `/api/v1`.

## Общее
- Аутентификация: `Authorization: Bearer <JWT>`.
- Ошибки: единый формат `{ "error": { "code": ..., "message": ... } }`.
- Пагинация: `?page=&size=`.

## Черновик эндпоинтов по фазам

### Фаза 0 — auth
- `POST /api/v1/auth/register`
- `POST /api/v1/auth/login` → `{ accessToken, refreshToken }`
- `POST /api/v1/auth/refresh`
- `GET  /api/v1/me`

### Фаза 1 — courses / groups
- `POST /api/v1/courses`, `GET /api/v1/courses`, `GET /api/v1/courses/{id}`
- `POST /api/v1/courses/{id}/materials`
- `POST /api/v1/groups`, `POST /api/v1/groups/{id}/members`

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
