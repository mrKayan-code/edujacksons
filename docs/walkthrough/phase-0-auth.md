# Разбор кода — Фаза 0 (скелет + auth)

> Учебный разбор. Читай сверху вниз, параллельно открывая упомянутые файлы.
> Пути кликабельны из этого файла (relative). В конце — вопросы для самопроверки.

---

## 0. Как читать этот документ

Порядок разбора = порядок, в котором проще всего понять систему:
**данные → JWT → безопасность → бизнес-логика → web → сквозные сценарии**.

Это не порядок, в котором выполняется запрос. Поток запроса разберём отдельно в §7.

---

## 1. Большая картина: слои и поток

Приложение — **модульный монолит**. Один процесс Spring Boot, внутри — модули
(`auth`, `common`, дальше `courses`, `problems`...). Каждый модуль разложен на слои:

```
api/         ← контроллеры (HTTP) и DTO. Знают про web, не знают про БД.
service/     ← бизнес-логика. Публичная «дверь» модуля. Ничего не знает про HTTP.
repository/  ← доступ к БД (Spring Data JPA).
domain/      ← сущности (то, что лежит в таблицах) и enum'ы.
config/      ← конфигурация модуля (бины, безопасность).
```

Правило: **зависимости идут внутрь**. `api` → `service` → `repository` → `domain`.
Контроллер не лезет в репозиторий напрямую, а БД ничего не знает про HTTP. Это то, что
позже позволит вынести модуль в отдельный сервис почти без переписывания.

Поток HTTP-запроса (упрощённо):

```
HTTP-запрос
  → [цепочка фильтров Spring Security]   ← здесь проверяется JWT
  → DispatcherServlet
  → Controller (api/)                    ← валидирует вход, вызывает сервис
  → Service (service/)                   ← бизнес-логика, транзакция
  → Repository (repository/)             ← SQL через JPA
  → PostgreSQL
  ← ответ мапится в DTO и сериализуется в JSON
```

---

## 2. Точка входа и как Spring вообще «оживает»

Файл: [`EdujacksonsApplication.java`](../../src/main/java/dev/edujacksons/EdujacksonsApplication.java)

```java
@SpringBootApplication
@ConfigurationPropertiesScan
public class EdujacksonsApplication { public static void main(...) { SpringApplication.run(...); } }
```

Ключевые идеи, которые надо усвоить один раз и навсегда:

- **`@SpringBootApplication`** = «просканируй пакет `dev.edujacksons` и всё под ним, найди
  мои классы-компоненты и собери из них приложение». Он включает автоконфигурацию
  (Spring Boot сам поднимет веб-сервер, пул соединений к БД, Flyway и т.д. по тому, что
  лежит в classpath).
- **Bean (бин)** — объект, которым управляет Spring: создаёт его сам, хранит в единственном
  экземпляре (singleton) и «вкладывает» туда, где он нужен.
- **DI (Dependency Injection / внедрение зависимостей)** — ты не создаёшь объекты через
  `new`. Ты объявляешь в конструкторе, *что тебе нужно*, а Spring подставляет готовый бин.
  Смотри любой наш класс: `AuthService(UserRepository, PasswordEncoder, JwtService, ...)` —
  все четыре Spring создаст и передаст сам.
- **Как класс становится бином:** аннотации-стереотипы — `@Component`, `@Service`,
  `@RestController`, `@Configuration`, `@Repository`. Spring находит их при сканировании.
  (Интерфейсы Spring Data, как `UserRepository`, становятся бинами по-особому — см. §3.3.)
- **`@ConfigurationPropertiesScan`** — включает подхват классов настроек
  (`@ConfigurationProperties`), у нас это `JwtProperties` (§4.2).

Мысленная модель: **Spring — это фабрика объектов + телефонная книга**. При старте он строит
граф всех бинов и связей между ними. Дальше ты просто «просишь» нужное в конструкторе.

---

## 3. Слой данных

### 3.1 BaseEntity — общий предок всех таблиц

