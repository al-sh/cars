# Деплой и инфраструктура

> **Зависимости:** CONTEXT.md

Docker Compose для локальной разработки и развёртывания.

---

## Архитектура деплоя

```
┌─────────────────────────────────────────────────────────────────┐
│                         Docker Network                           │
│                                                                  │
│  ┌──────────┐    ┌──────────┐    ┌──────────┐    ┌──────────┐  │
│  │  nginx   │    │ backend  │    │ postgres │    │  ollama  │  │
│  │  :80     │───▶│  :8080   │───▶│  :5432   │    │  :11434  │  │
│  │          │    │          │───────────────────▶│          │  │
│  └──────────┘    └──────────┘    └──────────┘    └──────────┘  │
│       │                                                         │
│       │ serves static                                           │
│       ▼                                                         │
│  ┌──────────┐                                                   │
│  │ frontend │                                                   │
│  │ (built)  │                                                   │
│  └──────────┘                                                   │
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
      - LLM_BASE_URL=http://ollama:11434
    depends_on:
      postgres:
        condition: service_healthy
      ollama:
        condition: service_started
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

  # LLM
  ollama:
    image: ollama/ollama:latest
    volumes:
      - ollama_data:/root/.ollama
    networks:
      - carsai
    deploy:
      resources:
        reservations:
          memory: 8G

networks:
  carsai:
    driver: bridge

volumes:
  postgres_data:
  ollama_data:
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
  base-url: ${LLM_BASE_URL}

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

# Загрузка модели Ollama
echo "🤖 Pulling LLM model..."
docker compose exec ollama ollama pull qwen2.5:7b

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

# LLM
LLM_MODEL=qwen2.5:7b
```

---

## Требования к системе

### Минимальные

| Ресурс | Значение |
|--------|----------|
| CPU | 4 cores |
| RAM | 16 GB |
| Disk | 20 GB |
| OS | Linux, macOS, Windows (WSL2) |

### Рекомендуемые

| Ресурс | Значение |
|--------|----------|
| CPU | 8 cores |
| RAM | 32 GB |
| Disk | 50 GB SSD |
| GPU | NVIDIA 8GB+ (для ускорения LLM) |

---

## Мониторинг

### Health checks

```bash
# Backend health
curl http://localhost/api/actuator/health

# Database
docker compose exec postgres pg_isready

# Ollama
curl http://localhost:11434/api/tags
```

### Логи

```bash
# Все сервисы
docker compose logs -f

# Конкретный сервис
docker compose logs -f backend
docker compose logs -f ollama
```

---

## Troubleshooting

### Ollama не отвечает

```bash
# Проверить статус
docker compose ps ollama

# Перезапустить
docker compose restart ollama

# Проверить модель
docker compose exec ollama ollama list
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

## GPU поддержка (опционально)

### docker-compose.gpu.yml

```yaml
version: '3.8'

services:
  ollama:
    image: ollama/ollama:latest
    volumes:
      - ollama_data:/root/.ollama
    networks:
      - carsai
    deploy:
      resources:
        reservations:
          devices:
            - driver: nvidia
              count: 1
              capabilities: [gpu]
```

```bash
# Запуск с GPU
docker compose -f docker-compose.yml -f docker-compose.gpu.yml up -d
```
