# Деплой и инфраструктура

> **Зависимости:** CONTEXT.md

Docker Compose для локальной разработки и развёртывания.

---

## Архитектура деплоя

```
┌─────────────────────────────────────────────────────────────────┐
│                         Docker Network                           │
│                                                                  │
│  ┌──────────┐    ┌──────────┐    ┌──────────┐                 │
│  │  nginx   │    │ backend  │    │ postgres │                 │
│  │  :80     │───▶│  :8080   │───▶│  :5432   │                 │
│  │          │    │          │    │          │                 │
│  └──────────┘    └──────────┘    └──────────┘                 │
│       │                │                                         │
│       │ serves static  │ HTTP API                                │
│       ▼                ▼                                         │
│  ┌──────────┐    ┌──────────────────────────────────────┐      │
│  │ frontend │    │      DeepSeek API (external)          │      │
│  │ (built)  │    │      https://api.deepseek.com        │      │
│  └──────────┘    └──────────────────────────────────────┘      │
└─────────────────────────────────────────────────────────────────┘
        │
        │ :80
        ▼
    Browser
```

---

## Docker Compose

### docker-compose.yml

```yaml
version: '3.8'

services:
  # Reverse proxy и статика
  nginx:
    image: nginx:alpine
    ports:
      - "80:80"
    volumes:
      - ./nginx/nginx.conf:/etc/nginx/nginx.conf:ro
      - ./ui/dist/ui/browser:/usr/share/nginx/html:ro
    depends_on:
      - backend
    networks:
      - carsai

  # Backend API
  backend:
    build:
      context: ./backend
      dockerfile: Dockerfile
    environment:
      - SPRING_PROFILES_ACTIVE=docker
      - DB_HOST=postgres
      - DB_PORT=5432
      - DB_NAME=carsai
      - DB_USERNAME=carsai
      - DB_PASSWORD=${DB_PASSWORD:-carsai}
      - JWT_SECRET=${JWT_SECRET:-your-256-bit-secret-key-here-change-in-production}
      - DEEPSEEK_API_KEY=${DEEPSEEK_API_KEY}
      - LLM_BASE_URL=https://api.deepseek.com
      - LLM_MODEL=${LLM_MODEL:-deepseek-chat}
    depends_on:
      postgres:
        condition: service_healthy
    networks:
      - carsai
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 3

  # Database
  postgres:
    image: postgres:16-alpine
    environment:
      - POSTGRES_DB=carsai
      - POSTGRES_USER=carsai
      - POSTGRES_PASSWORD=${DB_PASSWORD:-carsai}
    volumes:
      - postgres_data:/var/lib/postgresql/data
    networks:
      - carsai
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U carsai -d carsai"]
      interval: 10s
      timeout: 5s
      retries: 5

networks:
  carsai:
    driver: bridge

volumes:
  postgres_data:
```

---

## Конфигурация nginx

### nginx/nginx.conf

```nginx
events {
    worker_connections 1024;
}

http {
    include       /etc/nginx/mime.types;
    default_type  application/octet-stream;

    # Логирование
    log_format main '$remote_addr - $remote_user [$time_local] "$request" '
                    '$status $body_bytes_sent "$http_referer" '
                    '"$http_user_agent"';
    access_log /var/log/nginx/access.log main;

    sendfile on;
    keepalive_timeout 65;
    gzip on;
    gzip_types text/plain text/css application/json application/javascript;

    server {
        listen 80;
        server_name localhost;

        root /usr/share/nginx/html;
        index index.html;

        # API проксирование
        location /api/ {
            proxy_pass http://backend:8080/api/;
            proxy_http_version 1.1;
            proxy_set_header Host $host;
            proxy_set_header X-Real-IP $remote_addr;
            proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
            proxy_set_header X-Forwarded-Proto $scheme;
            
            # SSE поддержка
            proxy_set_header Connection '';
            proxy_buffering off;
            proxy_cache off;
            proxy_read_timeout 120s;
        }

        # Angular SPA
        location / {
            try_files $uri $uri/ /index.html;
        }

        # Кэширование статики
        location ~* \.(js|css|png|jpg|jpeg|gif|ico|svg|woff|woff2)$ {
            expires 1y;
            add_header Cache-Control "public, immutable";
        }
    }
}
```

---

## Dockerfile для Backend

### backend/Dockerfile

```dockerfile
# Build stage
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app

# Кэширование зависимостей
COPY gradle gradle
COPY gradlew build.gradle.kts settings.gradle.kts ./
RUN ./gradlew dependencies --no-daemon

# Сборка
COPY src src
RUN ./gradlew bootJar --no-daemon

# Runtime stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Создание non-root пользователя
RUN addgroup -g 1000 app && adduser -u 1000 -G app -D app
USER app

COPY --from=build /app/build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
```

---

## Профили Spring Boot

### application-docker.yml