Файл: [`common/domain/BaseEntity.java`](../../src/main/java/dev/edujacksons/common/domain/BaseEntity.java)

```java
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity {
    @Id private UUID id;
    @CreatedDate  private Instant createdAt;
    @LastModifiedDate private Instant updatedAt;
    @PrePersist void ensureId() { if (id == null) id = UUID.randomUUID(); }
}
```

- **`@MappedSuperclass`** — «это не таблица, но мои поля/колонки наследуются каждой сущностью,
  которая меня расширяет». То есть `id`, `created_at`, `updated_at` появятся у `users` и у
  всех будущих таблиц — без копипасты.
- **`@Id`** — первичный ключ. У нас **UUID** (а не автоинкремент). Почему: при будущем распиле
  на микросервисы UUID можно генерировать на любой стороне без похода в БД и без коллизий.
- **`@PrePersist`** — колбэк JPA «прямо перед первым `INSERT`». Тут мы гарантируем, что у
  сущности есть id. Мы генерируем id **в приложении**, а не в БД.
- **Аудит:** `@CreatedDate` / `@LastModifiedDate` + слушатель `AuditingEntityListener`
  автоматически проставляют время создания и последнего изменения. Чтобы это заработало,
  аудит надо включить — это делает [`common/config/JpaConfig.java`](../../src/main/java/dev/edujacksons/common/config/JpaConfig.java)
  одной аннотацией `@EnableJpaAuditing`.

### 3.2 User — сущность пользователя

Файл: [`auth/domain/User.java`](../../src/main/java/dev/edujacksons/auth/domain/User.java)

```java
@Entity @Table(name = "users")
public class User extends BaseEntity {
    @Column(unique = true) private String email;
    @Column(name = "password_hash") private String passwordHash;
    @Enumerated(EnumType.STRING) private Role role;
    private String displayName;
}
```

- **`@Entity`** — «этот класс отображается на строку таблицы». **`@Table(name="users")`** —
  имя таблицы (иначе было бы `user`, а это зарезервированное слово в SQL).
- **`@Column`** — тонкая настройка колонки (имя, уникальность, `nullable`).
- **`@Enumerated(EnumType.STRING)`** — enum `Role` хранится **строкой** (`'TEACHER'`), а не
  числом. Строкой безопаснее: добавишь новую роль в середину — старые данные не «поедут».
- **Пароль тут — `passwordHash`, не `password`.** Мы **никогда** не храним пароль в открытом
  виде. В БД лежит BCrypt-хеш (см. §5.5).
- **Lombok:** `@Getter/@Setter/@NoArgsConstructor` — генерируют геттеры/сеттеры/пустой
  конструктор на этапе компиляции, чтобы не писать их руками. JPA требует пустой конструктор.

### 3.3 Repository — доступ к БД без единой строки SQL

Файл: [`auth/repository/UserRepository.java`](../../src/main/java/dev/edujacksons/auth/repository/UserRepository.java)

```java
public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
}
```

Магия Spring Data JPA:

- Ты пишешь **только интерфейс**, реализацию Spring генерирует сам в рантайме и регистрирует
  как бин. От `JpaRepository<User, UUID>` бесплатно приходят `save`, `findById`, `findAll`,
  `delete` и т.д. (`User` — тип сущности, `UUID` — тип ключа).
- **Derived queries (запросы по имени метода):** Spring парсит имя `findByEmail` и сам строит
  `SELECT ... WHERE email = ?`. `existsByEmail` → `SELECT EXISTS(...)`. Имя метода = запрос.
- **`Optional<User>`** — «может вернуться пусто». Это лучше, чем `null`: заставляет явно
  обработать «не найдено» (см. `.orElseThrow(...)` в сервисе).

### 3.4 Flyway — миграции = версии схемы БД

Файл: [`db/migration/V1__init_auth.sql`](../../src/main/resources/db/migration/V1__init_auth.sql)

