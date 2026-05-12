package ru.yandex.practicum.shop.filter;

import com.github.f4b6a3.uuid.UuidCreator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpCookie;
import org.springframework.http.ResponseCookie;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.UUID;

@Component
public class SessionWebFilter implements WebFilter {

    public static final String SESSION_ATTRIBUTE = "SESSION_ID_ATTR";
    private static final String COOKIE_NAME = "SESSION_ID";

    @Value("${app.cookie.max-age:7d}")
    private Duration cookieMaxAge;

    @Override
    @NonNull
    public Mono<Void> filter(@NonNull ServerWebExchange exchange, @NonNull WebFilterChain chain) {
        HttpCookie sessionCookie = exchange.getRequest().getCookies().getFirst(COOKIE_NAME);
        UUID sessionUuid;

        if (sessionCookie == null || sessionCookie.getValue().isBlank()) {
            sessionUuid = createSessionId(exchange);
        } else {
            try {
                sessionUuid = UUID.fromString(sessionCookie.getValue());
            } catch (IllegalArgumentException e) {
                sessionUuid = createSessionId(exchange);
            }
        }

        exchange.getAttributes().put(SESSION_ATTRIBUTE, sessionUuid);
        return chain.filter(exchange);
    }

    private UUID createSessionId(ServerWebExchange exchange) {
        UUID uuid = UuidCreator.getTimeOrderedEpoch();
        exchange.getResponse().addCookie(
                ResponseCookie.from(COOKIE_NAME, uuid.toString())
                        .httpOnly(true)
                        .path("/")
                        .maxAge(cookieMaxAge)
                        .build()
        );
        return uuid;
    }
}
