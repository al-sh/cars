package com.carsai.back.common.dto;

import java.util.List;

/**
 * Обёртка для ответов с курсорной пагинацией (cursor-based).
 *
 * Используется для бесконечного скролла (infinite scroll), где нет
 * переключения страниц — только "загрузить ещё".
 * Например: сообщения в чате.
 *
 * Отличие от PagedResponse:
 * - PagedResponse: знаем total, перемещаемся по страницам (1, 2, 3...).
 *   Подходит для списков конечного размера.
 * - CursorResponse: не знаем total, только "есть ли ещё?" (hasMore).
 *   Подходит для потоков данных (чаты, лента, логи).
 *
 * Почему cursor лучше для чатов:
 * Обычная пагинация (page=2, perPage=20) ломается в реальном времени:
 * пока ты смотришь страницу 2, кто-то добавил сообщение — элементы сдвинулись.
 * Cursor-based: "покажи 20 сообщений до ID X" — стабильно, не зависит от новых данных.
 * Используется в Telegram, WhatsApp, Slack.
 *
 * Аналогия TypeScript (из контракта types.md):
 *   interface CursorResponse<T> {
 *     items: T[];
 *     hasMore: boolean;
 *   }
 *
 * Пример JSON:
 * {
 *   "items": [{ "id": "...", "content": "Привет!", "role": "user" }, ...],
 *   "hasMore": true
 * }
 *
 * @param items   элементы текущей "порции"
 * @param hasMore есть ли ещё элементы для загрузки
 */
public record CursorResponse<T>(
        List<T> items,
        boolean hasMore
) {}