```sql
CREATE TABLE users (
    id UUID PRIMARY KEY, email VARCHAR(255) NOT NULL, password_hash ...,
    role VARCHAR(32) NOT NULL, ...,
    CONSTRAINT users_role_check CHECK (role IN ('TEACHER','STUDENT'))
);
CREATE UNIQUE INDEX ux_users_email ON users (email);
```

- **Зачем Flyway:** схему БД нельзя менять «руками на проде». Flyway — это git для схемы.
  Каждое изменение = новый файл `V<номер>__<описание>.sql`. При старте Flyway смотрит, какие
  версии уже применены (в служебной таблице `flyway_schema_history`), и накатывает новые
  по порядку. У всех разработчиков и на проде — одинаковая схема.
- **Правило именования:** `V1__init_auth.sql`, `V2__...`. Два подчёркивания после версии.
  **Применённые миграции не редактируют** — только добавляют новые.
- **`CHECK`-constraint** дублирует enum на уровне БД: даже кривой `INSERT` мимо приложения
  не запишет невалидную роль. БД — последний рубеж целостности.

### 3.5 Кто владелец схемы: `ddl-auto=validate`

Файл: [`application.yml`](../../src/main/resources/application.yml) → `spring.jpa.hibernate.ddl-auto: validate`

Важнейшая настройка. Варианты бывают `create`, `update`, `validate`, `none`. Мы выбрали
**`validate`**: Hibernate при старте **не меняет** схему, а только **сверяет** свои
`@Entity` с реальными таблицами и падает, если они разошлись. Схемой владеет **только
Flyway**. Это промышленный подход: никакой «магии», которая тихо меняет прод-таблицы.

Порядок на старте: Spring Boot сначала гоняет **Flyway** (создаёт таблицы), потом поднимает
**JPA** (сверяет). Если забудешь миграцию — приложение честно не стартует.

---

## 4. JWT — как работает вход без сессий

### 4.1 Что такое JWT (концепция)

JWT (JSON Web Token) — это **самодостаточный пропуск**. Строка из трёх частей через точку:

```
header.payload.signature
```

- **payload** — JSON с «claims» (утверждениями): кто это (`sub` = id пользователя), какая
  роль, когда истекает (`exp`). Это НЕ шифрование — payload легко раскодировать (base64).
  Секрет в токен не кладём.
- **signature** — подпись payload нашим секретным ключом (алгоритм HS256). Смысл: клиент
  не может подделать/поменять payload, потому что не знает секрет. Изменил хоть байт —
  подпись не сойдётся.

Зачем это нужно: сервер **не хранит сессии**. Он не помнит, кто залогинен. Клиент на каждый
запрос присылает токен в заголовке `Authorization: Bearer <токен>`, сервер проверяет подпись
и срок — и всё, личность установлена. Это и есть **stateless**-аутентификация (масштабируется
на много инстансов без общего хранилища сессий).

### 4.2 Настройки токена

Файл: [`auth/config/JwtProperties.java`](../../src/main/java/dev/edujacksons/auth/config/JwtProperties.java)

```java
@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(String secret, Duration expiration, String issuer) {}
```

- **`@ConfigurationProperties(prefix="app.jwt")`** — Spring возьмёт из `application.yml`
  ветку `app.jwt.*` и разложит по полям записи. `record` — неизменяемый класс-контейнер
  (Java 17+), идеально для настроек.
- **Секрет — из окружения** (`${JWT_SECRET:...}`), дефолт только для локалки. На проде секрет
  в коде/конфиге — это дыра.

### 4.3 Выпуск и проверка токена

Файл: [`auth/service/JwtService.java`](../../src/main/java/dev/edujacksons/auth/service/JwtService.java)

- **`issueToken(user)`** — строит токен: `subject` = id пользователя, плюс claims `email` и
  `role`, `issuedAt` = сейчас, `expiration` = сейчас + срок, и `.signWith(key)` — подпись.
- **`parse(token)`** — проверяет подпись и срок (`verifyWith(key).parseSignedClaims`) и
  достаёт обратно `userId/email/role` в маленькую запись `ParsedToken`. Если подпись не сошлась
  или токен просрочен — библиотека бросает `JwtException`.
