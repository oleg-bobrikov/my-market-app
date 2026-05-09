package ru.yandex.practicum.payment.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.payment.model.PaymentRequest;
import ru.yandex.practicum.payment.model.PaymentResponse;
import ru.yandex.practicum.payment.model.PaymentStatus;
import ru.yandex.practicum.payment.service.PaymentService;

import java.math.BigDecimal;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@WebFluxTest(PaymentController.class)
class PaymentControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private PaymentService paymentService;

    @Test
    void payOrder_Success_Returns200() {
        UUID sessionId = UUID.randomUUID();
        PaymentRequest request = new PaymentRequest("order-1", new BigDecimal("100.00"));
        PaymentResponse response = new PaymentResponse(PaymentStatus.SUCCESS, "order-1", new BigDecimal("900.00"));

        when(paymentService.payOrder(eq(sessionId), any(PaymentRequest.class)))
                .thenReturn(Mono.just(response));

        webTestClient.post()
                .uri("/payments/api")
                .header("session_id", sessionId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo("SUCCESS")
                .jsonPath("$.orderId").isEqualTo("order-1")
                .jsonPath("$.remainingBalance").isEqualTo("900.0");
    }

    @Test
    void payOrder_InsufficientFunds_Returns400() {
        UUID sessionId = UUID.randomUUID();
        PaymentRequest request = new PaymentRequest("order-1", new BigDecimal("1000.00"));

        when(paymentService.payOrder(eq(sessionId), any(PaymentRequest.class)))
                .thenReturn(Mono.error(new RuntimeException("Недостаточно средств на счете")));

        webTestClient.post()
                .uri("/payments/api")
                .header("session_id", sessionId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.status").isEqualTo("ERROR")
                .jsonPath("$.message").isEqualTo("Недостаточно средств на счете");
    }
}
