# Geosearch Server — Руководство для контрибьютеров

## Структура проекта

```
src/
├── main/java/ru/oldzoomer/geosearch/server/   # Основной код
│   └── GeosearchServerApplication.java        # Точка входа (Spring Boot)
├── main/resources/
│   └── application.yaml                       # Конфигурация приложения
└── test/java/ru/oldzoomer/geosearch/server/   # Тесты
    ├── GeosearchServerApplicationTests.java   # Базовый тест
    ├── TestGeosearchServerApplication.java    # Переопределение для тестов
    └── TestcontainersConfiguration.java       # Конфиг Testcontainers
compose.yaml                                   # Docker Compose (Redis)
```

**Пакет:** `ru.oldzoomer.geosearch.server` — все новые классы размещать внутри этого пакета с подпакетами по
модулям/доменам (например, `server.api`, `server.service`, `server.model`).

**Стек:** Spring Boot 4.1.1, Java 25, Gradle 9.7.1, Redis, SpringDoc OpenAPI, Lombok, Testcontainers, GraalVM Native
Image.

## Команды сборки, тестирования и разработки

| Команда                 | Описание                                           |
|-------------------------|----------------------------------------------------|
| `./gradlew build`       | Полная сборка проекта (компиляция + тесты)         |
| `./gradlew test`        | Запуск юнит- и интеграционных тестов               |
| `./gradlew bootRun`     | Запуск приложения в режиме разработки (с DevTools) |
| `./gradlew bootTestRun` | Запуск тестового варианта приложения               |
| `./gradlew buildNative` | Сборка GraalVM Native Image                        |
| `docker compose up -d`  | Запуск зависимостей (Redis) из `compose.yaml`      |

> **Примечание:** `./gradlew` — обёртка Gradle Wrapper. Внешний Gradle устанавливать не нужно.
> Для тестов с Testcontainers требуется запущенный Docker.

## Стиль кода и правила именования

- **Язык:** Java 25
- **Отступы:** 4 пробела (без табуляций)
- **Lombok:** использовать для boilerplate (`@Data`, `@Builder`, `@RequiredArgsConstructor`, `@Slf4j` и т.д.)
- **Именование пакетов:** доменная нотация — `ru.oldzoomer.geosearch.server.<module>`
- **Именование классов:** PascalCase (`UserService`, `TopicController`)
- **Именование методов и переменных:** camelCase (`findNearbyLocations`, `userRepository`)
- **Именование файлов:** совпадает с именем публичного класса
- **Тесты:** суффикс `Test` (например, `UserServiceTest`)
- **Тестовые методы:** `test<MethodName>_<Scenario>_<ExpectedResult>` —
  `testFindNearby_WithValidRadius_ReturnsLocations()`
- **Валидация:** использовать JSR-380 аннотации (`@NotBlank`, `@Size`, `@Positive`) на DTO

## Рекомендации по VCS

### Коммиты

Следовать [Conventional Commits](https://www.conventionalcommits.org/):

```
<type>(<scope>): <subject>

feat(api): add nearby search endpoint
fix(service): handle null coordinates in radius query
refactor(model): rename LocationDto to PlaceDto
test(controller): add integration test for search endpoint
chore(deps): update spring-boot to 4.1.1
```

**Типы:** `feat`, `fix`, `refactor`, `test`, `chore`, `docs`, `style`, `ci`, `perf`.

### Пулл-реквесты

-each PR должен содержать одно логическое изменение

- Описание PR: что изменено и почему
- Связать с issue (если есть) через `Fixes #123`
- Если есть UI/API изменения — добавить скриншоты или примеры запросов
- Все проверки (`./gradlew build`) должны проходить локально перед отправкой

## Обязательные инструменты и источники данных

- **Context7** и доступ в Интернет — обязательные инструменты. Перед началом работы **обязательно проверять документацию
  ** через эти источники. При отсутствии тулзов немедленно прекратить работу агента до подключения.
- **OpenStreetMap (OSM)** — основной источник данных для:
    - **POI** (точки интереса): извлекать через Overpass API или прямые запросы к OSM.
    - **Общественный транспорт**: маршруты, остановки, расписания — только из OSM-данных.
- При реализации геолокационных функций всегда проверять актуальность данных через OSM и документацию API.

## Архитектурные заметки

- **Redis** используется для кэширования. Конфигурация в `application.yaml`.
- **Testcontainers** запускает Redis в Docker для интеграционных тестов.
- **SpringDoc OpenAPI** автоматически генерирует документацию API на `/swagger-ui.html`.
- **GraalVM Native Image** поддерживается — для сборки нужен GraalVM SDK.
- **Docker Compose** (`compose.yaml`) поднимает только Redis на порту 6379.
