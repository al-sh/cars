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

# Читаем DNS-резолвер из /etc/resolv.conf — единственный надёжный способ
# получить актуальный адрес в любом контейнерном окружении (Railway, Docker и т.д.).
_resolver=$(awk '/^nameserver/{print $2; exit}' /etc/resolv.conf)
if [ -z "$_resolver" ]; then
  echo "ERROR: could not determine DNS resolver from /etc/resolv.conf" >&2
  exit 1
fi
# nginx требует IPv6-адреса в квадратных скобках: [fd12::10]
case "$_resolver" in
  *:*) export RESOLVER="[$_resolver]" ;;
  *)   export RESOLVER="$_resolver" ;;
esac

# Подставляем переменные в шаблон nginx.
# Явный список гарантирует, что nginx-переменные ($host, $uri и т.д.) остаются нетронутыми.
envsubst '$PORT $BACKEND_HOST $BACKEND_PORT $RESOLVER' \
  < /etc/nginx/nginx.conf.template \
  > /etc/nginx/nginx.conf

exec nginx -g 'daemon off;'
