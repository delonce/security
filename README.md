# Микросервисное приложение для информационной безопасности

## Описание проекта

Данный проект представляет собой многомодульное микросервисное приложение на базе Spring Boot, реализующее систему управления информационной безопасностью с функционалом регистрации пользователей, распределения ролей и выдачи токенов доступа.

### Архитектура

Приложение состоит из трех независимых микросервисов:

1. **auth-service** (порт 8081) - Сервис аутентификации и авторизации
   - Выдача JWT токенов (access и refresh)
   - Валидация учетных данных
   - Управление refresh токенами

2. **user-service** (порт 8082) - Сервис управления пользователями
   - Регистрация новых пользователей
   - Управление профилями пользователей
   - Распределение ролей пользователям

3. **role-service** (порт 8083) - Сервис управления ролями и правами доступа
   - Управление ролями
   - Управление правами доступа (permissions)
   - Связывание ролей с правами доступа

### Диаграмма архитектуры

```
┌─────────────┐      ┌─────────────┐      ┌─────────────┐
│   Client    │─────▶│auth-service │──gRPC─▶│user-service│
│  (HTTP)     │      │  (HTTP/gRPC) │       │  (HTTP/gRPC) │
└─────────────┘      └─────────────┘       └─────────────┘
                            │                      │
                            │ gRPC                 │
                            ▼                      ▼
                     ┌─────────────┐      ┌─────────────┐
                     │role-service │      │  PostgreSQL │
                     │  (HTTP/gRPC)│      └─────────────┘
                     └─────────────┘
                            │
                            ▼
                     ┌─────────────┐
                     │  PostgreSQL │
                     └─────────────┘
```

### Межсервисное взаимодействие

Микросервисы взаимодействуют друг с другом через **gRPC** вместо публичных REST API:

- **user-service** (gRPC порт 9092) - предоставляет gRPC сервер для:
  - Валидации учетных данных пользователей (`ValidateCredentials`)
  - Получения информации о пользователях (`GetUserById`, `GetUserByUsername`)
  - Получения информации о ролях (`GetRoleById`)
  - Получения ролей пользователей (`GetUserRoles`)

- **auth-service** - использует `UserServiceGrpcClient` для обращения к user-service через gRPC:
  - При логине вызывает `validateUserCredentials()` для проверки учетных данных
  - При обновлении токена вызывает `getUserById()` для получения информации о пользователе

- **role-service** - использует `UserServiceGrpcClient` для обращения к user-service через gRPC:
  - При получении роли с правами вызывает `getRoleById()` для получения информации о роли

**Реализация gRPC:**
- **Серверная сторона**: `user-service/src/main/java/org/security/user/grpc/UserGrpcService.java` - реализует gRPC методы
- **Клиентская сторона**: 
  - `auth-service/src/main/java/org/security/auth/client/UserServiceGrpcClient.java`
  - `role-service/src/main/java/org/security/role/client/UserServiceGrpcClient.java`
- **Контракт**: `proto/user-service.proto` - определение сервисов и сообщений

**Преимущества gRPC:**
- Более высокая производительность за счет бинарного протокола
- Типобезопасность через Protocol Buffers
- Поддержка потоковой передачи данных
- Меньший размер сообщений по сравнению с JSON
- Автоматическая генерация клиентского и серверного кода из proto-файлов

## Технологический стек

- **Java 21**
- **Spring Boot 3.2.0**
- **Maven** - система сборки
- **PostgreSQL** - база данных
- **Liquibase** - миграции БД
- **JWT (jjwt)** - токены доступа
- **Caffeine** - кеширование
- **Micrometer + Prometheus** - метрики
- **Resilience4j** - отказоустойчивость
- **gRPC** - межсервисное взаимодействие
- **Protocol Buffers** - сериализация данных для gRPC
- **JUnit + Mockito** - unit тесты
- **JMH** - бенчмарки
- **JMeter** - нагрузочное тестирование
- **Helm** - деплой в Kubernetes

## Примененные темы курса

### 1. Многомодульная архитектура
- **Файл**: `pom.xml` (корневой)
- **Описание**: Проект организован как многомодульный Maven проект с родительским POM и тремя модулями-сервисами

### 2. Spring Boot приложения
- **Файлы**: 
  - `auth-service/src/main/java/org/security/auth/AuthServiceApplication.java`
  - `user-service/src/main/java/org/security/user/UserServiceApplication.java`
  - `role-service/src/main/java/org/security/role/RoleServiceApplication.java`
- **Описание**: Каждый модуль является независимым Spring Boot приложением

