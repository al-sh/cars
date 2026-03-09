# Build stage
FROM node:20-alpine AS build
WORKDIR /app

# Кэширование зависимостей
COPY front/package*.json ./
RUN npm ci --quiet

# Сборка Angular в production-режиме
COPY front/ .
RUN npm run build

# Runtime stage — nginx раздаёт статику и проксирует /api/
FROM nginx:alpine

COPY nginx/nginx.conf /etc/nginx/nginx.conf
# Angular 19 кладёт результат сборки в dist/ui/browser
COPY --from=build /app/dist/ui/browser /usr/share/nginx/html

EXPOSE 80
