# 🛒 My Market App

Веб-приложение «Витрина интернет-магазина», разработанное на **Spring Boot 3.4.1** с использованием реактивного стека технологий и микросервисного подхода.

## 📌 Описание проекта

Приложение представляет собой интернет-магазин, состоящий из двух сервисов:
*   **Shop Service:** Основной сервис магазина (витрина, корзина, заказы).
*   **Payment Service:** Сервис обработки платежей и управления балансом.

Функционал:
* Просмотр товаров с пагинацией и поиском.
* Сортировка товаров по алфавиту и цене.
* Добавление товаров в корзину (с использованием `SESSION_ID` в куках).
* Оформление заказов с проверкой баланса и оплатой через Payment Service.
* Просмотр истории заказов.

Проект реализован на **Spring WebFlux**, **Spring Data R2DBC**, **Redis** и **Thymeleaf**.

---

## 🚀 Технологии

* Java 21
* Spring Boot 3.4.1
* Spring WebFlux (реактивный стек)
* Spring Data R2DBC
* Redis (Reactive)
* Thymeleaf
* Gradle (Kotlin DSL)
* H2 / PostgreSQL (R2DBC драйверы)
* JUnit 5 / AssertJ / Reactor Test
* MapStruct
* Lombok

---

## 📂 Структура проекта

Проект организован в виде мультимодульной системы:

### `shop` — Сервис магазина
* **`ru.yandex.practicum.shop.model` (Domain):** Бизнес-объекты.
* **`ru.yandex.practicum.shop.service` (Use Cases):** Бизнес-логика.
* **`ru.yandex.practicum.shop.entity` (Infrastructure):** Сущности БД (R2DBC).
* **`ru.yandex.practicum.shop.repository` (Infrastructure):** Репозитории.
* **`ru.yandex.practicum.shop.controller` (Interface Adapters):** WebFlux контроллеры.
* **`ru.yandex.practicum.shop.client` (Infrastructure):** Клиент для связи с Payment Service.

### `payment` — Сервис платежей
* **`ru.yandex.practicum.payment.service`:** Логика обработки платежей.
* **`ru.yandex.practicum.payment.repository`:** Доступ к данным аккаунтов.

```text
my-market-app/
├── shop/                  # Модуль магазина
│   ├── src/main/java/ru/yandex/practicum/shop/
│   │   ├── controller/    # Веб-слой
│   │   ├── entity/        # Слой БД
│   │   ├── model/         # Доменный слой
│   │   ├── service/       # Слой логики
│   │   ├── repository/    # Доступ к данным
│   │   └── client/        # HTTP клиенты
├── payment/               # Модуль платежей
│   ├── src/main/java/ru/yandex/practicum/payment/
├── docker-compose.yml     # Запуск инфраструктуры (PostgreSQL, Redis)
└── build.gradle.kts       # Общая конфигурация
```

---

## 🖥️ Функциональность

### 🏪 Витрина товаров
* Отображение списка товаров (плиткой).
* Поиск по названию (регистронезависимый).
* Сортировка (`ALPHA`, `PRICE`).
* Пагинация (настраиваемый размер страницы).

**URL:** `GET /items` (или `/`)

### 📦 Страница товара
* Детальная информация о товаре.
* Управление количеством товара в корзине прямо со страницы.

**URL:** `GET /items/{id}`

### 🛒 Корзина
* Просмотр списка выбранных товаров.
* Изменение количества (`ADD`, `REMOVE`, `DELETE`) через POST-запрос.
* Автоматический расчёт итоговой суммы.

**URL:** `GET /cart/items`

### 📑 Заказы
* История всех оформленных заказов.
* Детальный просмотр состава заказа.

**URL:**
* `GET /orders` — список всех заказов.
* `GET /orders/{id}` — детали конкретного заказа.

### 💳 Оформление заказа
* Проверка баланса пользователя в Payment Service.
* Списание средств (оплата) через API Payment Service.
* Создание заказа на основе текущей корзины пользователя в БД PostgreSQL.
* Очистка корзины в Redis и БД после успешной покупки.

**URL:** `POST /buy`

---

## ⚙️ Запуск проекта

### 1. Локальный запуск (через Gradle Wrapper)
Для запуска обоих сервисов:
```bash
./gradlew bootRun
```
Или сборка и запуск jar (для shop):
```bash
./gradlew :shop:build
java -jar shop/build/libs/shop-0.0.1-SNAPSHOT.jar
```

