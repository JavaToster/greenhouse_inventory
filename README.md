# greenhouse-inventory

![Java](https://img.shields.io/badge/Java-21-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0.6-brightgreen)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15-blue)
![Redis](https://img.shields.io/badge/Redis-challenge--response-DC382D)
![Liquibase](https://img.shields.io/badge/Liquibase-migrations-cc6600)
![Docker](https://img.shields.io/badge/Docker-multi--stage-2496ED)

Сервис кластеров теплиц и IoT-устройств для системы **[Greenhouse](../../)**.

Отвечает за регистрацию кластеров, назначение рабочих, генерацию и активацию IoT-устройств, а также за challenge-response аутентификацию устройств с выдачей JWT роли `DEVICE`. Является центральной точкой проверки владения кластером — `greenhouse-telemetries` делегирует сюда авторизацию при запросах на чтение телеметрии.

> Это один из сервисов super-репозитория **Greenhouse**, подключённый как git submodule. Общая архитектура, docker-compose и инструкция по запуску всей системы — в [README супер-репозитория](../../README.md).

---

## Содержание

- [Роль в системе](#роль-в-системе)
- [Технологии](#технологии)
- [Доменная модель](#доменная-модель)
- [API](#api)
- [Аутентификация устройств (challenge-response)](#аутентификация-устройств-challenge-response)
- [Защита от подбора подписи](#защита-от-подбора-подписи)
- [Шифрование секретов устройств](#шифрование-секретов-устройств)
- [Безопасность и роли](#безопасность-и-роли)
- [Обработка ошибок](#обработка-ошибок)
- [Производительность](#производительность)
- [Миграции БД](#миграции-бд)
- [Переменные окружения](#переменные-окружения)
- [Запуск](#запуск)
- [Структура проекта](#структура-проекта)

---

## Роль в системе

```
greenhouse-authentication
        │
        │ GET /api/users/{id}          ← валидация роли owner/worker
        │ POST /api/users/batch         ← обогащение списков кластеров
        ▼
┌──────────────────────────────────────────────────────────┐
│                    greenhouse-inventory                   │
│                                                            │
│  POST /api/clusters                — регистрация кластера │
│  POST /api/clusters/{id}/workers   — назначение рабочего  │
│  POST /api/devices/auth/challenge  — шаг 1 (публичный)   │
│  POST /api/devices/auth/verify     — шаг 2 (публичный)   │
│  GET  /api/devices/secrets/{token} — активация устройств  │
│  GET  /api/devices/my-clusters/{id}← вызов из telemetries│
└───────────────────────┬────────────────────────────────────┘
                         │
              ┌──────────┴──────────┐
          PostgreSQL              Redis
     (clusters, devices,   (challenges TTL 30с,
      clusters_workers)     попытки аутентификации,
                             блокировки устройств TTL 5м,
                             temp-секреты TTL 5м)
```

При регистрации кластера и назначении рабочего сервис синхронно проверяет роль пользователя через Feign-вызов к `greenhouse-authentication`. `greenhouse-telemetries` делегирует сюда проверку владения кластером, форвардя JWT пользователя.

## Технологии

- **Java 21**, **Spring Boot 4.0.6**
- Spring Web MVC, Spring Security (`@PreAuthorize`, кастомный `JwtFilter`)
- Spring Data JPA, **PostgreSQL**
- Spring Data Redis — challenge-response, rate-limit попыток, блокировки устройств, временные секреты
- **Liquibase** — миграции схемы
- **java-jwt** (Auth0) — выпуск `DEVICE`-токенов и верификация `USER`-токенов (HMAC256)
- **Spring Cloud OpenFeign** — вызовы `greenhouse-authentication`
- AES/GCM (javax.crypto) — шифрование секретов устройств в БД
- **ModelMapper**, Lombok, Jakarta Validation
- Docker / multi-stage build: `maven:3.9.5-eclipse-temurin-21` → `eclipse-temurin:21-jre-jammy`

## Доменная модель

### `Cluster`

| Поле | Тип | Описание |
|---|---|---|
| `id` | `UUID` | Генерируется БД (`@GeneratedValue`) |
| `name` | `varchar(100)` | Название кластера теплиц |
| `description` | `varchar(200)` | Необязательное описание |
| `ownerId` | `bigint` | Telegram ID владельца — проверяется через auth-сервис при создании |
| `devices` | `Set<Device>` | Устройства кластера (`@OneToMany`, LAZY) |
| `workerIds` | `Set<Long>` | Telegram ID назначенных рабочих (`clusters_workers`) |

### `Device`

| Поле | Тип | Описание |
|---|---|---|
| `id` | `UUID` | Генерируется приложением при создании |
| `secret` | `varchar(128)` | AES/GCM-зашифрованный секрет — только так хранится в БД |
| `status` | `varchar(20)` | `PENDING_ACTIVAT` при создании, `ACTIVE` после активации монтажником |
| `cluster` | `Cluster` | `@ManyToOne` |
| `rawSecret` | `@Transient` | Сырой секрет — существует только в момент создания, в БД не попадает |

> Временная блокировка устройства после неудачных попыток аутентификации хранится в Redis с TTL — а не как статус в БД. Это позволяет блокировке истекать автоматически без каких-либо задач по очистке.

## API

### `/api/clusters`

| Метод | Путь | Доступ | Описание |
|---|---|---|---|
| GET | `/api/clusters` | `ADMIN`, `OWNER`, `WORKER` | Список кластеров с деталями пользователей |
| POST | `/api/clusters` | `INSTALLER`, `ADMIN` | Регистрация кластера + генерация N устройств |
| POST | `/api/clusters/{clusterId}/workers` | `OWNER`, `ADMIN` | Назначить рабочего |
| DELETE | `/api/clusters/{clusterId}/workers` | `OWNER`, `ADMIN` | Снять рабочего |

**Логика фильтрации `GET /api/clusters`:**
- `ADMIN` — возвращает все кластеры; принимает необязательные query-параметры `ownerId` и `workerId` для фильтрации;
- `OWNER` — только свои кластеры (по `telegramId` из JWT), параметры фильтрации игнорируются;
- `WORKER` — только кластеры, в которые он назначен.

Ответ обогащается деталями пользователей (owner, workers) через батч-запрос к `greenhouse-authentication` — все ID собираются в один запрос, разбитый на чанки по 500 элементов.

**Регистрация кластера** — перед сохранением в БД сервис проверяет через Feign, что `ownerId` существует и имеет роль `OWNER`. Если нет — `400 Bad Request`, кластер не создаётся:

```bash
curl -X POST http://localhost:8081/api/clusters \
  -H "Authorization: Bearer <installer-jwt>" \
  -H "Content-Type: application/json" \
  -d '{"ownerId": 123456789, "name": "Теплица №1", "devicesCount": 5}'
```

`devicesCount`: от `1` до `100` (`@Min(1)`, `@Max(100)`).

**Ответ** — одноразовый токен для получения секретов устройств:

```json
{ "clusterId": "a1b2c3d4-...", "token": "f9e8d7c6-..." }
```

**Назначение рабочего** — аналогично проверяется, что `workerId` имеет роль `WORKER`:

```bash
curl -X POST http://localhost:8081/api/clusters/{clusterId}/workers \
  -H "Authorization: Bearer <owner-jwt>" \
  -H "Content-Type: application/json" \
  -d '{"workerId": 987654321}'
```

`workerId` должен быть `> 0` (`@Min(1)`). Повторное назначение уже добавленного рабочего — `400 Bad Request`.

### `/api/devices`

| Метод | Путь | Доступ | Описание |
|---|---|---|---|
| GET | `/api/devices/secrets/{token}` | `INSTALLER`, `ADMIN` | Одноразовое получение и активация секретов |
| GET | `/api/devices/my-clusters/{clusterId}` | `OWNER`, `ADMIN` | Список устройств кластера |
| DELETE | `/api/devices/{id}/remove` | `ADMIN` | Удалить устройство |

**Получение секретов** — токен одноразовый: при первом чтении атомарно удаляется из Redis (`GETDEL`), все устройства кластера переводятся в статус `ACTIVE`. Повторный запрос с тем же токеном — `403 Forbidden`:

```bash
curl -H "Authorization: Bearer <installer-jwt>" \
  http://localhost:8081/api/devices/secrets/f9e8d7c6-...
```

```json
[
  { "deviceId": "uuid-1", "rawSecret": "kQ7n..." },
  { "deviceId": "uuid-2", "rawSecret": "pL3x..." }
]
```

**Список устройств кластера** — `OWNER` видит только устройства своих кластеров (проверка владения по `ownerId`); `ADMIN` — любых.

### `/api/devices/auth` — публичные, для самих устройств

| Метод | Путь | Доступ | Описание |
|---|---|---|---|
| POST | `/api/devices/auth/challenge/{deviceId}` | public | Шаг 1: получить challenge |
| POST | `/api/devices/auth/verify` | public | Шаг 2: подтвердить подписью, получить JWT |

`{deviceId}` типизирован как `UUID` — произвольные строки отклоняются на уровне роутинга.

## Аутентификация устройств (challenge-response)

Устройства не используют логин/пароль:

**Шаг 1 — получить challenge:**

```bash
curl -X POST http://localhost:8081/api/devices/auth/challenge/{deviceId}
```

Сервис проверяет, что устройство с таким `id` существует в БД (иначе `400`), генерирует `UUID.randomUUID()` как challenge и сохраняет его в Redis с TTL **30 секунд**.

```json
{ "challenge": "550e8400-e29b-41d4-a716-446655440000" }
```

**Шаг 2 — подтвердить подпись:**

```bash
curl -X POST http://localhost:8081/api/devices/auth/verify \
  -H "Content-Type: application/json" \
  -d '{"deviceId": "uuid", "challenge": "550e8400-...", "signature": "a3f8..."}'
```

Сервис последовательно проверяет:
1. Устройство не заблокировано (Redis-ключ `device-blocked:{id}`);
2. Статус устройства в БД — `ACTIVE` (не активированное устройство не может авторизоваться);
3. Challenge атомарно читается и удаляется из Redis (`GETDEL`) — защита от повторного использования;
4. Challenge из запроса совпадает с сохранённым;
5. Подпись `HMAC-SHA256(challenge, decryptedSecret)` верна — сравнение через `MessageDigest.isEqual` (константное время, защита от timing-атак).

При успехе возвращается JWT с ролью `DEVICE` и claim'ом `cluster_id`, TTL **30 минут**. При ошибке подписи — инкрементируется счётчик попыток.

## Защита от подбора подписи

Каждая неудачная проверка подписи увеличивает счётчик в Redis через `INCR` (ключ `device-auth-attempt:{deviceId}`, TTL **5 минут** от первой попытки). При достижении **5 неудачных попыток** устройство блокируется на **5 минут** — Redis-ключ `device-blocked:{deviceId}` с TTL. Счётчик сбрасывается при успешной аутентификации.

```
Попытки 1–4 → 401 Invalid signature, счётчик растёт
Попытка 5   → 401 + устройство заблокировано на 5 минут
Попытки 6+  → 401 Too many authentication attempts (до истечения TTL)
```

## Шифрование секретов устройств

Секрет генерируется один раз при создании кластера: 32 байта из `SecureRandom`, кодируется в Base64 URL-safe. Далее:

- Передаётся монтажнику через одноразовый Redis-токен (TTL 5 минут) в поле `rawSecret`;
- В БД хранится **только зашифрованная версия** через `AES/GCM/NoPadding`. IV (12 байт) генерируется для каждого шифрования отдельно и хранится вместе с шифротекстом;
- Тег аутентификации GCM (128 бит) обеспечивает целостность — любое изменение шифротекста в БД будет обнаружено при расшифровке.

Параметры (`DEVICE_SECRET_ENCRYPT_KEY`, алгоритм, длина IV и тега) задаются переменными окружения.

## Безопасность и роли

`SecurityConfiguration`:
- `/api/devices/auth/**` — публичные эндпоинты (устройствам нечего предъявить до прохождения challenge-response);
- все остальные запросы — `anyRequest().authenticated()`;
- stateless-сессии, CSRF отключён;
- `@PreAuthorize` на контроллерах дополнительно проверяет `principal instanceof UserPrincipal`, чтобы `DevicePrincipal` (IoT-устройство) не мог попасть на пользовательские эндпоинты даже если бы имело совпадающий role-claim.

`JwtAuthenticationProvider` различает `USER`- и `DEVICE`-токены по claim'у `token_type` и строит соответствующий principal (`UserPrincipal` с ролью или `DevicePrincipal` с `deviceId` и `clusterId`).

Feign-клиент к auth-сервису (`auth-user-service`) логирует только метод и статус ответа (`Logger.Level.BASIC`) — заголовки с JWT в лог не попадают.

## Обработка ошибок

`GlobalExceptionHandler` — единый формат `{ "statusCode": ..., "message": ... }`:

| Исключение | HTTP статус |
|---|---|
| `MethodArgumentNotValidException` | 400 Bad Request (с перечислением ошибок по полям) |
| `HttpMessageNotReadableException` | 400 Bad Request |
| `DataIntegrityViolationException` | 400 Bad Request |
| `BadRequestException` | 400 Bad Request |
| `BadCredentialsException` | 401 Unauthorized |
| `AccessDeniedException` | 403 Forbidden |
| `EntityNotFoundException` | 404 Not Found |
| `FeignException` | 500 Internal Server Error |
| `Exception` (fallback) | 500 Internal Server Error |

## Производительность

`ClusterRepository` использует `LEFT JOIN FETCH` для `devices` и `workerIds` во всех трёх выборках (`findAllWithDetails`, `findByOwnerIdWithDetails`, `findByWorkerIdsContainingWithDetails`) — устройства и рабочие подгружаются одним запросом, N+1 исключён.

Feign-клиент к `greenhouse-authentication` (`auth-user-service`) имеет отдельные таймауты: `connectTimeout: 2000ms`, `readTimeout: 3000ms` — быстрее дефолтных 5000ms, так как используется в критическом пути изменения данных (создание кластера, назначение рабочего).

## Миграции БД

```
db/changelog/
└── migrations/
    └── cluster-and-devices-initial.yaml
        ├── clusters          — основная таблица кластеров
        ├── devices           — устройства, FK → clusters (CASCADE DELETE)
        └── clusters_workers  — рабочие кластера, composite PK (cluster_id, worker_id), CASCADE DELETE
```

## Переменные окружения

| Переменная | Назначение |
|---|---|
| `INVENTORY_SERVER_PORT` | Порт сервиса (пример: `8081`) |
| `INVENTORY_SPRING_DATASOURCE_URL` | JDBC URL базы `greenhouse_inventory` |
| `SPRING_DATASOURCE_USERNAME` / `SPRING_DATASOURCE_PASSWORD` | Креды Postgres |
| `REDIS_HOST` / `REDIS_PORT` | Подключение к Redis |
| `SECURITY_JWT` | Общий JWT-секрет — **одинаковый во всех сервисах** |
| `DEVICE_SECRET_ENCRYPT_KEY` | AES-ключ для шифрования секретов (16/24/32 байта) |
| `DEVICE_SECRET_ENCRYPT_ALGORITHM` | Например, `AES/GCM/NoPadding` |
| `DEVICE_SECRET_ENCRYPT_IV_LENGTH_BYTES` | Длина IV в байтах (рекомендуется `12` для GCM) |
| `DEVICE_SECRET_ENCRYPT_TAG_LENGTH_BITS` | Длина тега аутентификации (рекомендуется `128`) |
| `CLIENTS_USER` | Базовый URL `greenhouse-authentication` для Feign (`UserClient`) |

> ⚠️ `DEVICE_SECRET_ENCRYPT_KEY` и `SECURITY_JWT` — критичные секреты. Не коммитьте `.env` с реальными значениями.

## Запуск

### В составе всей системы (рекомендуется)

```bash
cd Greenhouse
docker compose up -d --build
```

### Локально, для разработки

Нужны локальный Postgres (база `greenhouse_inventory`), Redis и заполненные переменные окружения. `greenhouse-authentication` должен быть доступен по `CLIENTS_USER`.

```bash
./mvnw clean package -DskipTests
java -jar target/inventory-0.0.1-SNAPSHOT.jar
```

### Docker-образ отдельно

```bash
docker build -t greenhouse-inventory .
docker run --rm -p 8081:8081 --env-file .env greenhouse-inventory
```

## Структура проекта

```
src/main/java/com/example/inventory/
├── controllers/
│   ├── ClusterController.java        # GET/POST /api/clusters, workers
│   ├── DeviceController.java          # secrets, my-clusters, remove
│   └── DeviceAuthController.java       # challenge, verify
├── services/
│   ├── ClusterService.java             # бизнес-логика кластеров, батч-обогащение
│   └── DeviceService.java               # challenge-response, шифрование, активация
├── store/
│   ├── ClusterStore.java                # доступ к ClusterRepository
│   └── DeviceStore.java                  # доступ к DeviceRepository
├── repositories/
│   ├── postgres/
│   │   ├── ClusterRepository.java       # JOIN FETCH запросы
│   │   └── DeviceRepository.java
│   └── redis/
│       └── RedisRepository.java          # GETDEL, INCR, TTL-операции
├── models/
│   ├── Cluster.java                       # JPA-entity
│   └── Device.java                         # JPA-entity, Persistable<UUID>
├── DTO/
│   ├── cluster/                             # RegisterNewClusterDTO, WorkerAssigmentDTO, ...
│   ├── device/                               # DeviceInfoDTO, ClusterDevicesTempSecretsDTO, ...
│   ├── auth/                                  # ChallengeDTO, DeviceAuthRequestDTO, ...
│   ├── user/                                   # UserInfoDTO, UserInfoBatchDTO
│   └── error/                                   # ErrorResponseDTO
├── security/
│   ├── jwt/                                     # JwtUtil, JwtFilter, JwtAuthenticationProvider,
│   │                                             #   DeviceJwtTokenIssuer
│   ├── principals/                                # UserPrincipal, DevicePrincipal
│   ├── EncryptionUtil.java                         # AES/GCM шифрование
│   └── FeignJwtInterceptor.java                     # форвард JWT в межсервисные вызовы
├── clients/
│   └── UserClient.java                              # Feign → greenhouse-authentication
├── configurations/
│   ├── security/SecurityConfiguration.java
│   ├── redis/RedisConfiguration.java
│   ├── feign/                                        # FeignConfiguration, UserClientErrorDecoder
│   └── general/BeanConfiguration.java                # ModelMapper, Feign Logger.Level.BASIC
├── exceptions/
│   └── GlobalExceptionHandler.java
└── util/
    ├── Convertor.java                               # Entity → DTO
    ├── enums/                                        # Role, TokenType, DeviceStatus
    └── redis/RedisKeyCreator.java                    # единая точка формирования Redis-ключей
```
