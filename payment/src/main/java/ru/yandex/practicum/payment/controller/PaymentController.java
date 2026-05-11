package ru.yandex.practicum.payment.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.payment.api.BalanceApi;
import ru.yandex.practicum.payment.api.DefaultApi;
import ru.yandex.practicum.payment.model.Balance;
import ru.yandex.practicum.payment.model.PaymentRequest;
import ru.yandex.practicum.payment.model.PaymentResponse;
import ru.yandex.practicum.payment.service.PaymentService;

@RestController
@RequiredArgsConstructor
public class PaymentController implements BalanceApi, DefaultApi {
    private final PaymentService paymentService;

    @Override
    @RequestMapping(
        method = RequestMethod.POST,
        value = "/payments/api",
        produces = { "application/json" },
        consumes = { "application/json" }
    )
    public Mono<ResponseEntity<PaymentResponse>> payOrder(
            Mono<PaymentRequest> paymentRequest,
            ServerWebExchange exchange
    ) {
        String sessionId = exchange.getRequest().getHeaders().getFirst("session_id");
        if (sessionId == null) {
            return Mono.error(new IllegalArgumentException("Missing session_id header"));
        }
        return paymentRequest.flatMap(request -> paymentService.payOrder(java.util.UUID.fromString(sessionId), request)
                .map(ResponseEntity::ok));
    }

    @Override
    @RequestMapping(
        method = RequestMethod.GET,
        value = "/payments/api/balance",
        produces = { "application/json" }
    )
    public Mono<ResponseEntity<Balance>> getBalance(
            ServerWebExchange exchange
    ) {
        String sessionId = exchange.getRequest().getHeaders().getFirst("session_id");
        if (sessionId == null) {
            return Mono.error(new IllegalArgumentException("Missing session_id header"));
        }
        return paymentService.getBalance(java.util.UUID.fromString(sessionId))
                .map(ResponseEntity::ok);
    }
}