- Ключ создаётся один раз в конструкторе из секрета: `Keys.hmacShaKeyFor(secret.getBytes())`.
  Для HS256 секрет должен быть **≥ 32 байт** — поэтому дефолтный такой длинный.

Проверить понимание помогает тест
[`JwtServiceTest.java`](../../src/test/java/dev/edujacksons/auth/service/JwtServiceTest.java):
выпустили → распарсили → claims совпали; просроченный и «подкрученный» токены отвергаются.

---

## 5. Spring Security — самая мясная часть (ты её и открыл)

Общая идея Spring Security: **перед** твоими контроллерами стоит **цепочка фильтров**
(Filter Chain). Каждый фильтр что-то делает с запросом и передаёт дальше. Аутентификация и
авторизация происходят *здесь*, до того как запрос вообще дойдёт до контроллера.

Два разных слова, не путай:
- **Аутентификация** — «кто ты?» (проверка личности: пароль или токен).
- **Авторизация** — «что тебе можно?» (доступ к эндпоинту по роли/правам).

### 5.1 Сборка цепочки — SecurityConfig

Файл: [`auth/config/SecurityConfig.java`](../../src/main/java/dev/edujacksons/auth/config/SecurityConfig.java)

```java
@Bean SecurityFilterChain securityFilterChain(HttpSecurity http) {
    http.csrf(disable)
        .sessionManagement(STATELESS)
        .authorizeHttpRequests(auth -> auth
            .requestMatchers(POST, "/api/auth/register", "/api/auth/login").permitAll()
            .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
            .anyRequest().authenticated())
        .exceptionHandling(ex -> ex.authenticationEntryPoint(authenticationEntryPoint))
        .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
    return http.build();
}
```

Разбор по строчкам:

- **`@Bean SecurityFilterChain`** — мы *собираем* цепочку фильтров и отдаём её Spring. Метод
  с `@Bean` в `@Configuration`-классе = «вот тебе готовый объект-бин, положи в книгу».
- **`csrf disable`** — CSRF-защита нужна для форм с cookie-сессиями. У нас stateless API на
  токенах в заголовке — CSRF не применим, выключаем.
- **`STATELESS`** — «не создавай HTTP-сессию, не клади ничего в память между запросами».
  Личность держится только в токене. Прямое следствие §4.1.
- **`authorizeHttpRequests`** — правила авторизации, читаются **сверху вниз**, первое
  совпадение выигрывает:
  - регистрация и логин — `permitAll()` (иначе как бы ты вошёл без токена);
  - health-check — `permitAll()` (мониторинг не должен логиниться);
  - **`anyRequest().authenticated()`** — всё остальное требует валидного токена. Это «дефолт
    запрещено» — правильная политика (забыл открыть эндпоинт — он закрыт, а не наоборот).
- **`exceptionHandling(...entryPoint)`** — что делать, когда доступа нет. См. §5.4.
- **`addFilterBefore(jwtAuthenticationFilter, ...)`** — **вставляем наш JWT-фильтр** в цепочку
  перед стандартным. Порядок фильтров важен: наш должен успеть установить личность раньше,
  чем сработает проверка авторизации.

### 5.2 Наш фильтр — JwtAuthenticationFilter

Файл: [`auth/config/JwtAuthenticationFilter.java`](../../src/main/java/dev/edujacksons/auth/config/JwtAuthenticationFilter.java)

```java
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    protected void doFilterInternal(req, res, chain) {
        String header = req.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ") && контекст пуст) {
            ParsedToken parsed = jwtService.parse(header.substring(7));
            var auth = new UsernamePasswordAuthenticationToken(
                    parsed.userId(), null, [ROLE_<role>]);
            SecurityContextHolder.getContext().setAuthentication(auth);
        }
        chain.doFilter(req, res); // всегда пропускаем дальше
    }
}
```

- **`OncePerRequestFilter`** — базовый класс, гарантирующий, что фильтр отработает **один раз
  на запрос** (без него при внутренних форвардах мог бы срабатывать дважды).
