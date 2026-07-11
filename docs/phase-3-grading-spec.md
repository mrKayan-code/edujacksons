# Фаза 3 — `grading`: спецификация для реализации

> **Назначение файла.** Это исполнимая спецификация модуля ручной проверки (`grading`).
> Все развилки закрыты здесь — исполнителю ничего не додумывать. Обоснование ключевого
> решения — в `docs/adr/0007-grading-manual-review.md`. Источник правды по фазам — `task.md` §7.
>
> **Чеклист фазы (`task.md`):**
> - [ ] `grading`: воркфлоу проверки, оценки, фидбэк учителя (учитель ревьюит решение ученика)
> - [ ] Ученик видит статус и комментарии

---

## 0. Ключевая идея (прочитать первым)

Ручная проверка — **отдельная сущность `Review` в модуле `grading`**, ортогональная автопроверке
Judge0. Она **не** меняет таблицу `submissions` и **не** трогает её статусы. Одно решение
(`submission`) может иметь **0 или 1** проверку.

- Автопроверка (`SubmissionStatus`: `QUEUED → FINISHED/FAILED`) — жизненный цикл модуля `submissions`,
  Фаза 2. **Не трогаем.**
- Ручная проверка (`ReviewStatus`: `DRAFT → PUBLISHED`) — жизненный цикл модуля `grading`, Фаза 3.

Учитель может ревьюить **любое** существующее решение, независимо от его авто-вердикта
(в т.ч. `WRONG_ANSWER`, `TIME_LIMIT_EXCEEDED`, `FAILED`): ставит числовую оценку и/или текстовый
фидбэк по коду. Auto-вердикт и ручная оценка сосуществуют и не переопределяют друг друга.

Почему отдельная таблица, а не колонки в `submissions`, и почему проверка ортогональна автопроверке —
см. ADR `0007`.

---

## 1. Модель данных

### 1.1. Статусы и переходы

**`ReviewStatus`** (новый enum, модуль `grading`):

| Статус | Смысл | Виден ученику? |
|---|---|---|
| `DRAFT` | Учитель начал/сохранил проверку, но ещё не опубликовал | **Нет** |
| `PUBLISHED` | Проверка опубликована — оценка и фидбэк видны ученику | Да |

**Допустимые переходы** (единственный инициатор всегда — **TEACHER**-владелец задачи; система и
ученик статус не меняют):

| Из | В | Триггер | Побочный эффект |
|---|---|---|---|
| *(нет записи)* | `DRAFT` | `POST .../review` с `publish=false` | создаётся запись |
| *(нет записи)* | `PUBLISHED` | `POST .../review` с `publish=true` | создаётся запись, ставится `published_at` |
| `DRAFT` | `DRAFT` | `PATCH` (правка без публикации) | `updated_at` |
| `DRAFT` | `PUBLISHED` | `PATCH` c `publish=true` | ставится `published_at`, `updated_at` |
| `PUBLISHED` | `PUBLISHED` | `PATCH` (правка оценки/фидбэка **задним числом**) | `updated_at`; `published_at` **не меняется** |

Запрещено (для MVP; вернёт `409`/`400`): `PUBLISHED → DRAFT` (нельзя «спрятать» опубликованное),
удаление проверки, повторное `POST` при уже существующей проверке решения.

**Связь с `SubmissionStatus` (Фаза 2).** Никакой. Это разные сущности в разных модулях.
Проверять `submission.status` для создания ревью **не нужно** — ревью допустимо на решении в любом
авто-статусе. Ортогональность — сознательное решение (ADR `0007`).

**Производный статус проверки для ученика** (`grading` отдаёт наружу, в БД не хранится отдельно):
- `NOT_REVIEWED` — записи нет **или** есть только `DRAFT` (черновик ученику невидим);
- `REVIEWED` — существует `PUBLISHED`-проверка.

### 1.2. Сущность / таблица

Отдельная сущность, **не расширение `Submission`** (ADR `0007`, границы модулей — ADR `0004`).

**Entity `Review`** (`dev.edujacksons.grading.domain.Review`), таблица `reviews`.
Наследует `common.domain.BaseEntity` (`id`, `created_at`, `updated_at` — как во всём проекте).

