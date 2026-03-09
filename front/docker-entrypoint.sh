#!/bin/sh
set -e

# Проверяем, что обязательные переменные заданы
if [ -z "$BACKEND_HOST" ]; then
  echo "ERROR: BACKEND_HOST is not set" >&2
  exit 1
fi
if [ -z "$BACKEND_PORT" ]; then
  echo "ERROR: BACKEND_PORT is not set" >&2
  exit 1
fi

# Подставляем переменные в шаблон nginx.
# Явный список '$PORT $BACKEND_HOST $BACKEND_PORT' гарантирует, что
# nginx-переменные ($host, $uri, $remote_addr и т.д.) остаются нетронутыми.
envsubst '$PORT $BACKEND_HOST $BACKEND_PORT' \
  < /etc/nginx/nginx.conf.template \
  > /etc/nginx/nginx.conf

exec nginx -g 'daemon off;'
