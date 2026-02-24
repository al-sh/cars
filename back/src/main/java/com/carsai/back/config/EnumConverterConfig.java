package com.carsai.back.config;

import com.carsai.back.car.BodyType;
import com.carsai.back.car.DriveType;
import com.carsai.back.car.EngineType;
import com.carsai.back.car.Transmission;
import org.springframework.context.annotation.Configuration;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Регистрирует конвертеры String → Enum для @RequestParam.
 * @JsonCreator работает только для JSON-тела, но не для query-параметров.
 * Здесь подключаем lower-case → UPPER_CASE конвертацию для car enum-ов.
 */
@Configuration
public class EnumConverterConfig implements WebMvcConfigurer {

    @Override
    public void addFormatters(FormatterRegistry registry) {
        registry.addConverter(String.class, BodyType.class, BodyType::fromJson);
        registry.addConverter(String.class, EngineType.class, EngineType::fromJson);
        registry.addConverter(String.class, Transmission.class, Transmission::fromJson);
        registry.addConverter(String.class, DriveType.class, DriveType::fromJson);
    }
}