| Поле | Тип (Java) | Колонка | Null | Смысл |
|---|---|---|---|---|
| `id` | `UUID` | `id` (PK) | нет | из `BaseEntity` |
| `submissionId` | `UUID` | `submission_id` | нет | ссылка на `submissions.id`, **без FK** (межмодульно). `UNIQUE` → 0..1 проверка на решение |
| `problemId` | `UUID` | `problem_id` | нет | ссылка на `problems.id`. Денормализовано из решения при создании (через `SubmissionDirectory`) — для выборок «журнал по задаче» и гейта без повторных обращений к порту |
| `studentId` | `UUID` | `student_id` | нет | автор решения (`users.id`). Для выборок «мои оценки» ученика и гейта чтения |
| `reviewerId` | `UUID` | `reviewer_id` | нет | учитель, поставивший/правивший оценку (`users.id`) |
| `status` | `ReviewStatus` | `status` | нет | `DRAFT` / `PUBLISHED` (`EnumType.STRING`) |
| `score` | `Integer` | `score` | **да** | Числовая оценка `0..100` (проценты). `NULL` = проверка без числовой оценки (только фидбэк) |
| `feedback` | `String` | `feedback` | **да** | Текстовый комментарий учителя (markdown) |
| `publishedAt` | `OffsetDateTime` | `published_at` | **да** | Момент первой публикации. `NULL` пока `DRAFT` |
| `createdAt` / `updatedAt` | — | — | нет | из `BaseEntity` |

Ссылки на другие модули — **только по id, без FK** (ADR `0004`). FK внутри `grading` нет (сущность одна).

### 1.3. Миграция `V4__grading.sql`

