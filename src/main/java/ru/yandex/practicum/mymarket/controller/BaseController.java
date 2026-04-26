package ru.yandex.practicum.mymarket.controller;

import com.github.f4b6a3.uuid.UuidCreator;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Controller;
import org.springframework.util.MultiValueMap;
import org.springframework.web.server.ServerWebExchange;

import java.time.Duration;
import java.util.UUID;

@Controller
public class BaseController {
    public UUID resolveSessionId(String sessionId, ServerWebExchange exchange) {
        if (sessionId == null || sessionId.isBlank()) {
            return createSessionId(exchange);
        }
        try {
            return UUID.fromString(sessionId);
        } catch (IllegalArgumentException e) {
            return createSessionId(exchange);
        }
    }

    private UUID createSessionId(ServerWebExchange exchange) {
        UUID uuid = UuidCreator.getTimeOrderedEpoch();
        exchange.getResponse().addCookie(
                ResponseCookie.from("SESSION_ID", uuid.toString())
                        .httpOnly(true)
                        .path("/")
                        .maxAge(Duration.ofDays(7))
                        .build()
        );

        return uuid;
    }

    protected String getParam(MultiValueMap<String, String> formData, MultiValueMap<String, String> queryParams, String name) {
        String value = formData.getFirst(name);
        return value != null ? value : queryParams.getFirst(name);
    }
}
