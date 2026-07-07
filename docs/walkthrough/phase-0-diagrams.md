# Диаграммы — Фаза 0 (auth)

> Диаграммы в формате **Mermaid** (текст → картинка). На GitHub рендерятся сами.
> Локально в VS Code: расширение **Markdown Preview Mermaid Support** + превью `Ctrl+Shift+V`.
> Дополняют словесный разбор [`phase-0-auth.md`](phase-0-auth.md).

---

## 1. Модульный монолит: слои и поток запроса

```mermaid
flowchart LR
    Client["Клиент<br/>(браузер / Next.js)"]
    subgraph App["Spring Boot приложение"]
        direction TB
        FC["Security Filter Chain"]
        Ctrl["api/ · AuthController"]
        Svc["service/ · AuthService"]
        Repo["repository/ · UserRepository"]
        FC --> Ctrl --> Svc --> Repo
    end
    DB[("PostgreSQL")]
    Client -->|"HTTP + JSON"| FC
    Repo -->|"SQL (JPA)"| DB
```

Зависимости идут внутрь: `api → service → repository → domain`. Контроллер не лезет в БД напрямую.

---

## 2. Цепочка фильтров: аутентификация → авторизация

```mermaid
flowchart TD
    A["Запрос + опц. заголовок<br/>Authorization: Bearer"] --> B{"JwtAuthenticationFilter<br/>токен валиден?"}
    B -->|"да"| C["кладёт личность + роль<br/>в SecurityContext"]
    B -->|"нет / битый / отсутствует"| D["контекст пуст"]
    C --> E{"AuthorizationFilter<br/>правило для URL"}
    D --> E
    E -->|"permitAll"| F["пропустить"]
    E -->|"authenticated \nи личность есть"| F
    E -->|"authenticated, но личности нет"| G["401<br/>RestAuthenticationEntryPoint"]
    F --> H["Controller"]
```

Аутентификация («кто ты» + роли) — раньше; авторизация («что можно») — позже, иначе нечего проверять.

---

## 3. Дверь №1 — `POST /api/auth/login` (проверка пароля по БД)

```mermaid
sequenceDiagram
    autonumber
    participant C as Клиент
    participant Ctrl as AuthController
    participant Svc as AuthService
    participant AM as AuthenticationManager
    participant UDS as AppUserDetailsService
    participant DB as PostgreSQL
    participant PE as PasswordEncoder
    participant JWT as JwtService

    C->>Ctrl: POST /login {email, password}
    Ctrl->>Ctrl: @Valid проверяет DTO
    Ctrl->>Svc: login(email, password)
    Svc->>AM: authenticate(email, rawPassword)
    AM->>UDS: loadUserByUsername(email)
    UDS->>DB: SELECT ... WHERE email = ?
    DB-->>UDS: user (+ password_hash)
    UDS-->>AM: UserDetails
    AM->>PE: matches(rawPassword, hash)
    PE-->>AM: true / false
    alt пароль верный
        Svc->>JWT: issueToken(user)
        JWT-->>Svc: JWT
        Svc-->>Ctrl: AuthResult
        Ctrl-->>C: 200 AuthResponse {token, user}
    else пароль неверный
        AM-->>Svc: BadCredentialsException
        Ctrl-->>C: 401 ApiError
    end
```

---

## 4. Дверь №2 — `GET /api/auth/me` (проверка токена, БД не для личности)

```mermaid
sequenceDiagram
    autonumber
    participant C as Клиент
    participant F as JwtAuthenticationFilter
    participant SC as SecurityContext
    participant AF as AuthorizationFilter
    participant Ctrl as AuthController
    participant Svc as AuthService
    participant DB as PostgreSQL

    C->>F: GET /me + Authorization: Bearer token
    F->>F: parse(token) — подпись + срок
    F->>SC: setAuthentication(userId, ROLE_x)
    Note over F,SC: БД НЕ трогаем — личность из токена
    F->>AF: передать дальше
    AF->>AF: /me требует authenticated — личность есть
    AF->>Ctrl: пропустить
    Ctrl->>Svc: getById(userId)
    Svc->>DB: SELECT ... WHERE id = ?
    DB-->>Svc: user
    Svc-->>Ctrl: User
    Ctrl-->>C: 200 UserResponse (без password_hash)
```

Сравни 3 и 4: логин сверяет пароль **по базе**; `/me` проверяет **токен**, а в базу идёт лишь за данными профиля.

---

## 5. Классы модуля auth (упрощённо)

> Показаны домен и связка сервисов. Конфиг/фильтры (`SecurityConfig`, `JwtAuthenticationFilter`)
> опущены для читаемости — они «обвязка», см. диаграммы 2–4.

```mermaid
classDiagram
    class BaseEntity {
        <<abstract>>
        -UUID id
        -Instant createdAt
        -Instant updatedAt
    }
    class User {
        -String email
        -String passwordHash
        -Role role
        -String displayName
    }
    class Role {
        <<enumeration>>
        TEACHER
        STUDENT
    }
    class UserRepository {
        <<interface>>
        +findByEmail(String)
        +existsByEmail(String)
    }
    class AuthController {
        +register(RegisterRequest)
        +login(LoginRequest)
        +me(UUID)
    }
    class AuthService {
        +register(...)
        +login(...)
        +getById(UUID)
    }
    class JwtService {
        +issueToken(User)
        +parse(String)
    }
    class AppUserDetailsService {
        +loadUserByUsername(String)
    }
    class AuthenticationManager {
        <<Spring>>
    }

    BaseEntity <|-- User
    User --> Role
    AuthController --> AuthService
    AuthService --> UserRepository
    AuthService --> JwtService
    AuthService --> AuthenticationManager
    AuthenticationManager --> AppUserDetailsService
    AppUserDetailsService --> UserRepository
    UserRepository --> User
```

---

## 6. Схема БД: таблица `users`

```mermaid
erDiagram
    USERS {
        uuid id PK
        varchar email UK
        varchar password_hash
        varchar role "CHECK: TEACHER | STUDENT"
        varchar display_name
        timestamptz created_at
        timestamptz updated_at
    }
```

По мере фаз сюда добавятся `courses`, `groups`, `problems`, `submissions` и связи между ними.
