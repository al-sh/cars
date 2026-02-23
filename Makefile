.PHONY: back back-stop front front-stop dev dev-stop test db db-stop db-logs

# Запуск фронта и бэка одновременно
dev:
	cd back && mvn spring-boot:run -Dspring-boot.run.profiles=dev &
	cd front && npm start

# Остановка фронта и бэка
dev-stop: back-stop front-stop

# Запуск приложения с dev-профилем (jwt.secret из application-dev.yml)
back:
	cd back && mvn spring-boot:run -Dspring-boot.run.profiles=dev

# Остановка приложения (убивает процесс на порту 8080)
back-stop:
	kill $$(lsof -ti:8080) 2>/dev/null || true

# Запуск Angular dev-сервера
front:
	cd front && npm start

# Остановка Angular dev-сервера (убивает процесс на порту 4200)
front-stop:
	kill $$(lsof -ti:4200) 2>/dev/null || true

# Запуск тестов
test:
	cd back && mvn test

# Запуск PostgreSQL в фоне
db:
	docker compose -f back/docker-compose.yml up -d

# Остановка PostgreSQL
db-stop:
	docker compose -f back/docker-compose.yml down

# Логи PostgreSQL
db-logs:
	docker compose -f back/docker-compose.yml logs -f
