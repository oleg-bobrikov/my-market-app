package ru.yandex.practicum.shop.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.shop.client.model.PaymentRequest;
import org.springframework.http.HttpStatusCode;
import ru.yandex.practicum.shop.exception.InsufficientFundsException;
import java.util.Map;

import java.math.BigDecimal;
import java.util.UUID;

@Component
public class PaymentClient {
    private final WebClient webClient;

    public PaymentClient(@Value("${payment.service.url:http://localhost:8081}") String baseUrl) {
        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .build();
    }

    public Mono<BigDecimal> getBalance(UUID sessionId) {
        return webClient.get()
                .uri("/payments/api/balance")
                .header("session_id", sessionId.toString())
                .retrieve()
                .bodyToMono(Map.class)
                .map(map -> {
                    Object balance = map.get("balance");
                    if (balance == null) {
                        return BigDecimal.ZERO;
                    }
                    return new BigDecimal(balance.toString());
                });
    }

    public Mono<Void> pay(PaymentRequest paymentRequest, UUID sessionId) {
        return webClient.post()
                .uri("/payments/api")
                .header("session_id", sessionId.toString())
                .bodyValue(paymentRequest)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, response ->
                        response.bodyToMono(Map.class)
                                .flatMap(body -> {
                                    String message = body.get("message") != null ? body.get("message").toString() : "Ошибка платежа";
                                    return Mono.error(new InsufficientFundsException(message));
                                })
                )
                .bodyToMono(Void.class);
    }
}
