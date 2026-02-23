package com.carsai.back.chat;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;

import com.carsai.back.chat.dto.ChatDto;
import com.carsai.back.common.dto.PagedResponse;
import com.carsai.back.common.exception.ChatNotFoundException;
import com.carsai.back.user.User;
import com.carsai.back.user.UserRepository;
import com.carsai.back.user.UserRole;

/**
 * Unit-тесты для ChatService — CRUD операции и проверка владельца чата.
 *
 * Ключевые сценарии:
 * 1. Пользователь может видеть/изменять только СВОИ чаты
 * 2. Soft delete: deleted = true, физического удаления нет
 * 3. ChatNotFoundException при доступе к чужому/несуществующему чату
 *
 * Все зависимости замокированы — тест проверяет только логику ChatService,
 * а не SQL-запросы или HTTP-слой.
 */
@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    @Mock
    private ChatRepository chatRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ChatService chatService;

    // ===== createChat() тесты =====

    @Test
    void createChat_shouldSaveWithTitle() {
        // given
        UUID userId = UUID.randomUUID();
        String title = "Седан до 2 млн";

        User userRef = User.builder()
                .id(userId).email("u@t.com").name("U").role(UserRole.CLIENT).build();

        // getReferenceById() — возвращает прокси объект User без SQL-запроса.
        // Нам нужен только для установки связи @ManyToOne в Chat.
        when(userRepository.getReferenceById(userId)).thenReturn(userRef);
        when(chatRepository.save(any(Chat.class))).thenAnswer(inv -> {
            // Симулируем @GeneratedValue и @PrePersist из Chat entity
            Chat saved = inv.getArgument(0);
            saved.setId(UUID.randomUUID());
            saved.setCreatedAt(Instant.now());
            saved.setUpdatedAt(Instant.now());
            return saved;
        });

        // when
        ChatDto result = chatService.createChat(userId, title);

        // then
        assertThat(result.title()).isEqualTo("Седан до 2 млн");
        assertThat(result.userId()).isEqualTo(userId);
        // Новый чат всегда имеет 0 сообщений (оптимизация: не делаем COUNT-запрос)
        assertThat(result.messageCount()).isEqualTo(0);
        verify(chatRepository).save(any(Chat.class));
    }

    @Test
    void createChat_shouldSaveWithNullTitle() {
        // given — title не передан (чат создаётся без заголовка, он появится позже)
        UUID userId = UUID.randomUUID();
        User userRef = User.builder()
                .id(userId).email("u@t.com").name("U").role(UserRole.CLIENT).build();

        when(userRepository.getReferenceById(userId)).thenReturn(userRef);
        when(chatRepository.save(any(Chat.class))).thenAnswer(inv -> {
            Chat saved = inv.getArgument(0);
            saved.setId(UUID.randomUUID());
            saved.setCreatedAt(Instant.now());
            saved.setUpdatedAt(Instant.now());
            return saved;
        });

        // when
        ChatDto result = chatService.createChat(userId, null);

        // then — title допускает null
        assertThat(result.title()).isNull();
    }

    // ===== updateChat() тесты =====

    @Test
    void updateChat_shouldUpdateTitle() {
        // given
        UUID userId = UUID.randomUUID();
        UUID chatId = UUID.randomUUID();

        User user = User.builder()
                .id(userId).email("u@t.com").name("U").role(UserRole.CLIENT).build();
        Chat chat = Chat.builder()
                .id(chatId).user(user).title("Старый заголовок")
                .createdAt(Instant.now()).updatedAt(Instant.now()).deleted(false)
                .build();

        when(chatRepository.findByIdAndUserIdAndDeletedFalse(chatId, userId))
                .thenReturn(Optional.of(chat));
        when(chatRepository.save(any(Chat.class))).thenReturn(chat);
        when(chatRepository.countMessagesByChatId(chatId)).thenReturn(5);

        // when
        ChatDto result = chatService.updateChat(userId, chatId, "Новый заголовок");

        // then
        assertThat(result.title()).isEqualTo("Новый заголовок");
        assertThat(result.messageCount()).isEqualTo(5);
    }

    @Test
    void updateChat_shouldThrow_whenChatNotFound() {
        // given — чат не существует ИЛИ принадлежит другому пользователю
        // В обоих случаях findByIdAndUserIdAndDeletedFalse возвращает empty.
        // Этот метод — главная защита от доступа к чужим чатам.
        UUID userId = UUID.randomUUID();
        UUID chatId = UUID.randomUUID();

        when(chatRepository.findByIdAndUserIdAndDeletedFalse(chatId, userId))
                .thenReturn(Optional.empty());

        // when & then — ChatNotFoundException → GlobalExceptionHandler → 404
        assertThatThrownBy(() -> chatService.updateChat(userId, chatId, "Новый заголовок"))
                .isInstanceOf(ChatNotFoundException.class);

        // Пользователь не должен обновить чужой чат
        verify(chatRepository, never()).save(any());
    }

    // ===== deleteChat() тесты =====

    @Test
    void deleteChat_shouldSetDeletedTrue() {
        // given
        UUID userId = UUID.randomUUID();
        UUID chatId = UUID.randomUUID();

        User user = User.builder()
                .id(userId).email("u@t.com").name("U").role(UserRole.CLIENT).build();
        Chat chat = Chat.builder()
                .id(chatId).user(user).title("Чат")
                .createdAt(Instant.now()).updatedAt(Instant.now()).deleted(false)
                .build();

        when(chatRepository.findByIdAndUserIdAndDeletedFalse(chatId, userId))
                .thenReturn(Optional.of(chat));
        when(chatRepository.save(any(Chat.class))).thenReturn(chat);

        // when
        chatService.deleteChat(userId, chatId);

        // then — soft delete: физически чат не удаляется, только помечается
        // Реальный SQL: UPDATE chats SET deleted = true WHERE id = ?
        assertThat(chat.isDeleted()).isTrue();
        verify(chatRepository).save(chat);
    }

    @Test
    void deleteChat_shouldThrow_whenChatNotFound() {
        // given — нельзя удалить чужой или несуществующий чат
        UUID userId = UUID.randomUUID();
        UUID chatId = UUID.randomUUID();

        when(chatRepository.findByIdAndUserIdAndDeletedFalse(chatId, userId))
                .thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> chatService.deleteChat(userId, chatId))
                .isInstanceOf(ChatNotFoundException.class);

        verify(chatRepository, never()).save(any());
    }

    // ===== getUserChats() тесты =====

    @Test
    @SuppressWarnings("unchecked")
    void getUserChats_shouldReturnPagedResult() {
        // given
        UUID userId = UUID.randomUUID();
        User user = User.builder()
                .id(userId).email("u@t.com").name("U").role(UserRole.CLIENT).build();
        Chat chat = Chat.builder()
                .id(UUID.randomUUID()).user(user).title("Тестовый чат")
                .createdAt(Instant.now()).updatedAt(Instant.now()).deleted(false)
                .build();

        // Specification — динамический WHERE-строитель. Внутри getUserChats() он
        // создаётся как лямбда, поэтому точно сравнить не можем — используем any().
        //
        // PageImpl — реализация Page<T> для тестов.
        // Конструктор PageImpl(list) — один элемент, page=0, size=1.
        // После PagedResponse.from() → page=1 (конвертируем из 0-based в 1-based).
        when(chatRepository.findAll(any(Specification.class), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(chat)));
        when(chatRepository.countMessagesByChatId(chat.getId())).thenReturn(3);

        // when
        PagedResponse<ChatDto> result = chatService.getUserChats(userId, 1, 20, null);

        // then
        assertThat(result.items()).hasSize(1);
        assertThat(result.items().get(0).title()).isEqualTo("Тестовый чат");
        assertThat(result.items().get(0).messageCount()).isEqualTo(3);
        assertThat(result.total()).isEqualTo(1);
    }

    @Test
    @SuppressWarnings("unchecked")
    void getUserChats_shouldFilterBySearch() {
        // given — поисковый запрос передан, Specification дополнится LIKE-условием
        UUID userId = UUID.randomUUID();

        // Возвращаем пустой список — поиск "кроссовер" не нашёл совпадений
        when(chatRepository.findAll(any(Specification.class), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of()));

        // when
        PagedResponse<ChatDto> result = chatService.getUserChats(userId, 1, 20, "кроссовер");

        // then
        assertThat(result.items()).isEmpty();
        assertThat(result.total()).isEqualTo(0);
        // Важно: findAll должен быть вызван — search не должен пропускать запрос
        verify(chatRepository).findAll(any(Specification.class), any(PageRequest.class));
    }
}
