package com.carsai.back.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * AOP-аспект логирования REST-запросов.
 *
 * Перехватывает все методы @RestController и записывает в logs/rest-api.log:
 * - HTTP-метод, путь, параметры запроса
 * - Для POST/PATCH: аргументы метода (тело запроса)
 * - LLM-вызовы: что отправили → что получили (из RestLogContext)
 * - Финальный ответ пользователю и время выполнения
 * - Ошибки при их возникновении
 */
@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class RestLoggingAspect {

    private final ObjectMapper objectMapper;

    private static final String SEPARATOR = "═".repeat(70);
    private static final String LLM_SEPARATOR = "─".repeat(50);
    private static final int MAX_BODY_LENGTH = 2000;

    @Around("within(@org.springframework.web.bind.annotation.RestController *)")
    public Object logRestCall(ProceedingJoinPoint pjp) throws Throwable {
        long startMs = System.currentTimeMillis();

        HttpServletRequest request = getRequest();
        String method = request != null ? request.getMethod() : "UNKNOWN";
        String uri = request != null ? request.getRequestURI() : "UNKNOWN";
        String queryParams = request != null ? buildQueryParams(request) : "";
        String user = getCurrentUser();

        StringBuilder sb = new StringBuilder();
        sb.append("\n").append(SEPARATOR).append("\n");
        sb.append(">>> ").append(method).append(" ").append(uri);
        if (!queryParams.isEmpty()) {
            sb.append("?").append(queryParams);
        }
        sb.append("\n");
        sb.append("    User: ").append(user).append("\n");

        // Для POST/PATCH логируем аргументы метода (тело запроса)
        if ("POST".equals(method) || "PATCH".equals(method)) {
            String body = buildMethodArgs(pjp.getArgs());
            if (!body.isEmpty()) {
                sb.append("    Body: ").append(truncate(body)).append("\n");
            }
        }

        Object result = null;
        Throwable thrown = null;
        try {
            result = pjp.proceed();
            return result;
        } catch (Throwable t) {
            thrown = t;
            throw t;
        } finally {
            long durationMs = System.currentTimeMillis() - startMs;

            // LLM-вызовы (только если были)
            List<RestLogContext.LlmCallEntry> llmCalls = RestLogContext.getLlmCalls();
            if (!llmCalls.isEmpty()) {
                sb.append("\n").append("    LLM-вызовы (").append(llmCalls.size()).append("):\n");
                for (RestLogContext.LlmCallEntry call : llmCalls) {
                    sb.append("    ").append(LLM_SEPARATOR).append("\n");
                    sb.append("    [").append(call.requestType()).append("] ").append(call.durationMs()).append("ms\n");
                    if (call.error() != null) {
                        sb.append("    → ОШИБКА: ").append(call.error()).append("\n");
                    } else {
                        sb.append("    → Запрос: ").append(truncate(call.userMessage())).append("\n");
                        sb.append("    ← Ответ:  ").append(truncate(call.responseContent())).append("\n");
                    }
                }
                RestLogContext.clear();
            }

            // Финальный ответ / ошибка
            sb.append("\n");
            if (thrown != null) {
                sb.append("    ❌ Ошибка (").append(durationMs).append("ms): ")
                        .append(thrown.getClass().getSimpleName()).append(": ").append(thrown.getMessage()).append("\n");
            } else {
                String responseBody = serializeResult(result);
                sb.append("    ✓ Ответ (").append(durationMs).append("ms): ")
                        .append(truncate(responseBody)).append("\n");
            }
            sb.append(SEPARATOR);

            log.info(sb.toString());
        }
    }

    private HttpServletRequest getRequest() {
        try {
            ServletRequestAttributes attrs =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            return attrs != null ? attrs.getRequest() : null;
        } catch (Exception e) {
            return null;
        }
    }

    private String getCurrentUser() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated()) {
                return auth.getName();
            }
        } catch (Exception ignored) {}
        return "anonymous";
    }

    private String buildQueryParams(HttpServletRequest request) {
        Map<String, String[]> params = request.getParameterMap();
        if (params.isEmpty()) return "";
        return params.entrySet().stream()
                .map(e -> e.getKey() + "=" + String.join(",", e.getValue()))
                .collect(Collectors.joining("&"));
    }

    private String buildMethodArgs(Object[] args) {
        if (args == null || args.length == 0) return "";
        // Пропускаем служебные объекты Spring Security и примитивные path-переменные
        return Arrays.stream(args)
                .filter(a -> a != null
                        && !(a instanceof com.carsai.back.security.UserPrincipal)
                        && !(a instanceof java.util.UUID))
                .map(this::serializeResult)
                .filter(s -> !s.isBlank())
                .collect(Collectors.joining(", "));
    }

    private String serializeResult(Object obj) {
        if (obj == null) return "null";
        try {
            if (obj instanceof String s) return s;
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return obj.toString();
        }
    }

    private String truncate(String s) {
        if (s == null) return "null";
        if (s.length() <= MAX_BODY_LENGTH) return s;
        return s.substring(0, MAX_BODY_LENGTH) + "... [truncated " + (s.length() - MAX_BODY_LENGTH) + " chars]";
    }
}
