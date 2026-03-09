# Деплой на Railway

## Архитектура

```
Internet → Railway HTTPS → Frontend (nginx)
                               ├── /        → Angular SPA (статика)
                               └── /api/    → backend.railway.internal:PORT
                                                    ↓
                                              Spring Boot
                                                    ↓
                                              PostgreSQL (Railway managed)
                                                    ↓
                                              DeepSeek API (external)
```

---

## Первоначальная настройка

### 1. Создать проект

1. Зайти на [railway.app](https://railway.app) → **New Project → Deploy from GitHub repo**
2. Выбрать репозиторий `cars`
3. Railway создаст первый сервис автоматически — это будет backend

### 2. Добавить PostgreSQL

В проекте: **New → Database → Add PostgreSQL**

Railway автоматически создаст переменные `PGHOST`, `PGPORT`, `PGDATABASE`, `PGUSER`, `PGPASSWORD`.

### 3. Настроить Backend-сервис

В сервисе → **Settings**:

| Параметр | Значение |
|----------|----------|
| Service Name | `backend` |
| Root Directory | `back` |
| Dockerfile Path | `Dockerfile` |
| Watch Paths | `back/**` |

В сервисе → **Variables**:

| Переменная | Значение |
|------------|----------|
| `SPRING_PROFILES_ACTIVE` | `railway` |
| `JWT_SECRET` | *(сгенерировать: `openssl rand -base64 32`)* |
| `DEEPSEEK_API_KEY` | *(ключ с platform.deepseek.com)* |

PostgreSQL-переменные добавить через **Add Reference → PostgreSQL** (выбрать все PG* переменные).

### 4. Добавить Frontend-сервис

В проекте: **New → GitHub Repo** → тот же репозиторий. В **Settings**:

| Параметр | Значение |
|----------|----------|
| Service Name | `frontend` |
| Root Directory | *(пусто — корень)* |
| Dockerfile Path | `front/Dockerfile.railway` |
| Watch Paths | `front/**`, `nginx/**` |

В **Variables**:

| Переменная | Значение |
|------------|----------|
| `BACKEND_HOST` | `${{backend.RAILWAY_PRIVATE_DOMAIN}}` |
| `BACKEND_PORT` | `${{backend.PORT}}` |

`PORT` Railway проставляет автоматически — вручную не задавать.

---

## Ветки и окружения

Railway поддерживает **environments** — отдельные окружения для разных веток.

### Связать ветку с окружением

1. В проекте → **Environments** → **New Environment** (например, `staging`)
2. В каждом сервисе → **Settings → Source → Branch** → выбрать нужную ветку

Типовая схема:

| Ветка | Окружение Railway | URL |
|-------|-------------------|-----|
| `main` | `production` | `https://yourapp.up.railway.app` |
| `develop` | `staging` | `https://yourapp-staging.up.railway.app` |

Каждое окружение имеет свои переменные и свою PostgreSQL — данные не смешиваются.

---

## Запуск деплоя

### Автоматически

При пуше в отслеживаемую ветку Railway запускает деплой автоматически.

```bash
git push origin main        # → деплой в production
git push origin develop     # → деплой в staging
```

### Вручную из Railway dashboard

1. Открыть сервис → вкладка **Deployments**
2. Нажать **Deploy** (или **Redeploy** для последнего коммита)

### Вручную через Railway CLI

```bash
# Установить CLI
npm install -g @railway/cli

# Войти
railway login

# Привязать проект (из корня репозитория)
railway link

# Задеплоить текущую ветку
railway up

# Задеплоить конкретный сервис
railway up --service backend
railway up --service frontend
```

---

## Переменные окружения

Изменение переменных в Railway dashboard **автоматически перезапускает** сервис — новый деплой не нужен.

Для локальной разработки использовать `.env` (см. `.env.example`).

---

## Логи и мониторинг

```bash
# Через CLI
railway logs --service backend
railway logs --service frontend

# Health check backend
curl https://<your-backend-url>/actuator/health
```

В dashboard: сервис → вкладка **Logs** — логи в реальном времени.

---

## Файлы деплоя

| Файл | Назначение |
|------|------------|
| `back/Dockerfile` | Сборка backend (Maven multi-stage) |
| `front/Dockerfile` | Сборка frontend для локального docker-compose |
| `front/Dockerfile.railway` | Сборка frontend для Railway (с envsubst) |
| `front/nginx.conf.template` | Nginx-конфиг с плейсхолдерами для Railway |
| `nginx/nginx.conf` | Nginx-конфиг для локального docker-compose |
| `docker-compose.yml` | Локальный запуск всего стека |
| `back/src/main/resources/application-railway.yml` | Spring Boot профиль для Railway |
| `back/src/main/resources/application-docker.yml` | Spring Boot профиль для docker-compose |
| `.env.example` | Шаблон переменных окружения |
