import { Chat } from '../models/chat.model';
import { Message } from '../models/message.model';

export const MOCK_CHATS: Chat[] = [
  {
    id: '1',
    user_id: 'user-1',
    title: 'Поиск седана до 2 млн',
    created_at: '2024-01-15T10:00:00Z',
    updated_at: '2024-01-15T10:30:00Z',
    message_count: 5
  },
  {
    id: '2',
    user_id: 'user-1',
    title: 'Кроссовер для семьи',
    created_at: '2024-01-14T14:20:00Z',
    updated_at: '2024-01-14T15:45:00Z',
    message_count: 8
  },
  {
    id: '3',
    user_id: 'user-1',
    title: null,
    created_at: '2024-01-13T09:15:00Z',
    updated_at: '2024-01-13T09:25:00Z',
    message_count: 3
  },
  {
    id: '4',
    user_id: 'user-1',
    title: 'Электромобиль до 3 млн',
    created_at: '2024-01-12T16:30:00Z',
    updated_at: '2024-01-12T17:10:00Z',
    message_count: 12
  }
];

export const MOCK_MESSAGES: Record<string, Message[]> = {
  '1': [
    {
      id: 'm1',
      chat_id: '1',
      role: 'user',
      content: 'Ищу седан до 2 миллионов рублей. Что посоветуете?',
      created_at: '2024-01-15T10:00:00Z'
    },
    {
      id: 'm2',
      chat_id: '1',
      role: 'assistant',
      content: 'Отличный выбор! В этом ценовом диапазоне есть несколько интересных вариантов. Можете уточнить, какие характеристики важны: размер, мощность, комплектация?',
      created_at: '2024-01-15T10:01:00Z'
    },
    {
      id: 'm3',
      chat_id: '1',
      role: 'user',
      content: 'Нужен комфортный, с хорошей шумоизоляцией. Автомат обязателен.',
      created_at: '2024-01-15T10:05:00Z'
    },
    {
      id: 'm4',
      chat_id: '1',
      role: 'assistant',
      content: 'Рекомендую обратить внимание на Toyota Camry, Hyundai Sonata или Skoda Octavia. Все они имеют отличную шумоизоляцию и доступны с автоматической коробкой передач.',
      created_at: '2024-01-15T10:06:00Z'
    },
    {
      id: 'm5',
      chat_id: '1',
      role: 'user',
      content: 'Спасибо, изучу эти варианты!',
      created_at: '2024-01-15T10:30:00Z'
    }
  ],
  '2': [
    {
      id: 'm6',
      chat_id: '2',
      role: 'user',
      content: 'Нужен кроссовер для семьи с двумя детьми. Бюджет до 3 млн.',
      created_at: '2024-01-14T14:20:00Z'
    },
    {
      id: 'm7',
      chat_id: '2',
      role: 'assistant',
      content: 'Для семьи с детьми важны безопасность, просторный салон и багажник. Какие марки предпочитаете?',
      created_at: '2024-01-14T14:21:00Z'
    },
    {
      id: 'm8',
      chat_id: '2',
      role: 'user',
      content: 'Рассматриваю корейские и японские бренды.',
      created_at: '2024-01-14T14:25:00Z'
    },
    {
      id: 'm9',
      chat_id: '2',
      role: 'assistant',
      content: 'Отличный выбор! Рекомендую: Hyundai Tucson, Kia Sportage, Toyota RAV4 или Honda CR-V. Все модели имеют просторный салон, хорошую безопасность и подходят для семейных поездок.',
      created_at: '2024-01-14T14:26:00Z'
    },
    {
      id: 'm10',
      chat_id: '2',
      role: 'user',
      content: 'А что насчет расхода топлива?',
      created_at: '2024-01-14T15:00:00Z'
    },
    {
      id: 'm11',
      chat_id: '2',
      role: 'assistant',
      content: 'Средний расход у этих моделей: 8-10 л/100км в смешанном цикле. Если важна экономичность, рассмотрите гибридные версии Toyota RAV4 или Hyundai Tucson Hybrid - расход снижается до 5-6 л/100км.',
      created_at: '2024-01-14T15:01:00Z'
    },
    {
      id: 'm12',
      chat_id: '2',
      role: 'user',
      content: 'Спасибо за подробный ответ!',
      created_at: '2024-01-14T15:10:00Z'
    },
    {
      id: 'm13',
      chat_id: '2',
      role: 'assistant',
      content: 'Пожалуйста! Если возникнут дополнительные вопросы - обращайтесь.',
      created_at: '2024-01-14T15:45:00Z'
    }
  ],
  '3': [
    {
      id: 'm14',
      chat_id: '3',
      role: 'user',
      content: 'Привет!',
      created_at: '2024-01-13T09:15:00Z'
    },
    {
      id: 'm15',
      chat_id: '3',
      role: 'assistant',
      content: 'Здравствуйте! Чем могу помочь?',
      created_at: '2024-01-13T09:16:00Z'
    },
    {
      id: 'm16',
      chat_id: '3',
      role: 'user',
      content: 'Пока просто изучаю возможности',
      created_at: '2024-01-13T09:25:00Z'
    }
  ],
  '4': [
    {
      id: 'm17',
      chat_id: '4',
      role: 'user',
      content: 'Интересуют электромобили. Бюджет до 3 миллионов.',
      created_at: '2024-01-12T16:30:00Z'
    },
    {
      id: 'm18',
      chat_id: '4',
      role: 'assistant',
      content: 'Отличный выбор! Электромобили становятся всё популярнее. Какие требования к запасу хода и зарядке?',
      created_at: '2024-01-12T16:31:00Z'
    },
    {
      id: 'm19',
      chat_id: '4',
      role: 'user',
      content: 'Нужен запас хода минимум 400 км. Быстрая зарядка обязательна.',
      created_at: '2024-01-12T16:35:00Z'
    },
    {
      id: 'm20',
      chat_id: '4',
      role: 'assistant',
      content: 'В этом диапазоне рекомендую: Tesla Model 3, Hyundai IONIQ 5, Kia EV6 или BYD Atto 3. Все поддерживают быструю зарядку и имеют запас хода от 400 км.',
      created_at: '2024-01-12T16:36:00Z'
    },
    {
      id: 'm21',
      chat_id: '4',
      role: 'user',
      content: 'А что с инфраструктурой зарядки в России?',
      created_at: '2024-01-12T16:50:00Z'
    },
    {
      id: 'm22',
      chat_id: '4',
      role: 'assistant',
      content: 'Инфраструктура активно развивается. В крупных городах уже достаточно зарядных станций. Многие производители предлагают установку домашней зарядной станции. Также есть сети быстрых зарядок вдоль основных трасс.',
      created_at: '2024-01-12T16:51:00Z'
    },
    {
      id: 'm23',
      chat_id: '4',
      role: 'user',
      content: 'Спасибо, это важно знать!',
      created_at: '2024-01-12T17:00:00Z'
    },
    {
      id: 'm24',
      chat_id: '4',
      role: 'assistant',
      content: 'Рад помочь! Если нужна дополнительная информация по конкретным моделям - спрашивайте.',
      created_at: '2024-01-12T17:01:00Z'
    },
    {
      id: 'm25',
      chat_id: '4',
      role: 'user',
      content: 'Можно подробнее про Tesla Model 3?',
      created_at: '2024-01-12T17:05:00Z'
    },
    {
      id: 'm26',
      chat_id: '4',
      role: 'assistant',
      content: 'Tesla Model 3 - один из самых популярных электромобилей. Запас хода до 629 км (Long Range), разгон до 100 км/ч за 3.3 секунды (Performance), автопилот, минималистичный интерьер. Цена в России начинается от 2.5 млн рублей.',
      created_at: '2024-01-12T17:06:00Z'
    },
    {
      id: 'm27',
      chat_id: '4',
      role: 'user',
      content: 'Звучит интересно. А что с обслуживанием?',
      created_at: '2024-01-12T17:08:00Z'
    },
    {
      id: 'm28',
      chat_id: '4',
      role: 'assistant',
      content: 'Tesla имеет собственную сеть сервисных центров. Обслуживание проще, чем у ДВС - нет масла, фильтров, свечей. Основные расходы: шины, тормозные колодки (которые изнашиваются медленнее благодаря рекуперации), и периодическая замена салонного фильтра.',
      created_at: '2024-01-12T17:09:00Z'
    }
  ]
};
