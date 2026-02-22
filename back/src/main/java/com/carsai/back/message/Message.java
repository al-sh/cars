package com.carsai.back.message;

import java.time.Instant;
import java.util.UUID;

import com.carsai.back.chat.Chat;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entity-класс сообщения. Маппится на таблицу "messages" в PostgreSQL.
 *
 * Сообщение — это один элемент диалога в чате.
 * Сообщения иммутабельны: после создания не редактируются.
 * (В отличие от чатов, у сообщений нет updated_at и soft delete.)
 *
 * Связи:
 * - Message → Chat: много сообщений в одном чате (@ManyToOne)
 *
 * Аналогия Prisma:
 *   model Message {
 *     id        String      @id @default(uuid())
 *     chatId    String
 *     chat      Chat        @relation(fields: [chatId], references: [id])
 *     role      MessageRole
 *     content   String
 *     createdAt DateTime    @default(now())
 *   }
 */
@Entity
@Table(name = "messages")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Message {

    // === Первичный ключ ===

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // === Связь с чатом ===

    // @ManyToOne(fetch = LAZY) — много сообщений принадлежат одному чату.
    // LAZY: не загружать Chat из БД пока явно не нужен (экономия запросов).
    // При получении сообщений нам обычно нужен только chatId, а не весь объект Chat.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_id", nullable = false)
    private Chat chat;

    // === Роль отправителя ===

    // @Enumerated(EnumType.STRING) — хранить enum как строку ('USER', 'ASSISTANT', 'SYSTEM').
    // В JSON сериализуется в lower-case через @JsonValue в MessageRole enum.
    // НИКОГДА не используй EnumType.ORDINAL — порядковые числа (0, 1, 2) ломаются
    // при изменении порядка констант в enum.
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MessageRole role;

    // === Содержимое ===

    // TEXT в PostgreSQL — неограниченный текст (в отличие от VARCHAR с лимитом).
    // columnDefinition = "TEXT" явно задаёт тип колонки в БД.
    // Используем TEXT т.к. ответы ИИ могут быть длинными (несколько тысяч символов).
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    // === Временная метка ===

    // Только created_at — сообщения иммутабельны (не редактируются).
    // updatable = false — Hibernate не обновит это поле через UPDATE.
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    // === Lifecycle callback ===

    /**
     * Устанавливает created_at перед первым сохранением (INSERT).
     *
     * @PrePersist — JPA вызывает этот метод ПЕРЕД INSERT в БД.
     * После INSERT триггер trg_messages_update_chat (V4 миграция) автоматически
     * обновит updated_at родительского Chat — нам ничего дополнительно делать не нужно.
     */
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