- Логика: если есть заголовок `Bearer <token>` — распарсить, и если токен валиден — положить
  в **`SecurityContextHolder`** объект `Authentication`. Это «доска», куда security пишет
  «текущий пользователь». Дальше по цепочке правило `anyRequest().authenticated()` увидит, что
  личность установлена, и пропустит.
- **Principal у нас = `userId` (UUID).** Мы кладём id, а не весь объект User (не ходим в БД на
  каждый запрос — id и роль уже есть в токене). Отсюда в контроллере `@AuthenticationPrincipal
  UUID userId`.
- **Authority = `ROLE_<role>`** — Spring по соглашению ждёт префикс `ROLE_` для ролей.
- **Нет токена / токен битый → не бросаем ошибку тут**, просто не ставим личность и идём
  дальше. Решение «пускать или нет» примет слой авторизации; если эндпоинт защищён и личности
  нет — сработает entry point (§5.4) и вернёт 401.

### 5.3 UserDetailsService и AuthenticationManager — путь ЛОГИНА

Тут часто путаются. Есть **два разных пути** установления личности:

1. **Логин по паролю** (эндпоинт `/login`) — проверяем email+пароль против БД.
2. **Последующие запросы по токену** — проверяем JWT (это §5.2, БД не трогаем).

Для пути №1 используются:

- [`AppUserDetailsService.java`](../../src/main/java/dev/edujacksons/auth/service/AppUserDetailsService.java) —
  реализует интерфейс `UserDetailsService` с единственным методом `loadUserByUsername(email)`:
  достаёт `User` из БД и отдаёт Spring'у в его формате (`UserDetails`) с хешем пароля и ролью.
- **`AuthenticationManager`** (бин в `SecurityConfig`) — «менеджер проверки личности». Когда мы
  зовём `authenticationManager.authenticate(email, rawPassword)`, Spring под капотом:
  берёт `UserDetailsService` → грузит пользователя → сравнивает присланный пароль с хешем через
  `PasswordEncoder`. Совпало — ок; нет — бросает `BadCredentialsException`.
- Мы не пишем это сравнение руками — только предоставляем `UserDetailsService` и
  `PasswordEncoder` как бины, а `AuthenticationManager` собирает из них стандартный механизм
  (`DaoAuthenticationProvider`). Меньше своего кода = меньше багов в безопасности.

### 5.4 Единый 401 — RestAuthenticationEntryPoint

Файл: [`auth/config/RestAuthenticationEntryPoint.java`](../../src/main/java/dev/edujacksons/auth/config/RestAuthenticationEntryPoint.java)

Когда неаутентифицированный лезет в защищённый эндпоинт, Spring по умолчанию отдал бы
редирект на форму логина (нам не нужно — у нас API). Наш `AuthenticationEntryPoint` вместо
этого пишет аккуратный JSON `{status, error, message}` со статусом **401**. Подключается в
`exceptionHandling(...)` в §5.1.

### 5.5 PasswordEncoder — почему пароль нельзя «расшифровать»

Бин в `SecurityConfig`: `new BCryptPasswordEncoder()`.

- Пароль хешируется **односторонне**: из пароля можно получить хеш, из хеша пароль — нет.
- BCrypt внутри добавляет **соль** (случайную добавку) и намеренно **медленный** — чтобы
  перебор был дорогим. При регистрации: `encode(rawPassword)` → в БД. При логине сравнение
  делает `AuthenticationManager` через `matches(raw, hash)`.

---

## 6. Прикладной слой и web

### 6.1 AuthService — публичная дверь модуля

Файл: [`auth/service/AuthService.java`](../../src/main/java/dev/edujacksons/auth/service/AuthService.java)

- **`register`**: нормализует email (lower-case, trim) → проверяет `existsByEmail` (занят?
  бросаем `EmailAlreadyUsedException` → 409) → хеширует пароль → `save` → выпускает токен.
