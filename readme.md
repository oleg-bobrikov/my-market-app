# 🛒 My Market App

Веб-приложение «Витрина интернет-магазина», разработанное на **Spring Boot 3.4.5** с использованием реактивного стека технологий и принципов **Clean Architecture**.

## 📌 Описание проекта

Приложение представляет собой интернет-магазин с базовым функционалом:
* Просмотр товаров с пагинацией и поиском.
* Сортировка товаров по алфавиту и цене.
* Добавление товаров в корзину (с использованием `SESSION_ID` в куках).
* Оформление заказов и просмотр истории заказов.

Проект реализован на **Spring WebFlux**, **Spring Data R2DBC** и **Thymeleaf**.

---

## 🚀 Технологии

* Java 21
* Spring Boot 3.4.5
* Spring WebFlux (реактивный стек)
* Spring Data R2DBC
* Thymeleaf
* Gradle (Kotlin DSL)
* H2 / PostgreSQL (R2DBC драйверы)
* JUnit 5 / AssertJ / Reactor Test
* MapStruct
* Lombok

---

## 📂 Структура проекта (Clean Architecture)

Проект организован в соответствии с принципами чистой архитектуры:

* **`ru.yandex.practicum.mymarket.model` (Domain/Entities):** Чистые бизнес-объекты (POJO), не зависящие от фреймворков.
* **`ru.yandex.practicum.mymarket.service` (Use Cases):** Бизнес-логика приложения.
* **`ru.yandex.practicum.mymarket.entity` (Infrastructure):** Сущности базы данных с аннотациями Spring Data R2DBC.
* **`ru.yandex.practicum.mymarket.repository` (Infrastructure):** Реактивные репозитории для доступа к БД.
* **`ru.yandex.practicum.mymarket.controller` (Interface Adapters):** REST-контроллеры, обрабатывающие HTTP-запросы.
* **`ru.yandex.practicum.mymarket.mapper` (Interface Adapters):** Мапперы для преобразования между Entity, Model и DTO.

```text
my-market-app/
├── build.gradle.kts        # Конфигурация Gradle
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── ru.yandex.practicum.mymarket/
│   │   │       ├── controller/  # Веб-слой
│   │   │       ├── entity/      # Слой БД (Infrastructure)
│   │   │       ├── model/       # Доменный слой (Domain)
│   │   │       ├── service/     # Слой логики (Use Cases)
│   │   │       ├── mapper/      # Преобразование данных
│   │   │       └── repository/  # Доступ к данным
│   │   └── resources/
│   │       ├── static/          # Изображения товаров
│   │       ├── templates/       # Шаблоны Thymeleaf (реактивные)
│   │       ├── schema.sql       # Определение таблиц БД
│   │       └── data.sql         # Начальные данные
│   └── test/                    # Тесты (Unit, Integration, R2DBC)
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
* Создание заказа на основе текущей корзины пользователя.
* Очистка корзины после успешной покупки.

**URL:** `POST /buy`

---

## ⚙️ Запуск проекта

### 1. Локальный запуск (через Gradle Wrapper)
```bash
./gradlew bootRun
```
Или сборка и запуск jar:
```bash
./gradlew build
java -jar build/libs/my-market-app-0.0.1-SNAPSHOT.jar
```

Приложение будет доступно по адресу: [http://localhost:8080](http://localhost:8080)

### 2. Запуск через Docker
Сборка образа:
```bash
docker build -t my-market-app .
```
Запуск контейнера:
```bash
docker run -p 8080:8080 my-market-app
```

## 🛠️ Настройка окружения

Приложение поддерживает настройку через переменные окружения:

| Переменная | Описание | Значение по умолчанию |
|------------|----------|-----------------------|
| `SERVER_PORT` | Порт приложения | `8080` |
| `SPRING_R2DBC_URL` | URL подключения к БД (R2DBC) | `r2dbc:postgresql://localhost:5432/market` |
| `SPRING_R2DBC_USERNAME` | Пользователь БД | `pgsql` |
| `SPRING_R2DBC_PASSWORD` | Пароль БД | `pgsql` |

### 🛠️ Обработка ошибок
В приложении реализована централизованная обработка ошибок с помощью `GlobalErrorHandler`, которая корректно обрабатывает исключения валидации и некорректных запросов в реактивном стиле.

---

## 🧪 Тестирование

Проект содержит:
* Интеграционные тесты (`BaseIntegrationTest`).
* Тесты сервисов (`CartServiceTest`, `ItemServiceTest`, `OrderDtoServiceTest`) с использованием моков.
* Тесты репозиториев (`ItemRepositoryTest`, `CartRepositoryTest`, `OrderDtoRepositoryTest`) на реальной БД (H2 R2DBC).
* Реактивные тесты контроллеров (`BaseWebFluxTest`).

Запуск всех тестов:
```bash
./gradlew test
```

---

## 🗄️ Модель данных

### Доменные модели (Domain Layer):
* **Item:** id, title, description, price, imgPath.
* **CartItem:** id, sessionId, item, count.
* **Order:** id, sessionId, items, total.
* **OrderItem:** item, count.

### Сущности БД (Infrastructure Layer):
* **ItemEntity:** соответствует таблице `items`.
* **CartItemEntity:** соответствует таблице `cart_items`.
* **OrderEntity:** соответствует таблице `orders`.
* **OrderItemEntity:** соответствует таблице `order_items`.

---
