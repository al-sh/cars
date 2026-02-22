-- V4__create_messages_table.sql — создание таблицы сообщений.
--
-- Сообщение — это один элемент диалога в чате.
-- Каждое сообщение принадлежит чату и имеет роль:
--   USER      — написал пользователь
--   ASSISTANT — ответил ИИ
--   SYSTEM    — системный промпт (не отображается пользователю)
--
-- Связь с chats: messages.chat_id → chats.id (Many-to-One)

CREATE TABLE messages (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    -- Внешний ключ на чат. NOT NULL — сообщение всегда принадлежит чату.
    -- ON DELETE CASCADE: удаление чата удаляет все его сообщения.
    chat_id UUID NOT NULL,

    -- Роль отправителя. CHECK constraint гарантирует допустимые значения на уровне БД.
    -- Хранится в UPPER_CASE в БД ('USER', 'ASSISTANT', 'SYSTEM').
    -- В JSON сериализуется в lower-case ('user', 'assistant', 'system') — через @JsonValue.
    role VARCHAR(20) NOT NULL,

    -- Текст сообщения. TEXT — неограниченный размер (в отличие от VARCHAR).
    -- Сообщения ИИ могут быть длинными (несколько тысяч символов).
    content TEXT NOT NULL,

    -- Временная метка создания. Только created_at — сообщения не редактируются.
    -- updatable = false зафиксировано на уровне Entity (@Column(updatable = false)).
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT fk_messages_chat
        FOREIGN KEY (chat_id) REFERENCES chats(id) ON DELETE CASCADE,

    CONSTRAINT chk_messages_role CHECK (role IN ('USER', 'ASSISTANT', 'SYSTEM'))
);

-- Индекс для загрузки сообщений чата в хронологическом порядке.
-- Cursor-пагинация делает запросы вида: WHERE chat_id = ? AND created_at < ?
-- ORDER BY created_at DESC — новые сообщения первыми (для подгрузки истории снизу).
CREATE INDEX idx_messages_chat_id_created_at ON messages(chat_id, created_at DESC);

-- =============================================================================
-- Триггер: обновить updated_at в chats при добавлении сообщения.
--
-- Зачем: список чатов сортируется по updated_at (самый "живой" чат вверху).
-- Когда приходит новое сообщение — чат должен переместиться наверх списка.
--
-- Аналогия Node.js:
--   После INSERT сообщения делали бы: await db.chats.update({ updatedAt: new Date() }, { where: { id: chatId } })
--   Здесь это делает сама БД — автоматически, без дополнительного кода в Service.
--
-- Аналогия Prisma: $use middleware, но на уровне PostgreSQL.
-- =============================================================================

-- Функция, которую вызывает триггер.
-- RETURNS TRIGGER — обязательный тип возврата для триггерных функций.
-- NEW — псевдозапись с данными нового сообщения (NEW.chat_id — id чата).
CREATE OR REPLACE FUNCTION update_chat_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    UPDATE chats
    SET updated_at = now()
    WHERE id = NEW.chat_id;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Триггер привязывается к таблице messages.
-- AFTER INSERT — срабатывает ПОСЛЕ успешной вставки (не до).
-- FOR EACH ROW — выполняется для каждой вставленной строки.
CREATE TRIGGER trg_messages_update_chat
    AFTER INSERT ON messages
    FOR EACH ROW EXECUTE FUNCTION update_chat_updated_at();
