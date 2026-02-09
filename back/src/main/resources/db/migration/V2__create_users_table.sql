-- V2__create_users_table.sql — создание таблицы пользователей.
--
-- Это первая "настоящая" миграция (V1 была пустой инициализацией).
-- Flyway выполнит этот файл ровно один раз и запишет в flyway_schema_history.
--
-- Таблица users хранит всех пользователей системы: клиентов, менеджеров и админов.
-- Аналогия Node.js: это как Prisma-миграция или Knex migration.

CREATE TABLE users (
    -- UUID вместо auto-increment (SERIAL):
    -- 1. Безопаснее — нельзя перебрать ID (/users/1, /users/2, /users/3)
    -- 2. Не зависит от сервера — можно генерировать на клиенте
    -- 3. Уникален глобально — безопасно объединять данные из разных БД
    -- gen_random_uuid() — встроенная функция PostgreSQL для генерации UUID v4
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    -- Email — основной идентификатор для входа.
    -- NOT NULL + UNIQUE constraint гарантируют уникальность на уровне БД.
    email VARCHAR(255) NOT NULL,

    -- Имя пользователя для отображения в интерфейсе
    name VARCHAR(255) NOT NULL,

    -- Хеш пароля (BCrypt). Никогда не храним пароль в открытом виде!
    -- BCrypt хеш выглядит так: $2a$10$N9qo8uLOickgx2ZMRZoMye...
    -- Длина BCrypt хеша — 60 символов, но берём с запасом (255)
    password_hash VARCHAR(255) NOT NULL,

    -- Роль пользователя. CHECK constraint гарантирует валидность на уровне БД.
    -- DEFAULT 'CLIENT' — при регистрации все пользователи получают роль клиента.
    -- Менеджер/админ назначаются вручную.
    role VARCHAR(20) NOT NULL DEFAULT 'CLIENT',

    -- Временная метка создания. TIMESTAMPTZ хранит время с часовым поясом.
    -- now() — текущее время сервера PostgreSQL.
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),

    -- Soft delete: вместо физического удаления ставим флаг deleted = true.
    -- Преимущества:
    -- 1. Можно восстановить пользователя
    -- 2. Сохраняется история (чаты, сообщения ссылаются на user_id)
    -- 3. Соответствует GDPR — можно очистить персональные данные, оставив запись
    deleted BOOLEAN NOT NULL DEFAULT false,

    -- Constraint: email должен быть уникальным.
    -- Имя constraint (uk_users_email) помогает при отладке ошибок —
    -- вместо "unique_violation" увидим "uk_users_email" в сообщении об ошибке.
    CONSTRAINT uk_users_email UNIQUE (email),

    -- Constraint: роль может быть только одной из трёх.
    -- Дополнительная защита — даже если баг в Java-коде, БД не примет невалидные данные.
    CONSTRAINT chk_users_role CHECK (role IN ('CLIENT', 'MANAGER', 'ADMIN'))
);

-- Индекс на email для быстрого поиска при логине.
-- Без индекса PostgreSQL сканирует ВСЮ таблицу (Seq Scan).
-- С индексом — находит за O(log n) через B-tree (Index Scan).
-- UNIQUE constraint выше уже создаёт индекс, но явный индекс — хорошая практика
-- для документирования намерений.
CREATE INDEX idx_users_email ON users(email);