- **`login`**: зовёт `authenticationManager.authenticate(...)` (это и есть проверка пароля,
  §5.3) → грузит пользователя → выпускает токен. Неверный пароль → `BadCredentialsException`.
- **`getById`**: для `/me` — грузит пользователя по id из токена.
- **`@Transactional`** — оборачивает метод в транзакцию БД: либо все изменения применились,
  либо (при исключении) откатились целиком. `readOnly = true` на чтении — подсказка-оптимизация.
- Сервис **не знает про HTTP** и **не возвращает web-DTO**. Он отдаёт `AuthResult` (доменный
  результат: `User` + токен), а превращение в JSON-ответ — забота контроллера. Так слои
  остаются развязанными.

### 6.2 DTO, records и валидация

Папка: [`auth/api/dto/`](../../src/main/java/dev/edujacksons/auth/api/dto/)

- **DTO (Data Transfer Object)** — объекты «для передачи по сети», отдельные от сущностей БД.
  Зачем не отдавать `User` наружу: в нём `passwordHash` (нельзя светить) и лишние поля. DTO —
  это **контракт API**, он меняется медленнее и осознанно, независимо от схемы БД.
- Все DTO — **`record`** (краткие неизменяемые контейнеры).
- **Валидация входа** — аннотации прямо в `RegisterRequest`:
  `@Email`, `@NotBlank`, `@Size(min=8)`, `@NotNull`. Срабатывают, когда в контроллере стоит
  **`@Valid`**. Нарушение → `MethodArgumentNotValidException` → 400 (см. §6.4).
- `UserResponse.from(user)` / `AuthResponse.bearer(...)` — статические фабрики, маппинг
  сущность → DTO в одном месте.

### 6.3 Контроллер

Файл: [`auth/api/AuthController.java`](../../src/main/java/dev/edujacksons/auth/api/AuthController.java)

- **`@RestController`** = контроллер, где каждый метод возвращает **тело ответа** (JSON), а не
  имя html-страницы. **`@RequestMapping("/api/auth")`** — общий префикс путей.
- **`@PostMapping("/register")`**, **`@GetMapping("/me")`** — метод HTTP + путь.
- **`@RequestBody`** — «возьми JSON из тела и десериализуй в объект». **`@Valid`** — «сначала
  проверь его аннотациями валидации».
- **`ResponseEntity`** — обёртка, позволяющая явно задать статус: `201 Created` на регистрацию,
  `200 OK` на логин.
- **`@AuthenticationPrincipal UUID userId`** — Spring достаёт principal, который наш фильтр
  положил в контекст (§5.2). Замкнулся круг: фильтр → контекст → контроллер.
- Контроллер **тонкий**: провалидировал, позвал сервис, смапил результат в DTO. Никакой
  бизнес-логики.

### 6.4 Единый формат ошибок

Файлы: [`common/api/`](../../src/main/java/dev/edujacksons/common/api/) —
`ApiError`, `ApiException`, `GlobalExceptionHandler`.

- **`@RestControllerAdvice`** (`GlobalExceptionHandler`) — глобальный перехватчик исключений
  для всех контроллеров. Один метод на тип ошибки:
  - `ApiException` (наши доменные, напр. `EmailAlreadyUsedException`) → свой статус (409);
  - `MethodArgumentNotValidException` → 400 + карта «поле → сообщение»;
  - `BadCredentialsException` → 401.
- Смысл: контроллеры и сервисы **бросают исключения**, а превращение их в аккуратный
  HTTP-ответ (`ApiError`) собрано в **одном** месте. Не надо в каждом методе писать try/catch.

---

## 7. Три сквозных сценария (трассировки)

Теперь склей всё вместе. Проследи пальцем по файлам.

### A. Регистрация — `POST /api/auth/register`
1. Фильтры: JWT-фильтр видит, что заголовка `Bearer` нет → личность не ставит, пропускает.
   Авторизация: путь в `permitAll()` → проходит.