Стиль — как `V3` (UUID-ключи, `created_at`/`updated_at`, межмодульные ссылки без FK, CHECK на enum'ы):

```sql
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
```

`UNIQUE (submission_id)` уже даёт индекс по `submission_id` — отдельный не нужен.

---

## 2. Контракт модуля

### 2.1. Структура пакетов (как в остальных модулях: `api / domain / repository / service`)

```
grading/
  api/
    GradingController.java
    dto/  CreateReviewRequest, UpdateReviewRequest,
          ReviewResponse, StudentReviewResponse, GradebookEntryResponse
  domain/       Review, ReviewStatus
  repository/   ReviewRepository
  service/      GradingService, ReviewNotFoundException,
                ReviewAlreadyExistsException, ReviewNotPublishableException
```

### 2.2. DTO

**`CreateReviewRequest`** (body для `POST`):
```
{ "score": 0..100 | null, "feedback": string | null, "publish": boolean }   // publish по умолчанию false
```
**`UpdateReviewRequest`** (body для `PATCH`, patch-семантика — присланные поля перезаписывают):
```
{ "score": 0..100 | null, "feedback": string | null, "publish": boolean | null }
```
Валидация публикации: перейти/создаться в `PUBLISHED` можно только если после применения полей
есть **хотя бы одно** из `score`/`feedback` (не `null` и `feedback` не пустой) — иначе
`review_not_publishable` (400). Публиковать пустую проверку нельзя.

**`ReviewResponse`** (для TEACHER — полная):
```
{ id, submissionId, problemId, studentId, reviewerId, status, score, feedback,
  publishedAt, createdAt, updatedAt }
```
**`StudentReviewResponse`** (для STUDENT — усечённая, только опубликованное):
```
{ submissionId, score, feedback, publishedAt }
```
**`GradebookEntryResponse`** (строка учительского журнала по задаче — решение + его проверка):
```
{ submissionId, studentId, submissionStatus, verdict, submittedAt,
  reviewStatus: "NOT_REVIEWED"|"DRAFT"|"PUBLISHED", reviewId | null, score | null }
```

### 2.3. Эндпоинты

Пути — **без `/v1`**, `/api/...` (как auth/courses/problems/submissions; см. §5 про рассинхрон
в `api-contracts.md`). Аутентификация: `Authorization: Bearer <JWT>`. Все ошибки — единый формат
`{ "error": { "code", "message" } }` (через `GlobalExceptionHandler`).

| # | Метод | Путь | Роль | Запрос | Ответ | Гейт доступа |
|---|---|---|---|---|---|---|
| 1 | `POST` | `/api/submissions/{submissionId}/review` | TEACHER | `CreateReviewRequest` | `201` `ReviewResponse` | `problemDirectory.ownsProblem(actor, submission.problemId)` |
| 2 | `PATCH` | `/api/reviews/{reviewId}` | TEACHER | `UpdateReviewRequest` | `200` `ReviewResponse` | владелец задачи проверяемого решения |
| 3 | `GET` | `/api/reviews/{reviewId}` | TEACHER / STUDENT | — | `200` `ReviewResponse` (учитель) / `StudentReviewResponse` (ученик) | учитель-владелец **или** ученик-автор (ученику — только если `PUBLISHED`, иначе `404`) |
| 4 | `GET` | `/api/submissions/{submissionId}/review` | TEACHER / STUDENT | — | `200` `ReviewResponse` / `StudentReviewResponse` | как #3; удобно фронту, у которого есть `submissionId`. Нет записи (или ученику — `DRAFT`) → `404 review_not_found` |
| 5 | `GET` | `/api/problems/{problemId}/gradebook` | TEACHER | `?reviewed={true|false}` (опц.) | `200` `List<GradebookEntryResponse>` | `ownsProblem(actor, problemId)` |
| 6 | `GET` | `/api/problems/{problemId}/reviews/mine` | STUDENT | — | `200` `List<StudentReviewResponse>` | автор; только `PUBLISHED`, по своим решениям задачи |

Заметки по эндпоинтам:
- **#1**: если у решения уже есть проверка → `409 review_exists` (правки — через `PATCH`).
  `problemId`/`studentId` в `Review` заполняются из `SubmissionView` (не из тела запроса).
- **#3/#4 полиморфизм ответа**: тип DTO выбирается по роли вызывающего, не по параметру запроса.
  Ученик видит `StudentReviewResponse` и только когда `status=PUBLISHED`; черновик для ученика —
  как отсутствующий (`404`), чтобы не «светить» недоделанную оценку.
- **#5 журнал (очередь проверки)**: строится **внутри `grading`** — `SubmissionDirectory.listByProblem`
  даёт все решения всех учеников по задаче, к ним in-memory подшиваются проверки из таблицы `reviews`.
  `?reviewed=false` → только строки с `reviewStatus != PUBLISHED` (очередь «на проверку»);
  `?reviewed=true` → только `PUBLISHED`; без параметра — все. Сортировка: по времени решения, новые сверху.
- **Ролевой гейт** реализуем как в проекте: проверка прав — в `GradingService` через порты
  (`ProblemDirectory.ownsProblem`, сверка `review.studentId`/`submission.studentId` с `actorId`),
  а не только аннотациями. `@AuthenticationPrincipal UUID userId` — как в `SubmissionController`.

### 2.4. Коды ошибок

Добавить в `api-contracts.md` (Фаза 3):

| Код | HTTP | Когда |
|---|---|---|
| `review_not_found` | 404 | нет проверки (или ученик запросил `DRAFT`) |
| `submission_not_found` | 404 | `submissionId` не существует (через `SubmissionDirectory`) |
| `review_exists` | 409 | `POST` при уже существующей проверке решения |
| `review_not_publishable` | 400 | публикация без `score` и без `feedback` |
| `forbidden` | 403 | не владелец задачи / не автор решения |
| `validation_failed` | 400 | `score` вне `0..100` и пр. валидация полей |

---

## 3. Граничные случаи (все решены явно)

1. **Повторная отправка решения после того, как предыдущее уже оценено.**
   Каждая отправка — **новая** строка `submissions` (Фаза 2, без изменений). Проверка привязана к
   **конкретному** решению (`submission_id`). Новое решение приходит **без** проверки
   (`NOT_REVIEWED`), старое сохраняет свою `PUBLISHED`-проверку как исторический факт. Оценка «задним
   числом» старого решения не аннулируется и не переносится. Учитель видит новое непроверенное
   решение в журнале (#5, `?reviewed=false`). **Автопереноса/аннулирования нет.**

2. **Лимит попыток / история версий.**
   Лимита попыток `grading` **не вводит** (это тема дедлайнов/групп — Фаза 5). Отправки не
   ограничены. «История версий» решения = история `submissions` (уже есть, Фаза 2): каждая попытка —
   отдельная строка, каждую можно проверить независимо. Версионирования *самой проверки* (истории
   правок оценки) в MVP **нет** — правка перезаписывает поля in-place (см. п.3). Аудит правок —
   будущая фаза (`docs/observations.md`).

3. **Может ли учитель изменить оценку задним числом.**
   **Да.** `PATCH /api/reviews/{id}` (#2) правит `score`/`feedback` даже у `PUBLISHED`-проверки
   (переход `PUBLISHED → PUBLISHED`). `updated_at` обновляется, `published_at` остаётся прежним.
   История прежних значений не хранится (осознанная простота MVP; модель — один доверенный репетитор).
   Правит любой TEACHER-владелец задачи; `reviewer_id` перезаписывается на актуального правщика.

4. **Что видит ученик, пока проверка не завершена.**
   - Нет записи **или** только `DRAFT` → производный статус `NOT_REVIEWED`; эндпоинты #3/#4 отдают
     ученику `404 review_not_found` (черновик невидим). Фронт трактует `404` как «ожидает проверки
     учителя».
   - Есть `PUBLISHED` → `StudentReviewResponse` со `score`/`feedback`/`publishedAt`.
   - Авто-результат (вердикт/тесты Фазы 2) ученик видит **независимо** через существующий
     `GET /api/submissions/{id}` — он не затрагивается grading.

5. **Ревью на решении с авто-статусом `QUEUED`/`FAILED` или вердиктом `WRONG_ANSWER`.**
   Разрешено. `grading` не смотрит на `SubmissionStatus`/`Verdict`. Учитель вправе оценить/прокомментировать
   любое решение (например, дать частичный балл несмотря на авто-`WRONG_ANSWER`).

6. **Удаление решения/задачи.** В MVP не поддержано (нет эндпоинтов удаления с каскадом на другой
   модуль). FK между модулями нет, поэтому осиротевшая `review` теоретически возможна при будущем
   удалении — целостность на уровне приложения, как везде (ADR `0004`). Пометить в `observations.md`.

---

## 4. Стыковка с существующими модулями (через порты, не таблицы — ADR `0004`)

Граф зависимостей остаётся **DAG**, `grading` — новый потребитель, его никто не импортирует:
`grading → {submissions, problems}`; `submissions → {problems, judge}`; `problems → courses → auth`.
Цикла нет (ни `submissions`, ни `problems` не зависят от `grading`).

### 4.1. Новый порт в `submissions` (провайдер-сторона)

`submissions` сейчас **не** имеет публичного интерфейса — добавляем **аддитивно** (это часть работ
Фазы 3). Порт живёт у провайдера (`submissions`), реализуется **тонким бин-адаптером над
репозиторием** (не `SubmissionService`) — чтобы граф бинов остался ацикличным (ADR `0004` п.2).

```java
// dev.edujacksons.submissions.service
public interface SubmissionDirectory {
    Optional<SubmissionView> find(UUID submissionId);
    List<SubmissionView> listByProblem(UUID problemId);   // все решения всех учеников задачи, новые сверху
}

public record SubmissionView(
    UUID id, UUID problemId, UUID studentId,
    SubmissionStatus status, Verdict verdict, OffsetDateTime submittedAt) {}
```
- Реализация: `SubmissionDirectoryImpl` (`@Component`) над `SubmissionRepository` (+ новый метод
  `findByProblemIdOrderByCreatedAtDesc`). **Не** отдаёт наружу JPA-сущность `Submission` — только `SubmissionView`.
- `SubmissionView` не несёт `sourceCode` (журналу он не нужен; код учитель смотрит через
  `GET /api/submissions/{id}`, к которому у владельца задачи уже есть доступ по Фазе 2).

### 4.2. Существующий порт в `problems`

Переиспользуем `problems.service.ProblemDirectory.ownsProblem(userId, problemId)` — гейт TEACHER'а.
Ничего в `problems` менять не нужно.

### 4.3. Что модулю `grading` делать **нельзя**

- **Не** обогащать `GET /api/submissions/{id}` (модуль `submissions`) полем ревью — это создаст
  зависимость `submissions → grading` и **цикл**. Композиция авто-результата и ручной оценки —
  на стороне фронта (два запроса) или уже готовыми эндпоинтами `grading`.
- **Не** читать таблицы `submissions`/`problems` напрямую и **не** ставить FK на них.
- **Не** менять `submissions`/`SubmissionStatus`.

### 4.4. События (Kafka)

Фаза 3 — **синхронный REST, без Kafka.** Публикация ревью в будущем может эмитить событие
`ReviewPublished` для модуля `notifications` (Фаза 5). Сейчас **не реализуем** — только оставляем
точку расширения (упомянуть в README и `observations.md`).

---

## 5. Синхронизация документации

- `api-contracts.md`, черновик Фазы 3, указывал `POST /api/v1/submissions/{id}/review`. Это
  **устаревший** путь: все реализованные модули используют `/api/...` без `/v1`. Привести раздел
  Фазы 3 к эндпоинтам из §2.3 этого файла (уже сделано в рамках подготовки спеки).
- По завершении реализации: README модуля `grading` (пишет doc-агент), отметка `[x]` в `task.md` §7,
  запись новых точек риска в `docs/observations.md` (аудит правок оценки; осиротевшие ревью).

---

## 6. Определение готовности (Definition of Done)

- [ ] Миграция `V4__grading.sql` (таблица `reviews`).
- [ ] `Review`, `ReviewStatus`, `ReviewRepository`, `GradingService`, `GradingController`, DTO.
- [ ] Новый порт `SubmissionDirectory` + `SubmissionView` + `SubmissionDirectoryImpl` в `submissions`
      (+ метод репозитория `findByProblemIdOrderByCreatedAtDesc`).
- [ ] Гейты доступа (TEACHER-владелец / STUDENT-автор) в сервисе через порты.
- [ ] Коды ошибок и единый формат ответа.
- [ ] Сквозной интеграционный тест на Testcontainers (реальный PostgreSQL): ученик отправляет решение
      → учитель ставит и публикует оценку → ученик видит `PUBLISHED` → правка задним числом →
      повторная отправка → новое решение `NOT_REVIEWED` в журнале.
```