### 3. Spring Data JPA
- **Файлы**: 
  - `auth-service/src/main/java/org/security/auth/repository/RefreshTokenRepository.java`
  - `user-service/src/main/java/org/security/user/repository/UserRepository.java`
  - `role-service/src/main/java/org/security/role/repository/PermissionRepository.java`
- **Описание**: Использование Spring Data JPA для работы с БД

### 4. Liquibase миграции
- **Файлы**: 
  - `auth-service/src/main/resources/db/changelog/db.changelog-master.xml`
  - `user-service/src/main/resources/db/changelog/db.changelog-master.xml`
  - `role-service/src/main/resources/db/changelog/db.changelog-master.xml`
- **Описание**: Версионирование схемы БД через Liquibase

### 5. Кеширование (java.util.concurrent)
- **Файлы**: 
  - `auth-service/src/main/java/org/security/auth/config/CacheConfig.java`
  - `user-service/src/main/java/org/security/user/config/CacheConfig.java`
  - `role-service/src/main/java/org/security/role/config/CacheConfig.java`
- **Описание**: Использование Caffeine кеша с поддержкой java.util.concurrent для хранения справочных данных

### 6. Метрики и мониторинг
- **Файлы**: 
  - `auth-service/src/main/java/org/security/auth/service/MetricsService.java`
  - `user-service/src/main/java/org/security/user/service/MetricsService.java`
  - `role-service/src/main/java/org/security/role/service/MetricsService.java`
- **Описание**: Интеграция Micrometer + Prometheus для сбора метрик (RPS, latency, успешные/неуспешные запросы, размер кешей)

### 7. Отказоустойчивость (Resilience4j)
- **Файлы**: 
  - `auth-service/src/main/java/org/security/auth/service/AuthService.java`
  - `user-service/src/main/java/org/security/user/service/UserService.java`
- **Описание**: 
  - Retry - повторы при обращении к upstream сервисам
  - Circuit Breaker - защита от каскадных сбоев
  - Rate Limiter - защита от перегрузки

### 8. Планировщик задач
- **Файлы**: 
  - `auth-service/src/main/java/org/security/auth/scheduler/TokenCleanupScheduler.java`
  - `user-service/src/main/java/org/security/user/scheduler/UserMetricsScheduler.java`
  - `role-service/src/main/java/org/security/role/scheduler/PermissionCacheScheduler.java`
- **Описание**: Использование @Scheduled для фоновых процессов (очистка токенов, сбор метрик, инвалидация кеша)

### 9. Spring Security
- **Файлы**: 
  - `auth-service/src/main/java/org/security/auth/config/SecurityConfig.java`
  - `user-service/src/main/java/org/security/user/config/SecurityConfig.java`
- **Описание**: Настройка безопасности и аутентификации

### 10. JWT токены
- **Файлы**: 
  - `auth-service/src/main/java/org/security/auth/service/JwtService.java`
- **Описание**: Генерация и валидация JWT токенов

### 15. gRPC межсервисное взаимодействие
- **Файлы**: 
  - `proto/user-service.proto` - определение контракта
  - `user-service/src/main/java/org/security/user/grpc/UserGrpcService.java` - gRPC сервер
  - `auth-service/src/main/java/org/security/auth/client/UserServiceGrpcClient.java` - gRPC клиент
  - `role-service/src/main/java/org/security/role/client/UserServiceGrpcClient.java` - gRPC клиент
- **Описание**: 
  - Использование gRPC для межсервисного взаимодействия вместо REST
  - Protocol Buffers для сериализации данных
  - Автоматическая генерация Java классов из proto-файлов через protobuf-maven-plugin
  - Spring Boot gRPC starter для интеграции с Spring

### 11. Unit тесты (JUnit + Mockito)
- **Файлы**: 
  - `auth-service/src/test/java/org/security/auth/service/AuthServiceTest.java`
  - `user-service/src/test/java/org/security/user/service/UserServiceTest.java`
  - `role-service/src/test/java/org/security/role/service/RoleServiceTest.java`
- **Описание**: Unit тесты с использованием JUnit 5 и Mockito

### 12. JMH бенчмарки
- **Файлы**: 
  - `auth-service/src/benchmarks/java/org/security/auth/benchmark/JwtServiceBenchmark.java`
  - `user-service/src/benchmarks/java/org/security/user/benchmark/PasswordEncoderBenchmark.java`
  - `role-service/src/benchmarks/java/org/security/role/benchmark/PermissionLookupBenchmark.java`
- **Описание**: Бенчмарки производительности с использованием JMH