2. `AuthController.register`: `@Valid` проверяет DTO (email/пароль/роль). Кривой вход → 400.
3. `AuthService.register`: email занят? → 409. Иначе BCrypt-хеш → `save` (тут `@PrePersist`
   генерит UUID, аудит проставляет даты) → `jwtService.issueToken`.
4. Ответ `201` с телом `AuthResponse` (токен + данные пользователя).

### B. Логин — `POST /api/auth/login`
1. Фильтры: как в А (путь публичный).
2. `AuthController.login` → `AuthService.login` → `authenticationManager.authenticate`.
3. Менеджер через `AppUserDetailsService` грузит пользователя и сверяет пароль
   (`PasswordEncoder.matches`). Неверно → `BadCredentialsException` → **401**.
4. Верно → `issueToken` → `200` с токеном.

### C. Защищённый запрос — `GET /api/auth/me` с `Authorization: Bearer <token>`
1. `JwtAuthenticationFilter`: видит `Bearer`, `jwtService.parse` проверяет подпись+срок,
   кладёт в `SecurityContext` личность (principal = `userId`, authority = `ROLE_...`).
2. Авторизация: `anyRequest().authenticated()` — личность есть → проходит.
   (Не было бы токена → entry point → **401**.)
3. `AuthController.me(@AuthenticationPrincipal UUID userId)` → `AuthService.getById` →
   `UserResponse` → `200`.

---

## 8. Шпаргалка по терминам

| Термин | Одной фразой |
|---|---|
| Bean | объект под управлением Spring (создаёт и внедряет он сам) |
| DI | зависимости просишь в конструкторе, `new` не пишешь |
| `@Configuration` / `@Bean` | класс-фабрика бинов / метод, возвращающий бин |
| Entity | Java-класс, отображённый на строку таблицы |
| Repository | интерфейс доступа к БД, реализацию пишет Spring Data |
| Flyway migration | версионированный `.sql`, «git для схемы БД» |
| `ddl-auto=validate` | Hibernate только сверяет схему, владелец схемы — Flyway |
| JWT | подписанный самодостаточный токен-пропуск (stateless) |
| Filter Chain | цепочка фильтров перед контроллерами (тут и живёт security) |
| Authentication (в контексте) | «кто текущий пользователь», лежит в `SecurityContextHolder` |
| Principal | идентификатор текущего пользователя (у нас — `userId`) |
| UserDetailsService | как загрузить пользователя по логину (путь пароля) |
| AuthenticationManager | механизм проверки логина+пароля |
| PasswordEncoder | одностороннее хеширование пароля (BCrypt) |
| DTO | объект контракта API, отдельный от сущности БД |
| `@Valid` | запустить проверку аннотаций валидации на входном DTO |
| `@RestControllerAdvice` | одно место обработки исключений → HTTP-ответ |
| `@Transactional` | «всё-или-ничего» на уровне транзакции БД |

---

## 9. Вопросы для самопроверки

Ответь себе (в следующем сообщении я поспрашиваю интерактивно и разберём пробелы):

1. Почему пароль в БД нельзя «расшифровать» обратно, и как тогда работает вход?
2. Чем путь установления личности при `/login` отличается от пути при `/me`? Кто в каждом
   случае это делает и трогается ли БД?
3. Что именно лежит внутри JWT? Почему туда безопасно класть роль, но нельзя — пароль?
4. Что произойдёт при старте, если я добавлю в `User` поле, но забуду миграцию Flyway? Почему?
5. Зачем нужен `permitAll()` для `/register` и `/login`, и что защищает всё остальное?
6. Почему `AuthService` возвращает `AuthResult`, а не `AuthResponse`? Что этим достигается?
7. Куда `JwtAuthenticationFilter` кладёт личность и как она потом попадает в `@AuthenticationPrincipal` контроллера?
8. Зачем UUID-ключи вместо автоинкремента — какой довод именно для нашего проекта?
9. Что делает `@RestControllerAdvice` и почему это лучше, чем try/catch в каждом контроллере?
10. Почему `csrf` выключен, а сессии — `STATELESS`? Как это связано с тем, что мы на токенах?