Магазин будет доступен по адресу: [http://localhost:8080](http://localhost:8080)
Сервис платежей: [http://localhost:8081](http://localhost:8081)

### 2. Запуск через Docker Compose
Запуск всей инфраструктуры (БД, Redis) и сервисов:
```bash
docker-compose up -d
```

## 🛠️ Настройка окружения

Приложение поддерживает настройку через переменные окружения:

| Переменная | Описание | Значение по умолчанию |
|------------|----------|-----------------------|
| `SERVER_PORT` | Порт приложения | `8080` (shop), `8081` (payment) |
| `SPRING_R2DBC_URL` | URL подключения к БД (R2DBC) | `r2dbc:postgresql://localhost:5432/market` |
| `SPRING_R2DBC_USERNAME` | Пользователь БД | `pgsql` |
| `SPRING_R2DBC_PASSWORD` | Пароль БД | `pgsql` |
| `PAYMENT_SERVICE_URL` | URL сервиса платежей | `http://localhost:8081` |

### 🛠️ Обработка ошибок
В приложении реализована централизованная обработка ошибок с помощью `GlobalErrorHandler`, которая корректно обрабатывает исключения валидации и некорректных запросов в реактивном стиле.

### 🏗️ Архитектура (Redis + PostgreSQL + Payment Service)

В проекте реализована архитектура **Redis-first write model** для корзины и взаимодействие с внешним сервисом оплаты.

### 🔄 Data Flow (Корзина)

#### **Write path (Запись)**
1. **User request** — пользователь изменяет состав корзины (`PLUS`, `MINUS`, `DELETE`).
2. **Redis Lua script** — атомарное изменение состояния в Redis (защита от race conditions).
3. **Redis state updated** — обновление `HSET` (товары) + инкремент `version`.
4. **Event → Redis Streams** — публикация события в стрим `cart-events`.
5. **Async consumer** — `CartEventConsumer` асинхронно читает события.
6. **PostgreSQL (R2DBC)** — сохранение изменений в основную базу данных.

#### **Read path (Чтение)**
1. **Redis (primary source)** — данные корзины всегда запрашиваются из Redis.
2. **DB (fallback)** — если в Redis данных нет (miss), выполняется запрос к PostgreSQL (через Initializer).

### 🔄 Процесс оформления заказа (Checkout)
1. **Calculate Total** — расчет суммы заказа.
2. **Check Balance** — запрос баланса в `Payment Service`.
3. **Payment** — запрос на оплату в `Payment Service`.
4. **Create Order** — сохранение заказа и его позиций в PostgreSQL.
5. **Clear Cart** — удаление данных корзины из Redis и PostgreSQL.
6. **Transactional** — процесс обернут в реактивную транзакцию.

#### **Initialization / recovery**
* **DB → Redis bootstrap** — при запуске приложения `CartRedisInitializer` прогревает кэш Redis данными из базы.

---

### Ключевые особенности
* **Взаимодействие сервисов:** `Shop Service` взаимодействует с `Payment Service` через `WebClient`.
* **Redis-first write model (Lua script):**
    * Атомарное изменение данных корзины.
    * Версионирование каждой операции.
    * Встроенная защита от race conditions на уровне Redis.
* **Event streaming (Redis Streams):**
    * Использование `XADD` для регистрации событий `cart-events`.
    * Асинхронная обработка потребителем гарантирует отзывчивость UI.
* **Async DB persistence:**
    * БД обновляется только через Consumer.
    * UI не ждет завершения транзакции в БД.
* **Retry strategy:**
    * Использование `Retry.backoff(100, Duration.ofMillis(200))`.
    * Защита от взаимных блокировок (deadlocks) и временных сбоев БД.
* **Optimistic locking (version):**
    * Проверка `event.version() <= entity.getVersion()`.
    * Защита от обработки событий в неправильном порядке (out-of-order events).
* **Bootstrap cache (DB → Redis):**
    * `CartRedisInitializer` решает проблему "холодного старта" (cold start).

---

## 🔍 Качество кода

В проекте настроен **Qodana** для статического анализа кода. Конфигурация находится в файле `qodana.yaml`.

---

## 🧪 Тестирование

Для именования тестов используется паттерн: `method_WhenCondition_Result`.

Проект содержит:
* Интеграционные тесты (`BaseIntegrationTest`).
* Тесты сервисов (`CartServiceTest`, `ItemServiceTest`, `OrderServiceTest`) с использованием моков.
* Тесты репозиториев (`ItemRepositoryTest`, `CartRepositoryTest`, `OrderRepositoryTest`) на реальной БД (H2 R2DBC).
* Реактивные тесты контроллеров (`BaseWebFluxTest`).

Запуск всех тестов:
```bash
./gradlew test
```

---

## 🗄️ Модель данных

### Доменные модели (Domain Layer):
* **Item:** id, title, description, price, imgPath, count.
* **CartItem:** id, sessionId, itemId, count.
* **Order:** id, sessionId, items, total.
* **OrderItem:** id, orderId, itemId, count.

### Сущности БД (Infrastructure Layer):
* **ItemEntity:** соответствует таблице `items`.
* **CartItemEntity:** соответствует таблице `cart_items` (sessionId, itemId, count, version).
* **OrderEntity:** соответствует таблице `orders` (id, sessionId, total).
* **OrderItemEntity:** соответствует таблице `order_items` (id, orderId, itemId, count).
* **AccountEntity (Payment):** соответствует таблице `accounts` (id, sessionId, balance).

---
