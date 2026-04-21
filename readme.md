# 🛒 My Market App

Веб-приложение «Витрина интернет-магазина», разработанное на **Spring Boot 4.0.5** с использованием блокирующего стека технологий.

## 📌 Описание проекта

Приложение представляет собой интернет-магазин с базовым функционалом:
* Просмотр товаров с пагинацией и поиском.
* Сортировка товаров по алфавиту и цене.
* Добавление товаров в корзину (с использованием `SESSION_ID` в куках).
* Оформление заказов и просмотр истории заказов.

Проект реализован на **Spring Web MVC**, **Spring Data JPA** и **Thymeleaf**.

---

## 🚀 Технологии

* Java 21
* Spring Boot 4.0.5
* Spring Web MVC
* Spring Data JPA
* Hibernate ORM
* Thymeleaf
* Gradle (Kotlin DSL)
* H2 / PostgreSQL
* JUnit 5 / AssertJ
* MapStruct
* Lombok

---

## 📂 Структура проекта

```text
my-market-app/
├── build.gradle.kts        # Конфигурация Gradle
├── gradlew                 # Скрипт запуска Gradle (Unix)
├── gradlew.bat             # Скрипт запуска Gradle (Windows)
├── src/
│   ├── main/
│   │   ├── java/...        # Исходный код (ru.yandex.practicum.mymarket)
│   │   └── resources/
│   │       ├── static/     # Статические ресурсы (изображения)
│   │       ├── templates/  # Шаблоны Thymeleaf (HTML)
│   │       ├── data.sql    # Начальные данные для БД
│   │       └── application.properties
│   └── test/               # Тесты (Integration, WebMvc)
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
| `SPRING_DATASOURCE_URL` | URL подключения к БД | `jdbc:h2:file:./data/market;...` |
| `SPRING_DATASOURCE_USERNAME` | Пользователь БД | `sa` |
| `SPRING_DATASOURCE_PASSWORD` | Пароль БД | (пусто) |

---


## 🧪 Тестирование

Проект содержит:
* Интеграционные тесты (`CartIntegrationTest`, `OrderIntegrationTest`).
* Тесты контроллеров и редиректов.
* Тесты логики сортировки и отображения.

Запуск всех тестов:
```bash
./gradlew test
```

---

## 🗄️ Модель данных

### Основные сущности:
* **Item (Товар):** id, title, description, price, imgPath.
* **CartItem (Элемент корзины):** id, sessionId, item, count.
* **Order (Заказ):** id, sessionId, items (OrderItem), total.
* **OrderItem (Элемент заказа):** id, order, item, count.

---
