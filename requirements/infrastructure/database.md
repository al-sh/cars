# Схема базы данных

> **Зависимости:** CONTEXT.md, contracts/types.md

PostgreSQL 16, схема для всех сервисов.

---

## ER-диаграмма

```
┌──────────────────┐       ┌──────────────────┐
│      users       │       │  refresh_tokens  │
├──────────────────┤       ├──────────────────┤
│ id (PK)          │───┐   │ id (PK)          │
│ email (UNIQUE)   │   │   │ user_id (FK)     │───┐
│ name             │   │   │ token            │   │
│ password_hash    │   │   │ expires_at       │   │
│ role             │   │   │ created_at       │   │
│ created_at       │   │   └──────────────────┘   │
│ deleted          │   │                          │
└──────────────────┘   │                          │
         │             └──────────────────────────┘
         │ 1:N
         ▼
┌──────────────────┐
│      chats       │
├──────────────────┤
│ id (PK)          │
│ user_id (FK)     │
│ title            │
│ created_at       │
│ updated_at       │
│ deleted          │
└──────────────────┘
         │ 1:N
         ▼
┌──────────────────┐
│    messages      │
├──────────────────┤
│ id (PK)          │
│ chat_id (FK)     │
│ role             │
│ content          │
│ created_at       │
└──────────────────┘

┌──────────────────┐
│      cars        │
├──────────────────┤
│ id (PK)          │
│ brand            │
│ model            │
│ year             │
│ price            │
│ body_type        │
│ engine_type      │
│ engine_volume    │
│ power_hp         │
│ transmission     │
│ drive            │
│ seats            │
│ fuel_consumption │
│ description      │
│ image_url        │
│ created_at       │
└──────────────────┘
```

---

## DDL

### Таблица users

```sql
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) NOT NULL,
    name VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL DEFAULT 'CLIENT',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted BOOLEAN NOT NULL DEFAULT false,
    
    CONSTRAINT uk_users_email UNIQUE (email),
    CONSTRAINT chk_users_role CHECK (role IN ('CLIENT', 'MANAGER', 'ADMIN'))
);

CREATE INDEX idx_users_email ON users(email) WHERE deleted = false;
CREATE INDEX idx_users_role ON users(role) WHERE deleted = false;
```

### Таблица refresh_tokens

```sql
CREATE TABLE refresh_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    token VARCHAR(255) NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    
    CONSTRAINT fk_refresh_tokens_user 
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT uk_refresh_tokens_token UNIQUE (token)
);

CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens(user_id);
CREATE INDEX idx_refresh_tokens_expires_at ON refresh_tokens(expires_at);
```

### Таблица chats

```sql
CREATE TABLE chats (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    title VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted BOOLEAN NOT NULL DEFAULT false,
    
    CONSTRAINT fk_chats_user 
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_chats_user_id ON chats(user_id) WHERE deleted = false;
CREATE INDEX idx_chats_updated_at ON chats(updated_at DESC) WHERE deleted = false;
```

### Таблица messages

```sql
CREATE TABLE messages (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    chat_id UUID NOT NULL,
    role VARCHAR(20) NOT NULL,
    content TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    
    CONSTRAINT fk_messages_chat 
        FOREIGN KEY (chat_id) REFERENCES chats(id) ON DELETE CASCADE,
    CONSTRAINT chk_messages_role CHECK (role IN ('USER', 'ASSISTANT', 'SYSTEM'))
);

CREATE INDEX idx_messages_chat_id ON messages(chat_id);
CREATE INDEX idx_messages_created_at ON messages(chat_id, created_at DESC);
```

### Таблица cars

```sql
CREATE TABLE cars (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    brand VARCHAR(100) NOT NULL,
    model VARCHAR(100) NOT NULL,
    year INTEGER NOT NULL,
    price INTEGER NOT NULL,
    body_type VARCHAR(20) NOT NULL,
    engine_type VARCHAR(20) NOT NULL,
    engine_volume DECIMAL(3,1),
    power_hp INTEGER NOT NULL,
    transmission VARCHAR(20) NOT NULL,
    drive VARCHAR(10) NOT NULL,
    seats INTEGER NOT NULL,
    fuel_consumption DECIMAL(4,1),
    description TEXT,
    image_url VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    
    CONSTRAINT chk_cars_body_type CHECK (
        body_type IN ('SEDAN', 'SUV', 'HATCHBACK', 'WAGON', 'MINIVAN', 'COUPE', 'PICKUP')
    ),
    CONSTRAINT chk_cars_engine_type CHECK (
        engine_type IN ('PETROL', 'DIESEL', 'HYBRID', 'ELECTRIC')
    ),
    CONSTRAINT chk_cars_transmission CHECK (
        transmission IN ('MANUAL', 'AUTOMATIC', 'ROBOT', 'CVT')
    ),
    CONSTRAINT chk_cars_drive CHECK (
        drive IN ('FWD', 'RWD', 'AWD')
    ),
    CONSTRAINT chk_cars_year CHECK (year >= 2000 AND year <= 2030),
    CONSTRAINT chk_cars_price CHECK (price > 0),
    CONSTRAINT chk_cars_seats CHECK (seats >= 2 AND seats <= 9)
);

-- Индексы для поиска
CREATE INDEX idx_cars_price ON cars(price);
CREATE INDEX idx_cars_body_type ON cars(body_type);
CREATE INDEX idx_cars_engine_type ON cars(engine_type);
CREATE INDEX idx_cars_brand ON cars(brand);
CREATE INDEX idx_cars_year ON cars(year);
CREATE INDEX idx_cars_seats ON cars(seats);

-- Составной индекс для типичных запросов
CREATE INDEX idx_cars_search ON cars(body_type, price, year);
```

