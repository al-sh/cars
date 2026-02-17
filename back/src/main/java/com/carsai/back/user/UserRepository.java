package com.carsai.back.user;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository для доступа к таблице users через Spring Data JPA.
 *
 * Это ИНТЕРФЕЙС, а не класс. Spring Data сам генерирует реализацию при запуске.
 * Ты описываешь ЧТО хочешь получить (сигнатуру метода), а Spring пишет КАК (SQL).
 *
 * JpaRepository<User, UUID> — дженерик:
 * - User — с какой Entity работаем
 * - UUID — тип первичного ключа (поле id в User)
 *
 * Бесплатно получаем ~20 готовых методов:
 * - save(User entity)          → INSERT или UPDATE (если id уже есть)
 * - findById(UUID id)          → SELECT * FROM users WHERE id = ?
 * - findAll()                  → SELECT * FROM users
 * - delete(User entity)        → DELETE FROM users WHERE id = ?
 * - count()                    → SELECT COUNT(*) FROM users
 * - existsById(UUID id)        → SELECT COUNT(*) > 0 FROM users WHERE id = ?
 *
 *
 * ВАЖНО: Repository — это самый нижний слой архитектуры.
 * Controller → Service → Repository → PostgreSQL
 * Repository не содержит бизнес-логику — только доступ к данным.
 */
@Repository // Помечает интерфейс как Spring-компонент для доступа к данным.
            // Spring создаст реализацию (proxy-класс) и зарегистрирует в DI-контейнере.
            // В Angular аналог: @Injectable({ providedIn: 'root' }) — сервис доступен везде.
            // Технически @Repository можно опустить для JpaRepository (Spring Data
            // сканирует интерфейсы автоматически), но оставляем для явности.
public interface UserRepository extends JpaRepository<User, UUID> {

    /**
     * Найти пользователя по email.
     *
     * Spring Data разбирает имя метода по правилам:
     * find + By + Email → SELECT * FROM users WHERE email = ?
     *
     * Возвращает Optional<User> вместо User, потому что пользователь может
     * не существовать. Optional заставляет вызывающий код явно обработать
     * случай "не нашли" — это безопаснее, чем возвращать null.
     *
     *
     * @param email email для поиска (передаётся как параметр в WHERE)
     * @return Optional с пользователем, или пустой Optional если не найден
     */
    Optional<User> findByEmail(String email);

    /**
     * Проверить, существует ли пользователь с таким email.
     *
     * Spring Data: exists + By + Email → SELECT COUNT(*) > 0 FROM users WHERE email = ?
     * Возвращает примитив boolean (не нужен весь объект User — только факт существования).
     *
     * Используется при регистрации: проверяем уникальность email ДО попытки создания.
     * Быстрее, чем findByEmail() + проверка на пустоту, т.к. не загружает данные.
     *
     * @param email email для проверки
     * @return true если пользователь с таким email уже есть в БД
     */
    boolean existsByEmail(String email);
}