### 13. Нагрузочное тестирование (JMeter)
- **Файлы**: 
  - `jmeter/auth-service-test-plan.jmx`
  - `jmeter/user-service-test-plan.jmx`
  - `jmeter/role-service-test-plan.jmx`
- **Описание**: JMeter скрипты для нагрузочного тестирования всех эндпоинтов

### 14. Kubernetes и Helm
- **Файлы**: 
  - `deploy/helm/auth-service/`
  - `deploy/helm/user-service/`
  - `deploy/helm/role-service/`
- **Описание**: Helm чарты для деплоя каждого сервиса в Kubernetes

## Сборка и запуск

### Требования

- Java 21
- Maven 3.8+
- PostgreSQL 14+
- Docker и Kubernetes (для деплоя)

### Локальный запуск

1. **Создание баз данных**

```sql
-- Для auth-service
CREATE DATABASE auth_db;
CREATE USER auth_user WITH PASSWORD 'auth_password';
GRANT ALL PRIVILEGES ON DATABASE auth_db TO auth_user;

-- Для user-service
CREATE DATABASE user_db;
CREATE USER user_user WITH PASSWORD 'user_password';
GRANT ALL PRIVILEGES ON DATABASE user_db TO user_user;

-- Для role-service
CREATE DATABASE role_db;
CREATE USER role_user WITH PASSWORD 'role_password';
GRANT ALL PRIVILEGES ON DATABASE role_db TO role_user;
```

2. **Сборка проекта**

```bash
mvn clean install
```

3. **Запуск сервисов**

```bash
# Терминал 1
cd auth-service
mvn spring-boot:run

# Терминал 2
cd user-service
mvn spring-boot:run

# Терминал 3
cd role-service
mvn spring-boot:run
```

### Запуск в Docker

```bash
# Сборка образов
docker build -t auth-service:latest ./auth-service
docker build -t user-service:latest ./user-service
docker build -t role-service:latest ./role-service

# Запуск через docker-compose (требуется создать docker-compose.yml)
docker-compose up -d
```

### Деплой в Kubernetes

```bash
# Установка Helm чартов
helm install auth-service ./deploy/helm/auth-service
helm install user-service ./deploy/helm/user-service
helm install role-service ./deploy/helm/role-service

# Проверка статуса
kubectl get pods
kubectl get services
```

## API эндпоинты

### Auth Service (порт 8081)

#### POST /api/auth/login
Аутентификация пользователя и получение токенов

**Request:**
```json
{
  "username": "testuser",
  "password": "password123"
}
```

