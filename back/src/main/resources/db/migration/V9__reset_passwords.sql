-- V9__reset_passwords.sql — сброс паролей всех пользователей.
--
-- Устанавливает пароль "password" для всех пользователей через pgcrypto.
-- Используется для разработки и тестирования.

CREATE EXTENSION IF NOT EXISTS pgcrypto;

UPDATE users
SET password_hash = crypt('password', gen_salt('bf', 10));
