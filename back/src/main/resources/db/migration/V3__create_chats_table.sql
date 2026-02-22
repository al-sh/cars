-- V3__create_chats_table.sql — создание таблицы чатов.
--
-- Чат — это диалог пользователя с ИИ-ассистентом.
-- Один пользователь может иметь много чатов (разные сессии поиска автомобиля).
-- Каждый чат принадлежит ровно одному пользователю.
--
-- Связь с users: chats.user_id → users.id (Many-to-One)
-- Аналогия Prisma: model Chat { user User @relation(fields: [userId], references: [id]) }

CREATE TABLE chats (
    -- UUID первичный ключ — как и у users, для безопасности и распределённой генерации
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    -- Внешний ключ на пользователя. NOT NULL — каждый чат обязан принадлежать юзеру.
    -- ON DELETE CASCADE: если пользователь удалён — его чаты тоже удаляются.
    -- В реальном приложении с soft delete CASCADE может быть лишним,
    -- но здесь упрощаем: физическое удаление user → каскадное удаление чатов.
    user_id UUID NOT NULL,

    -- Заголовок чата — NULL по умолчанию (автогенерация после первого сообщения).
    -- VARCHAR(255) — достаточно для заголовка. TEXT если нужен неограниченный размер.
    title VARCHAR(255),

    -- Временные метки. TIMESTAMPTZ — timestamp with time zone, всегда хранит UTC.
    -- created_at — когда чат создан (не меняется).
    -- updated_at — когда последний раз была активность (обновляется при новом сообщении).
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),

    -- Soft delete — логическое удаление вместо физического.
    -- Пользователь "удаляет" чат — мы ставим deleted = true.
    -- Данные сохраняются в БД, но скрыты от пользователя.
    deleted BOOLEAN NOT NULL DEFAULT false,

    -- Внешний ключ: user_id ссылается на users.id.
    -- Имя constraint (fk_chats_user) помогает при отладке ошибок.
    -- ON DELETE CASCADE: удаление пользователя удаляет все его чаты.
    CONSTRAINT fk_chats_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Индекс на user_id для быстрого получения всех чатов пользователя.
-- WHERE deleted = false — partial index: индексируем только активные чаты.
-- Это уменьшает размер индекса и ускоряет запросы (мы всегда фильтруем deleted = false).
-- Аналогия: как фильтр в массиве TypeScript, но на уровне БД.
CREATE INDEX idx_chats_user_id ON chats(user_id) WHERE deleted = false;

-- Индекс на updated_at для сортировки чатов по последней активности.
-- DESC — новые чаты вверху (как в Telegram или WhatsApp).
-- WHERE deleted = false — снова partial index для активных чатов.
CREATE INDEX idx_chats_updated_at ON chats(updated_at DESC) WHERE deleted = false;