**Response:**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "tokenType": "Bearer",
  "expiresIn": 86400
}
```

#### POST /api/auth/refresh
Обновление access токена с помощью refresh токена

**Request:**
```json
{
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

**Response:**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "tokenType": "Bearer",
  "expiresIn": 86400
}
```

### User Service (порт 8082)

#### POST /api/users/register
Регистрация нового пользователя

**Request:**
```json
{
  "username": "newuser",
  "email": "user@example.com",
  "password": "password123"
}
```

**Response:**
```json
{
  "id": 1,
  "username": "newuser",
  "email": "user@example.com",
  "enabled": true,
  "createdAt": "2024-01-15T10:00:00",
  "roles": ["USER"]
}
```

#### GET /api/users/{id}
Получение информации о пользователе по ID

**Response:**
```json
{
  "id": 1,
  "username": "testuser",
  "email": "test@example.com",
  "enabled": true,
  "createdAt": "2024-01-15T10:00:00",
  "lastLoginAt": "2024-01-15T11:00:00",
  "roles": ["USER"]
}
```

#### GET /api/users/username/{username}
Получение информации о пользователе по username

#### GET /api/users/{id}/roles
Получение списка ролей пользователя

**Response:**
```json
["USER", "MODERATOR"]
```

#### POST /api/users/validate
Валидация учетных данных (REST эндпоинт для совместимости, рекомендуется использовать gRPC)

**Request:**
```json
{
  "username": "testuser",
  "password": "password123"
}
```

**Response:**
```json
{
  "id": 1,
  "username": "testuser"
}
```

**Примечание:** Этот эндпоинт доступен для обратной совместимости. Для межсервисного взаимодействия рекомендуется использовать gRPC метод `ValidateCredentials`.

### gRPC API (user-service)

Все методы доступны через gRPC на порту 9092:

- `ValidateCredentials` - валидация учетных данных
- `GetUserById` - получение пользователя по ID
- `GetUserByUsername` - получение пользователя по username
- `GetRoleById` - получение роли по ID
- `GetUserRoles` - получение ролей пользователя

Определения сервисов находятся в `proto/user-service.proto`

### Role Service (порт 8083)

#### GET /api/roles/{roleId}
Получение роли с правами доступа

**Response:**
```json
{
  "id": 1,
  "permissions": ["USER_READ", "USER_WRITE"]
}
```

#### GET /api/roles/{roleId}/permissions
Получение списка прав доступа для роли

**Response:**
```json
["USER_READ", "USER_WRITE", "USER_DELETE"]
```

#### GET /api/roles/permissions/{permissionId}
Получение информации о праве доступа

**Response:**
```json
{
  "id": 1,
  "name": "USER_READ",
  "description": "Read user information",
  "resource": "USER",
  "action": "READ"
}
```

#### GET /api/roles/permissions
Получение всех прав доступа

## Use Cases

### Use Case 1: Регистрация и аутентификация пользователя

1. **Регистрация пользователя**
   ```bash
   POST http://localhost:8082/api/users/register
   {
     "username": "john_doe",
     "email": "john@example.com",
     "password": "securePassword123"
   }
   ```
   - Пользователь создается в системе
   - Ему автоматически назначается роль USER
   - Пароль хешируется с помощью BCrypt

2. **Аутентификация**
   ```bash
   POST http://localhost:8081/api/auth/login
   {
     "username": "john_doe",
     "password": "securePassword123"
   }
   ```
   - Auth-service вызывает `UserServiceGrpcClient.validateUserCredentials()` 
   - Запрос отправляется через gRPC на user-service (порт 9092)
   - User-service проверяет учетные данные в БД и возвращает результат через gRPC
   - Auth-service генерирует access и refresh токены
   - Refresh токен сохраняется в БД auth-service

3. **Использование access токена**
   ```bash
   GET http://localhost:8082/api/users/1
   Authorization: Bearer {accessToken}
   ```

### Use Case 2: Обновление токена доступа

1. **Получение новых токенов**
   ```bash
   POST http://localhost:8081/api/auth/refresh
   {
     "refreshToken": "{refreshToken}"
   }
   ```
   - Старый refresh токен отзывается
   - Генерируются новые access и refresh токены
   - Новый refresh токен сохраняется в БД

### Use Case 3: Получение прав доступа пользователя

1. **Получение ролей пользователя**
   ```bash
   GET http://localhost:8082/api/users/1/roles
   ```
   - Возвращается список ролей пользователя

2. **Получение прав доступа для роли**
   ```bash
   GET http://localhost:8083/api/roles/1/permissions
   ```
   - Возвращается список прав доступа для указанной роли

### Use Case 4: Нагрузочное тестирование

1. **Запуск JMeter тестов**
   ```bash
   jmeter -n -t jmeter/auth-service-test-plan.jmx -l results.jtl
   jmeter -n -t jmeter/user-service-test-plan.jmx -l results.jtl
   jmeter -n -t jmeter/role-service-test-plan.jmx -l results.jtl
   ```

2. **Просмотр метрик**
   ```bash
   curl http://localhost:8081/actuator/prometheus
   curl http://localhost:8082/actuator/prometheus
   curl http://localhost:8083/actuator/prometheus
   ```

3. **Анализ метрик в Grafana**
   - RPS (Requests Per Second)
   - Latency (время отклика)
   - Успешные/неуспешные запросы
   - Размер кешей

## Метрики

Все сервисы экспортируют метрики в формате Prometheus:

- `auth.login.attempts` - количество попыток входа
- `auth.login.success` - успешные входы
- `auth.login.failure` - неудачные входы
- `auth.login.latency` - задержка при входе
- `user.registration.attempts` - попытки регистрации
- `user.registration.success` - успешные регистрации
- `user.validation.latency` - задержка валидации
- `role.lookup.attempts` - попытки поиска ролей

## Мониторинг

Для просмотра метрик рекомендуется использовать Grafana с дашбордами для:
- Мониторинга RPS каждого сервиса
- Отслеживания latency
- Мониторинга успешных/неуспешных запросов
- Отслеживания размера кешей
- Мониторинга ошибок

## Тестирование

### Unit тесты
```bash
mvn test
```

### JMH бенчмарки
```bash
mvn clean package
java -jar auth-service/target/benchmarks.jar
java -jar user-service/target/benchmarks.jar
java -jar role-service/target/benchmarks.jar
```

### Нагрузочное тестирование
```bash
# Запуск JMeter тестов
jmeter -n -t jmeter/auth-service-test-plan.jmx -l auth-results.jtl
jmeter -n -t jmeter/user-service-test-plan.jmx -l user-results.jtl
jmeter -n -t jmeter/role-service-test-plan.jmx -l role-results.jtl
```

## Лицензия

MIT
