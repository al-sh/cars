# Общие типы данных

Типы описаны в формате TypeScript для frontend и Java для backend.

---

## User

Пользователь системы.

**TypeScript:**
```typescript
interface User {
  id: string;
  email: string;
  name: string;
  role: 'client' | 'manager' | 'admin';
  created_at: string;  // ISO 8601
}
```

**Java:**
```java
public record UserDto(
    UUID id,
    String email,
    String name,
    UserRole role,
    Instant createdAt
) {}

public enum UserRole {
    CLIENT, MANAGER, ADMIN
}
```

---

## Chat

Диалог пользователя с ИИ.

**TypeScript:**
```typescript
interface Chat {
  id: string;
  user_id: string;
  title: string | null;
  created_at: string;
  updated_at: string;
  message_count: number;
}
```

**Java:**
```java
public record ChatDto(
    UUID id,
    UUID userId,
    String title,
    Instant createdAt,
    Instant updatedAt,
    int messageCount
) {}
```

---

## Message

Сообщение в чате.

**TypeScript:**
```typescript
interface Message {
  id: string;
  chat_id: string;
  role: 'user' | 'assistant' | 'system';
  content: string;
  created_at: string;
}
```

**Java:**
```java
public record MessageDto(
    UUID id,
    UUID chatId,
    MessageRole role,
    String content,
    Instant createdAt
) {}

public enum MessageRole {
    USER, ASSISTANT, SYSTEM
}
```

---

## Car

Автомобиль в справочнике.

**TypeScript:**
```typescript
interface Car {
  id: string;
  brand: string;
  model: string;
  year: number;
  price: number;
  body_type: BodyType;
  engine_type: EngineType;
  engine_volume: number;      // литры
  power_hp: number;           // лошадиные силы
  transmission: Transmission;
  drive: DriveType;
  seats: number;
  fuel_consumption: number;   // л/100км
  description: string | null;
  image_url: string | null;
}

type BodyType = 'sedan' | 'suv' | 'hatchback' | 'wagon' | 'minivan' | 'coupe' | 'pickup';
type EngineType = 'petrol' | 'diesel' | 'hybrid' | 'electric';
type Transmission = 'manual' | 'automatic' | 'robot' | 'cvt';
type DriveType = 'fwd' | 'rwd' | 'awd';
```

**Java:**
```java
public record CarDto(
    UUID id,
    String brand,
    String model,
    int year,
    int price,
    BodyType bodyType,
    EngineType engineType,
    BigDecimal engineVolume,
    int powerHp,
    Transmission transmission,
    DriveType drive,
    int seats,
    BigDecimal fuelConsumption,
    String description,
    String imageUrl
) {}

public enum BodyType {
    SEDAN, SUV, HATCHBACK, WAGON, MINIVAN, COUPE, PICKUP
}

public enum EngineType {
    PETROL, DIESEL, HYBRID, ELECTRIC
}

public enum Transmission {
    MANUAL, AUTOMATIC, ROBOT, CVT
}

public enum DriveType {
    FWD, RWD, AWD
}
```

---

## Пагинация

Общий формат ответа со списком.

**TypeScript:**
```typescript
interface PaginatedResponse<T> {
  items: T[];
  total: number;
  page: number;
  per_page: number;
}

// Для infinite scroll
interface CursorResponse<T> {
  items: T[];
  has_more: boolean;
}
```

**Java:**
```java
public record PagedResponse<T>(
    List<T> items,
    long total,
    int page,
    int perPage
) {}

public record CursorResponse<T>(
    List<T> items,
    boolean hasMore
) {}
```

---

## Auth

Типы для аутентификации.

**TypeScript:**
```typescript
interface AuthResponse {
  user: User;
  access_token: string;
  refresh_token: string;
}

interface LoginRequest {
  email: string;
  password: string;
}

interface RegisterRequest {
  email: string;
  password: string;
  name: string;
}
```

---

## Enums — справочные значения

Для использования в UI (селекты, фильтры):

```typescript
const BODY_TYPES = [
  { value: 'sedan', label: 'Седан' },
  { value: 'suv', label: 'Кроссовер/Внедорожник' },
  { value: 'hatchback', label: 'Хэтчбек' },
  { value: 'wagon', label: 'Универсал' },
  { value: 'minivan', label: 'Минивэн' },
  { value: 'coupe', label: 'Купе' },
  { value: 'pickup', label: 'Пикап' },
] as const;

const ENGINE_TYPES = [
  { value: 'petrol', label: 'Бензин' },
  { value: 'diesel', label: 'Дизель' },
  { value: 'hybrid', label: 'Гибрид' },
  { value: 'electric', label: 'Электро' },
] as const;

const TRANSMISSIONS = [
  { value: 'manual', label: 'Механика' },
  { value: 'automatic', label: 'Автомат' },
  { value: 'robot', label: 'Робот' },
  { value: 'cvt', label: 'Вариатор' },
] as const;

const DRIVE_TYPES = [
  { value: 'fwd', label: 'Передний' },
  { value: 'rwd', label: 'Задний' },
  { value: 'awd', label: 'Полный' },
] as const;
```
