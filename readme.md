# 🛒 My Market App

Веб-приложение «Витрина интернет-магазина», разработанное на **Spring Boot 3.5.14** с использованием реактивного стека технологий и микросервисного подхода.

## 📌 Описание проекта

Приложение представляет собой интернет-магазин, состоящий из двух сервисов:
* **Shop Service:** Основной сервис магазина (витрина, корзина, заказы). Реализована авторизация по логину и паролю.
* **Payment Service:** RESTful-сервис платежей и управления балансом на базе Spring WebFlux. Защищен с помощью OAuth2 Resource Server.
* **Keycloak:** Сервер идентификации и авторизации (IAM), обеспечивающий работу OAuth2.

Функционал:
* Просмотр товаров с пагинацией и поиском. Данные о товарах кэшируются в **Redis** (TTL 2 мин).
* Сортировка товаров по алфавиту и цене.
* Регистрация и аутентификация пользователей (логин/пароль) в Shop Service.
* Добавление товаров в корзину (с использованием `SESSION_ID` в куках).
* Оформление заказов с проверкой баланса и оплатой через Payment Service. Межсервисное взаимодействие защищено OAuth2 (Shop Service выступает как OAuth2 Client).
* Просмотр истории заказов.

Интеграция между сервисами реализована на основе **OpenAPI спецификации** с генерацией реактивного клиента и серверного кода.

---

## 🚀 Технологии

* Java 21
* Spring Boot 3.5.14
* Spring WebFlux (реактивный стек)
* Spring Data R2DBC (PostgreSQL / H2)
* Spring Data Redis (Reactive)
* Spring Security (Form Login & OAuth2)
* Keycloak (Authorization Server)
* OpenAPI / Swagger (генерация кода)
* MapStruct (маппинг DTO)
* Lombok (сокращение кода)
* Thymeleaf
* Gradle (Kotlin DSL)
* JUnit 5 / StepVerifier (Spring Boot Test)
* Docker / Docker Compose

---

## 📂 Структура проекта

Проект организован в виде мультимодульной системы Gradle:

```text
my-market-app/
├── shop/                  # Модуль магазина (основное приложение)
│   ├── src/main/java/     # Бизнес-логика (controllers, services, repositories)
│   ├── src/main/resources/# Конфигурация, SQL-скрипты, шаблоны Thymeleaf
│   ├── build/generated/   # Сгенерированный OpenAPI клиент
│   ├── Dockerfile         # Docker-образ для shop-service
│   └── build.gradle.kts   # Конфигурация модуля shop
├── payment/               # Модуль платежей (микросервис)
│   ├── src/main/java/     # Логика платежей (OAuth2 Resource Server)
│   ├── src/main/resources/# Конфигурация, схема БД
│   ├── build/generated/   # Сгенерированные OpenAPI интерфейсы и модели
│   ├── Dockerfile         # Docker-образ для payment-service
│   └── build.gradle.kts   # Конфигурация модуля payment
├── keycloak/              # Конфигурация Keycloak
│   └── import/            # Экспортированный реалм для авто-импорта
├── openapi.yaml           # Спецификация API интеграции
├── docker-compose.yml     # Оркестрация контейнеров (PostgreSQL, Redis, Keycloak, Services)
├── settings.gradle.kts    # Определение модулей проекта
├── build.gradle.kts       # Корневой скрипт сборки
└── qodana.yaml            # Конфигурация статического анализа кода
```

---

## ⚙️ Запуск проекта

### Сборка проекта
```bash
./gradlew clean build
```

### Запуск тестов
```bash
./gradlew test
```

### Запуск через Docker Compose
Запуск всей инфраструктуры и приложений:
```bash
docker-compose up --build
```
После запуска:
- Витрина магазина: [http://localhost:8080](http://localhost:8080)
- Сервис платежей: [http://localhost:8081](http://localhost:8081)
- Keycloak: [http://localhost:8082](http://localhost:8082) (admin/admin)
- Redis: `localhost:6379`
- PostgreSQL: `localhost:5432`

---

## 🏗️ Особенности реализации

### ⚡ Кэширование (Redis)
* Список товаров и карточки товаров кэшируются в Redis.
* Если данных в кэше нет, они загружаются из БД и сохраняются в кэш.
* Время жизни кэша (TTL) составляет 2 минуты.

### 💳 Интеграция платежей (OpenAPI & OAuth2)
* Описана спецификация `openapi.yaml`.
* В модуле `shop` генерируется WebClient для обращения к `payment`.
* Межсервисные вызовы защищены с помощью **OAuth2**. `shop-service` настроен как клиент, а `payment-service` — как ресурс-сервер.
* В модуле `payment` генерируются интерфейсы контроллеров и модели данных.
* Передача данных осуществляется в формате JSON.

### 🔐 Безопасность
* **Shop Service:** использует классическую аутентификацию через форму (Form Login) с хранением данных пользователей в БД. Выступает в роли **OAuth2 Client** для взаимодействия с сервисом платежей.
* **Payment Service:** защищен как **OAuth2 Resource Server**, требует валидный JWT-токен, выданный Keycloak.
* **Keycloak:** используется в качестве сервера авторизации. Все необходимые настройки (клиенты, роли) импортируются автоматически при запуске из директории `keycloak/import`.

---

## 🧪 Тестирование
Проект покрыт тестами с использованием:
* **JUnit 5**
* **Spring Boot Test** (интеграционные тесты)
* **TestContext Framework** (кэширование контекстов)
* **Embedded Redis** (для тестов кэширования)
* **StepVerifier** (для проверки реактивных потоков)
* **Mockito** (стабы и моки)

---
