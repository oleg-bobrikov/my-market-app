Вот готовый `README.md`, который можно сразу вставить в репозиторий 👇

---

# 🛒 My Market App

Веб-приложение «Витрина интернет-магазина», разработанное на **Spring Boot** с использованием блокирующего стека технологий.

## 📌 Описание проекта

Приложение представляет собой интернет-магазин с базовым функционалом:

* просмотр товаров
* поиск и сортировка
* добавление товаров в корзину
* оформление заказов
* просмотр истории заказов

Проект реализован с использованием **Spring Web MVC**, **Spring Data JPA** и **Hibernate**.

---

## 🚀 Технологии

* Java 21
* Spring Boot
* Spring Web MVC
* Spring Data JPA
* Hibernate ORM
* Thymeleaf
* Maven
* H2 / PostgreSQL
* JUnit 5
* Docker

---

## 📂 Структура проекта

```
my-market-app/
├── pom.xml
├── src/
│   ├── main/
│   │   ├── java/...         # исходный код приложения
│   │   └── resources/
│   │       ├── templates/   # HTML (Thymeleaf)
│   │       └── application.properties
│   └── test/               # тесты
```

---

## 🖥️ Функциональность

### 🏪 Витрина товаров

* Отображение списка товаров
* Поиск по названию и описанию
* Сортировка:

  * по алфавиту
  * по цене
* Пагинация

**URL:**

```
GET /items
GET /
```

---

### 📦 Страница товара

* Детальная информация о товаре
* Добавление/удаление из корзины
* Изменение количества

**URL:**

```
GET /items/{id}
POST /items/{id}
```

---

### 🛒 Корзина

* Список добавленных товаров
* Изменение количества
* Удаление товаров
* Подсчёт общей суммы

**URL:**

```
GET /cart/items
POST /cart/items
```

---

### 📑 Заказы

* Просмотр всех заказов
* Просмотр конкретного заказа

**URL:**

```
GET /orders
GET /orders/{id}
```

---

### 💳 Оформление заказа

* Эмуляция покупки
* Очистка корзины
* Переход на страницу заказа

**URL:**

```
POST /buy
```

---

## ⚙️ Запуск проекта

### 1. Сборка

```bash
mvn clean install
```

### 2. Запуск

```bash
java -jar target/my-market-app.jar
```

Приложение будет доступно по адресу:

```
http://localhost:8080
```

---

## 🐳 Docker

### Сборка образа

```bash
docker build -t my-market-app .
```

### Запуск контейнера

```bash
docker run -p 8080:8080 my-market-app
```

---

## 🗄️ Конфигурация

Файл:

```
src/main/resources/application.properties
```

Пример для H2:

```properties
spring.datasource.url=jdbc:h2:mem:testdb
spring.datasource.driverClassName=org.h2.Driver
spring.jpa.database-platform=org.hibernate.dialect.H2Dialect
spring.h2.console.enabled=true
```

---

## 🧪 Тестирование

Проект покрыт:

* Unit-тестами
* Интеграционными тестами

Используемые инструменты:

* JUnit 5
* Spring Boot Test
* TestContext Framework

Запуск тестов:

```bash
mvn test
```

---

## 📸 UI страницы

В проекте используются шаблоны Thymeleaf:

* `items.html` — витрина товаров
* `item.html` — карточка товара
* `cart.html` — корзина
* `orders.html` — список заказов
* `order.html` — заказ

---

## 📦 Модель данных

### Товар (Item)

* id
* title
* description
* imgPath
* price
* count

### Заказ (Order)

* id
* items
* totalSum

---

## 📈 Дополнительно

* Поддержка загрузки изображений товаров
* Возможность расширения (REST API, авторизация и т.д.)

---

## 📎 Требования

* Java 21+
* Maven 3.8+
* Docker (опционально)

---

## 👨‍💻 Автор

Учебный проект.
