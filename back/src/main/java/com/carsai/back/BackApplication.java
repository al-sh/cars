package com.carsai.back;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Точка входа Spring Boot приложения.
 *
 * @SpringBootApplication — комбинация трёх аннотаций:
 * 1. @Configuration — этот класс может содержать @Bean-методы (конфигурация).
 * 2. @EnableAutoConfiguration — Spring автоматически настраивает компоненты
 *    на основе зависимостей в pom.xml. Есть PostgreSQL driver? Настроит DataSource.
 *    Есть spring-boot-starter-web? Запустит встроенный Tomcat.
 * 3. @ComponentScan — Spring сканирует пакет com.carsai.back и все подпакеты,
 *    находит классы с @Controller, @Service, @Repository и создаёт их экземпляры.
 *
 * Аналогия Angular:
 * - SpringApplication.run() — как bootstrapApplication(AppComponent)
 * - @ComponentScan — как автоматическое обнаружение standalone-компонентов
 * - @EnableAutoConfiguration — как если бы Angular автоматически подключал
 *   HttpClientModule, когда видит HttpClient в imports
 *
 * Аналогия Express:
 * - Это как файл index.js с app.listen(8080), но Spring Boot сам запускает сервер,
 *   подключает middleware (фильтры), маршрутизацию и DI-контейнер.
 */
@SpringBootApplication
public class BackApplication {

    public static void main(String[] args) {
        // SpringApplication.run() делает всё:
        // 1. Создаёт ApplicationContext (DI-контейнер — хранит все бины/сервисы)
        // 2. Сканирует пакеты на @Component, @Service, @Controller, @Repository
        // 3. Выполняет автоконфигурацию (DataSource, JPA, Flyway, ...)
        // 4. Запускает встроенный Tomcat на порту из application.yml
        SpringApplication.run(BackApplication.class, args);
    }
}
