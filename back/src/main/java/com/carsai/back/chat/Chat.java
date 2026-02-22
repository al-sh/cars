package com.carsai.back.chat;

import java.time.Instant;
import java.util.UUID;

import com.carsai.back.user.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entity-класс чата. Маппится на таблицу "chats" в PostgreSQL.
 *
 * Чат — это диалог пользователя с ИИ-ассистентом по подбору автомобиля.
 * У одного пользователя может быть много чатов (разные сессии поиска).
 *
 * Связи:
 * - Chat → User: много чатов у одного пользователя (@ManyToOne)
 * - Chat → Message: у одного чата много сообщений (@OneToMany, не объявляем здесь
 *   чтобы избежать проблем с N+1 запросами — загружаем сообщения отдельно)
 *
 * Аналогия Prisma:
 *   model Chat {
 *     id        String   @id @default(uuid())
 *     userId    String
 *     user      User     @relation(fields: [userId], references: [id])
 *     title     String?
 *     createdAt DateTime @default(now())
 *     updatedAt DateTime @updatedAt
 *     deleted   Boolean  @default(false)
 *   }
 */
@Entity
@Table(name = "chats")
// Lombok: генерирует getters, setters, toString, equals, hashCode
@Data
// JPA требует конструктор без аргументов для создания объектов через рефлексию
@NoArgsConstructor
// Полезен для ручного создания объектов в тестах
@AllArgsConstructor
// Chat.builder().user(user).title("Седан до 2 млн").build() — читабельнее конструктора
@Builder
public class Chat {

    // === Первичный ключ ===

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // === Связь с пользователем ===

    // @ManyToOne — аннотация для связи "много к одному".
    // Много чатов принадлежат одному пользователю.
    //
    // fetch = FetchType.LAZY — НЕ загружать объект User из БД автоматически.
    // Hibernate загрузит User только когда явно вызовешь chat.getUser().
    // Это экономит ресурсы: если нужен только title чата — зачем тянуть все данные юзера?
    //
    // Правило: всегда начинай с LAZY. EAGER используй только если ВСЕГДА нужны данные связи.
    // В Mongoose аналог: по умолчанию LAZY, populate('user') — принудительная загрузка.
    @ManyToOne(fetch = FetchType.LAZY)
    // @JoinColumn — какая колонка в таблице chats является внешним ключом.
    // name = "user_id" — колонка в таблице chats (соответствует миграции V3).
    // nullable = false — чат ОБЯЗАН иметь пользователя (NOT NULL в БД).
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // === Основные поля ===

    // Заголовок чата. Nullable — при создании чата заголовка ещё нет.
    // Будет установлен после первого сообщения (этап 11).
    // В отличие от @Column(nullable = false), здесь нет аннотации — допустимо NULL.
    private String title;

    // === Временные метки ===

    // updatable = false — Hibernate никогда не обновит это поле через UPDATE.
    // Устанавливается один раз в @PrePersist и остаётся неизменным.
    // Аналог: createdAt в Mongoose с { immutable: true }.
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    // Обновляется при каждом изменении чата и при новом сообщении (через триггер в БД).
    // По updated_at сортируется список чатов пользователя (самый активный — первый).
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    // === Soft delete ===

    @Column(nullable = false)
    private boolean deleted;

    // === Lifecycle callbacks ===

    /**
     * Устанавливает значения по умолчанию перед первым сохранением (INSERT).
     *
     * @PrePersist — JPA вызывает этот метод ПЕРЕД INSERT.
     * Аналог Mongoose: pre('save', function() { ... })
     *
     * Почему не DEFAULT в @Column?
     * @Column(columnDefinition = "DEFAULT now()") работает только в DDL.
     * @PrePersist — работает всегда, даже если объект создан в памяти (без БД).
     * Нам нужно знать timestamps до сохранения (например, в тестах).
     */
    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
    }

    /**
     * Обновляет updatedAt при каждом изменении чата (UPDATE).
     *
     * @PreUpdate — JPA вызывает этот метод ПЕРЕД UPDATE.
     * Например: при изменении title чата.
     *
     * ВАЖНО: при отправке сообщения updated_at обновляется через PostgreSQL-триггер
     * (V4__create_messages_table.sql), а не через этот метод.
     * Это позволяет не загружать Chat entity только для обновления timestamp.
     */
    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}
