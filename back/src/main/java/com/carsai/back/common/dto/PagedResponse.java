package com.carsai.back.common.dto;

import java.util.List;
import java.util.function.Function;

import org.springframework.data.domain.Page;

/**
 * Обёртка для ответов с постраничной пагинацией (offset-based).
 *
 * Используется для списков с известным общим количеством элементов,
 * где пользователь переключает страницы (как в Google).
 * Например: список чатов.
 *
 * Параметризован дженериком T — может содержать любой тип DTO:
 * PagedResponse<ChatDto>, PagedResponse<UserDto> и т.д.
 *
 * Пример JSON:
 * {
 *   "items": [{ "id": "...", "title": "Подбор кроссовера" }, ...],
 *   "total": 42,
 *   "page": 1,
 *   "perPage": 20
 * }
 *
 * @param items   элементы текущей страницы
 * @param total   общее количество элементов (для расчёта количества страниц)
 * @param page    текущая страница (начинается с 1, не с 0)
 * @param perPage количество элементов на странице
 */
public record PagedResponse<T>(
        List<T> items,
        long total,
        int page,
        int perPage
) {

    /**
     * Фабричный метод: конвертирует Spring Page<Entity> → PagedResponse<DTO>.
     *
     * Spring Data возвращает Page<Chat> из Repository. Нам нужно:
     * 1. Конвертировать каждый Entity в DTO (Chat → ChatDto)
     * 2. Обернуть в наш формат ответа (PagedResponse)
     *
     * mapper — функция конвертации Entity → DTO.
     * Передаётся как method reference: PagedResponse.from(page, ChatDto::from)
     *
     * Пример использования:
     *   Page<Chat> chats = chatRepository.findAll(spec, pageable);
     *   return PagedResponse.from(chats, ChatDto::from);
     *
     * @param page   результат Spring Data JPA запроса с пагинацией
     * @param mapper функция конвертации Entity → DTO
     * @return PagedResponse с DTO и метаданными пагинации
     */
    public static <E, D> PagedResponse<D> from(Page<E> page, Function<E, D> mapper) {
        return new PagedResponse<>(
                page.getContent().stream().map(mapper).toList(),
                page.getTotalElements(),
                // Spring считает страницы с 0, наш API — с 1.
                // page.getNumber() возвращает 0-based индекс, добавляем 1.
                page.getNumber() + 1,
                page.getSize()
        );
    }
}
