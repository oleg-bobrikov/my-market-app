package ru.yandex.practicum.payment.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.payment.api.BalanceApi;
import ru.yandex.practicum.payment.api.DefaultApi;
import ru.yandex.practicum.payment.model.Balance;
import ru.yandex.practicum.payment.model.PaymentRequest;
import ru.yandex.practicum.payment.model.PaymentResponse;
import ru.yandex.practicum.payment.service.PaymentService;

import java.util.UUID;

@RestController
@RequestMapping("/payments/api")
@RequiredArgsConstructor
public class PaymentController implements BalanceApi, DefaultApi {
    private static final String SESSION_ID = "session_id";
    private final PaymentService paymentService;

    @Override
    @PostMapping("")
    public Mono<ResponseEntity<PaymentResponse>> payOrder(
            @RequestBody Mono<PaymentRequest> paymentRequest,
            ServerWebExchange exchange
    ) {
        return paymentRequest
                .flatMap(request -> {
                    UUID sessionId = getSessionId(exchange);
                    return paymentService.payOrder(sessionId, request);
                })
                .map(ResponseEntity::ok);
    }

    @Override
    @GetMapping("/balance")
    public Mono<ResponseEntity<Balance>> getBalance(
            ServerWebExchange exchange
    ) {
        try {
            UUID sessionId = getSessionId(exchange);
            return paymentService.getBalance(sessionId)
                    .map(ResponseEntity::ok);
        } catch (Exception e) {
            return Mono.error(e);
        }
    }

    private UUID getSessionId(ServerWebExchange exchange) {
        String sessionId = exchange.getRequest()
                .getHeaders()
                .getFirst(SESSION_ID);

        if (sessionId == null) {
            throw new IllegalArgumentException("Missing session_id header");
        }

        return UUID.fromString(sessionId);
    }
}