package com.carsai.back.user;

/**
 * Роли пользователей в системе.
 *
 * Enum в Java — это не просто набор строк (как в TypeScript),
 * а полноценный класс с фиксированным набором экземпляров.
 * Каждое значение (CLIENT, MANAGER, ADMIN) — это singleton-объект.
 *
 * Аналогия TypeScript:
 *   type UserRole = 'client' | 'manager' | 'admin';
 *
 * Аналогия Angular:
 *   В Angular мы бы создали union type или const enum.
 *   В Java enum — единственный правильный способ описать фиксированный набор значений.
 *
 * В БД хранится как строка ('CLIENT', 'MANAGER', 'ADMIN') благодаря
 * аннотации @Enumerated(EnumType.STRING) в Entity.
 *
 * При сериализации в JSON (для фронтенда) значения преобразуются
 * в lower-case: "client", "manager", "admin" — согласно контракту (types.md).
 * Это настраивается в DTO-слое (будет в этапе 4).
 */
public enum UserRole {
    CLIENT,   // Обычный пользователь — подбирает автомобили через чат
    MANAGER,  // Менеджер — может просматривать чаты клиентов
    ADMIN     // Администратор — полный доступ к системе
}
