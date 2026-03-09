#!/bin/sh
set -e

# BACKEND_HOST обязателен — без него некуда проксировать запросы
if [ -z "$BACKEND_HOST" ]; then
  echo "ERROR: BACKEND_HOST is not set" >&2
  exit 1
fi

# BACKEND_PORT — Railway задаёт через ${{backend.PORT}}.
# Дефолт 8080 совпадает с fallback-ом в application-railway.yml (${PORT:8080}).
export BACKEND_PORT="${BACKEND_PORT:-8080}"

# Подставляем переменные в шаблон nginx.
# Явный список '$PORT $BACKEND_HOST $BACKEND_PORT' гарантирует, что
# nginx-переменные ($host, $uri, $remote_addr и т.д.) остаются нетронутыми.
envsubst '$PORT $BACKEND_HOST $BACKEND_PORT' \
  < /etc/nginx/nginx.conf.template \
  > /etc/nginx/nginx.conf

exec nginx -g 'daemon off;'