---

## Триггеры

### Обновление updated_at для chats

```sql
CREATE OR REPLACE FUNCTION update_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = now();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_chats_updated_at
    BEFORE UPDATE ON chats
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at();
```

### Обновление updated_at чата при новом сообщении

```sql
CREATE OR REPLACE FUNCTION update_chat_on_message()
RETURNS TRIGGER AS $$
BEGIN
    UPDATE chats SET updated_at = now() WHERE id = NEW.chat_id;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_messages_update_chat
    AFTER INSERT ON messages
    FOR EACH ROW
    EXECUTE FUNCTION update_chat_on_message();
```

---

## Миграции (Flyway)

Структура директории:

```
src/main/resources/db/migration/
├── V1__create_users_table.sql
├── V2__create_refresh_tokens_table.sql
├── V3__create_chats_table.sql
├── V4__create_messages_table.sql
├── V5__create_cars_table.sql
├── V6__add_triggers.sql
└── V7__seed_cars_data.sql
```

---

## Seed данные

### Тестовые пользователи

```sql
-- V7.1__seed_users.sql
INSERT INTO users (id, email, name, password_hash, role) VALUES
-- password: 'password123' (bcrypt hash)
('00000000-0000-0000-0000-000000000001', 
 'client@example.com', 'Тестовый Клиент', 
 '$2a$10$...', 'CLIENT'),

('00000000-0000-0000-0000-000000000002', 
 'manager@example.com', 'Тестовый Менеджер', 
 '$2a$10$...', 'MANAGER');
```

### Тестовые автомобили

```sql
-- V7.2__seed_cars.sql
INSERT INTO cars (brand, model, year, price, body_type, engine_type, 
                  engine_volume, power_hp, transmission, drive, seats, 
                  fuel_consumption, description) VALUES

-- Кроссоверы
('Toyota', 'RAV4', 2023, 3500000, 'SUV', 'PETROL', 2.5, 199, 'AUTOMATIC', 'AWD', 5, 8.1,
 'Популярный семейный кроссовер с отличной надёжностью'),
('Mazda', 'CX-5', 2023, 3200000, 'SUV', 'PETROL', 2.5, 194, 'AUTOMATIC', 'AWD', 5, 7.8,
 'Стильный кроссовер с отличной управляемостью'),
('Kia', 'Sportage', 2023, 2800000, 'SUV', 'PETROL', 2.0, 150, 'AUTOMATIC', 'AWD', 5, 8.5,
 'Современный дизайн, богатая комплектация'),
('Hyundai', 'Tucson', 2023, 2900000, 'SUV', 'PETROL', 2.0, 150, 'AUTOMATIC', 'AWD', 5, 8.3,
 'Футуристичный дизайн, много технологий'),
('Toyota', 'Highlander', 2023, 6500000, 'SUV', 'PETROL', 3.5, 249, 'AUTOMATIC', 'AWD', 7, 11.5,
 'Большой семейный кроссовер'),

-- Седаны
('Toyota', 'Camry', 2023, 3000000, 'SEDAN', 'PETROL', 2.5, 200, 'AUTOMATIC', 'FWD', 5, 8.5,
 'Надёжный бизнес-седан'),
('Kia', 'K5', 2023, 2500000, 'SEDAN', 'PETROL', 2.5, 194, 'AUTOMATIC', 'FWD', 5, 8.2,
 'Спортивный седан с современным дизайном'),
('Hyundai', 'Sonata', 2023, 2600000, 'SEDAN', 'PETROL', 2.5, 180, 'AUTOMATIC', 'FWD', 5, 8.0,
 'Комфортный седан D-класса'),

-- Хэтчбеки
('Volkswagen', 'Golf', 2022, 2500000, 'HATCHBACK', 'PETROL', 1.4, 150, 'AUTOMATIC', 'FWD', 5, 6.5,
 'Эталон компактного хэтчбека'),
('Kia', 'Ceed', 2023, 1800000, 'HATCHBACK', 'PETROL', 1.6, 128, 'AUTOMATIC', 'FWD', 5, 6.8,
 'Практичный городской автомобиль'),

-- Минивэны
('Kia', 'Carnival', 2023, 4500000, 'MINIVAN', 'PETROL', 3.5, 294, 'AUTOMATIC', 'FWD', 7, 12.0,
 'Премиальный минивэн со сдвижными дверями'),

-- Электромобили
('Tesla', 'Model 3', 2023, 4500000, 'SEDAN', 'ELECTRIC', NULL, 283, 'AUTOMATIC', 'AWD', 5, NULL,
 'Запас хода 500 км, быстрая зарядка'),
('Zeekr', '001', 2023, 5000000, 'HATCHBACK', 'ELECTRIC', NULL, 544, 'AUTOMATIC', 'AWD', 5, NULL,
 'Премиальный электромобиль, запас хода 600 км');
```

---

## Конфигурация подключения

```yaml
# application.yml

spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/carsai
    username: ${DB_USERNAME:carsai}
    password: ${DB_PASSWORD:carsai}
    driver-class-name: org.postgresql.Driver
  
  jpa:
    hibernate:
      ddl-auto: validate  # Используем Flyway для миграций
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
        format_sql: true
    show-sql: false
  
  flyway:
    enabled: true
    locations: classpath:db/migration
    baseline-on-migrate: true
```

---

## Бэкап и восстановление

```bash
# Бэкап
pg_dump -h localhost -U carsai -d carsai > backup.sql

# Восстановление
psql -h localhost -U carsai -d carsai < backup.sql
```
