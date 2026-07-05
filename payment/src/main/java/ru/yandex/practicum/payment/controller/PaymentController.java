package ru.yandex.practicum.payment.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.payment.api.BalanceApi;
import ru.yandex.practicum.payment.api.DefaultApi;
import ru.yandex.practicum.payment.model.Balance;
import ru.yandex.practicum.payment.model.PaymentRequest;
import ru.yandex.practicum.payment.model.PaymentResponse;
import ru.yandex.practicum.payment.service.PaymentService;


@RestController
@RequestMapping("/payments/api")
@RequiredArgsConstructor
public class PaymentController implements BalanceApi, DefaultApi {
    private final PaymentService paymentService;

    @Override
    @PostMapping({"", "/"})
    @PreAuthorize("hasAuthority('SERVICE')")
    public Mono<ResponseEntity<PaymentResponse>> payOrder(
            @RequestBody Mono<PaymentRequest> paymentRequest,
            ServerWebExchange exchange
    ) {
        return paymentRequest
                .flatMap(paymentService::payOrder)
                .map(ResponseEntity::ok);
    }

    @Override
    @GetMapping("/balance/{accountId}")
    @PreAuthorize("hasAuthority('SERVICE')")
    public Mono<ResponseEntity<Balance>> getBalance(
            @PathVariable Long accountId,
            ServerWebExchange exchange
    ) {
        return paymentService.getBalance(accountId)
                .map(ResponseEntity::ok);
    }
}