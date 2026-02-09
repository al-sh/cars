package com.carsai.back.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Entity-класс пользователя. Маппится на таблицу "users" в PostgreSQL.
 *
 * Entity — это Java-класс, который JPA (Hibernate) связывает с таблицей в БД.
 * Каждое поле класса = колонка таблицы. Каждый объект = строка в таблице.
 * Hibernate автоматически конвертирует между Java-объектами и SQL.
 *
 * Аналогия Node.js:
 * - Prisma: model User { id String @id @default(uuid()) ... }
 * - Mongoose: new Schema({ email: { type: String, required: true } })
 * - TypeORM: @Entity() class User { @PrimaryGeneratedColumn("uuid") id: string; }
 *
 * Аналогия Angular:
 * - Это как тип User из models/user.model.ts, но с аннотациями,
 *   которые говорят Hibernate, как маппить на БД.
 *
 * ВАЖНО: Entity — это внутренний объект приложения. Наружу (в API) отдаём DTO.
 * Entity содержит password_hash и другие чувствительные данные.
 * DTO (Data Transfer Object) содержит только то, что нужно клиенту.
 * Entity ≈ полная модель, DTO ≈ проекция (view model).
 */
@Entity
// @Table — задаёт имя таблицы. Без этой аннотации Hibernate использовал бы имя класса ("User"),
// но "user" — зарезервированное слово в SQL, поэтому явно указываем "users".
@Table(name = "users")
// @Data — Lombok-аннотация, генерирует при компиляции:
// - getters для всех полей (getId(), getEmail(), ...)
// - setters для всех полей (setId(), setEmail(), ...)
// - toString() — для логирования
// - equals() и hashCode() — для сравнения объектов
// В JavaScript/TypeScript эта проблема не существует — свойства доступны напрямую.
// Lombok компенсирует многословность Java.
@Data
// @NoArgsConstructor — конструктор без аргументов: new User()
// JPA ТРЕБУЕТ конструктор без аргументов для создания объектов через рефлексию.
// Когда Hibernate загружает строку из БД, он делает: User user = new User(); user.setEmail("...");
@NoArgsConstructor
// @AllArgsConstructor — конструктор со всеми аргументами: new User(id, email, name, ...)
// Полезен для тестов и ручного создания объектов.
@AllArgsConstructor
// @Builder — паттерн Builder для удобного создания:
// User.builder().email("a@b.com").name("Иван").build()
// Читабельнее, чем конструктор с 7 параметрами.
// В JS аналог — передача объекта: new User({ email: "a@b.com", name: "Иван" })
@Builder
public class User {

    // === Первичный ключ ===

    // @Id — помечает поле как Primary Key.
    // Каждая Entity обязана иметь @Id.
    @Id
    // @GeneratedValue — БД сама генерирует значение при INSERT.
    // GenerationType.UUID — генерация UUID.
    // В PostgreSQL это работает через DEFAULT gen_random_uuid() в миграции.
    // Hibernate при INSERT не передаёт id — PostgreSQL сам подставляет UUID.
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // === Основные поля ===

    // @Column — настройки колонки в таблице.
    // nullable = false — NOT NULL constraint (Hibernate проверит перед INSERT).
    // unique = true — UNIQUE constraint (дублирует uk_users_email из миграции,
    // но позволяет Hibernate валидировать схему при ddl-auto: validate).
    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String name;

    // Хеш пароля. Колонка в БД называется "password_hash" (snake_case),
    // а поле в Java — "passwordHash" (camelCase).
    // Hibernate автоматически конвертирует camelCase → snake_case
    // благодаря стратегии именования по умолчанию (SpringPhysicalNamingStrategy).
    // Пароль НИКОГДА не хранится в открытом виде — только BCrypt хеш.
    @Column(nullable = false)
    private String passwordHash;

    // === Роль ===

    // @Enumerated(EnumType.STRING) — хранить enum как строку в БД ('CLIENT', 'MANAGER', 'ADMIN').
    // Без этой аннотации Hibernate сохранял бы ПОРЯДКОВЫЙ НОМЕР (0, 1, 2),
    // что ломается при изменении порядка значений в enum.
    // ВСЕГДА используй EnumType.STRING — это безопасно и читаемо в БД.
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role;

    // === Метаданные ===

    // Время создания пользователя.
    // Instant — Java-тип для момента времени в UTC.
    // Маппится на TIMESTAMPTZ в PostgreSQL.
    // Аналог Date в JavaScript или dayjs/moment объекта.
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    // Soft delete флаг.
    // false — активный пользователь, true — удалён.
    // Вместо DELETE FROM users WHERE id = ? делаем UPDATE users SET deleted = true WHERE id = ?
    // Все запросы к "живым" пользователям фильтруют: WHERE deleted = false
    @Column(nullable = false)
    private boolean deleted;
}