```yaml
spring:
  datasource:
    url: jdbc:postgresql://${DB_HOST}:${DB_PORT}/${DB_NAME}
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}

llm:
  api-key: ${DEEPSEEK_API_KEY}
  base-url: ${LLM_BASE_URL}
  model: ${LLM_MODEL}

jwt:
  secret: ${JWT_SECRET}

server:
  port: 8080

management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics
```

---

## Скрипты запуска

### scripts/start.sh

```bash
#!/bin/bash
set -e

echo "🚗 Starting CarsAI..."

# Проверка Docker
if ! command -v docker &> /dev/null; then
    echo "❌ Docker not found. Please install Docker."
    exit 1
fi

# Сборка frontend
echo "📦 Building frontend..."
cd ui
npm ci
npm run build
cd ..

# Сборка backend (опционально, если не используем multi-stage)
# echo "📦 Building backend..."
# cd backend
# ./gradlew bootJar
# cd ..

# Запуск
echo "🐳 Starting containers..."
docker compose up -d

# Ожидание готовности
echo "⏳ Waiting for services..."
sleep 10

# Проверка наличия API ключа
if [ -z "$DEEPSEEK_API_KEY" ]; then
    echo "⚠️  Warning: DEEPSEEK_API_KEY not set. Set it in .env file or environment."
    echo "   Get your API key at: https://platform.deepseek.com/api_keys"
fi

echo "✅ CarsAI is running at http://localhost"
```

### scripts/stop.sh

```bash
#!/bin/bash
echo "🛑 Stopping CarsAI..."
docker compose down
echo "✅ Stopped"
```

### scripts/reset.sh

```bash
#!/bin/bash
echo "🗑️  Resetting CarsAI (removing all data)..."
docker compose down -v
echo "✅ Reset complete"
```

---

## Переменные окружения

### .env.example

```bash
# Database
DB_PASSWORD=your_secure_password

# JWT
JWT_SECRET=your-256-bit-secret-key-here-minimum-32-characters

# DeepSeek API
DEEPSEEK_API_KEY=your-deepseek-api-key-here
LLM_MODEL=deepseek-chat
```

---

## Требования к системе

### Минимальные

| Ресурс | Значение |
|--------|----------|
| CPU | 2 cores |
| RAM | 4 GB |
| Disk | 10 GB |
| OS | Linux, macOS, Windows (WSL2) |
| Network | Доступ к интернету (для DeepSeek API) |

### Рекомендуемые

| Ресурс | Значение |
|--------|----------|
| CPU | 4 cores |
| RAM | 8 GB |
| Disk | 20 GB SSD |
| Network | Стабильное интернет-соединение |

---

## Мониторинг

### Health checks

```bash
# Backend health
curl http://localhost/api/actuator/health

# Database
docker compose exec postgres pg_isready

# DeepSeek API (проверка доступности через backend health check)
curl http://localhost/api/actuator/health
```

### Логи

```bash
# Все сервисы
docker compose logs -f

# Конкретный сервис
docker compose logs -f backend
```

---

## Troubleshooting

### DeepSeek API недоступен

```bash
# Проверить API ключ
echo $DEEPSEEK_API_KEY

# Проверить логи backend для ошибок API
docker compose logs backend | grep -i "deepseek\|llm"

# Проверить доступность API
curl -H "Authorization: Bearer $DEEPSEEK_API_KEY" \
     https://api.deepseek.com/v1/models

# Если ошибка 401 - неверный API ключ
# Если ошибка 429 - превышен rate limit
```

### База данных не подключается

```bash
# Проверить логи
docker compose logs postgres

# Подключиться вручную
docker compose exec postgres psql -U carsai -d carsai
```

### Frontend не обновляется

```bash
# Пересобрать и перезапустить nginx
cd ui && npm run build && cd ..
docker compose restart nginx
```

---

## Примечания по DeepSeek API

### Получение API ключа

1. Зарегистрируйтесь на https://platform.deepseek.com
2. Перейдите в раздел API Keys
3. Создайте новый ключ
4. Добавьте ключ в `.env` файл: `DEEPSEEK_API_KEY=your-key-here`

### Rate Limiting

DeepSeek API имеет ограничения на количество запросов в зависимости от тарифа:
- Бесплатный тариф: ограниченное количество запросов
- Платные тарифы: более высокие лимиты

**Реализовано в backend:**
- ✅ Retry с exponential backoff при ошибке 429 (3 попытки: 1s, 2s, 4s)
- ✅ Логирование токенов для мониторинга затрат
- ✅ Метрики использования через Micrometer

**Рекомендации:**
- Мониторить метрики `chat.llm.tokens` для контроля расходов
- Настроить алерты при превышении лимитов токенов
- Рассмотреть кэширование для повторяющихся запросов (опционально для MVP)

### Безопасность

⚠️ **Важно:** Никогда не коммитьте API ключ в репозиторий!
- Используйте `.env` файл (добавлен в `.gitignore`)
- В production используйте секреты Docker/Kubernetes или переменные окружения
