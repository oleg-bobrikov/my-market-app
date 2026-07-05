package ru.yandex.practicum.payment.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.http.MediaType;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.payment.model.PaymentRequest;
import ru.yandex.practicum.payment.model.PaymentResponse;
import ru.yandex.practicum.payment.model.PaymentStatus;
import ru.yandex.practicum.payment.service.PaymentService;


import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.csrf;

@WebFluxTest({PaymentController.class, PaymentExceptionHandler.class})
class PaymentControllerTest {

    @TestConfiguration
    static class TestConfig {
        @Bean
        public ReactiveJwtDecoder jwtDecoder() {
            return token -> Mono.empty();
        }
    }

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private PaymentService paymentService;

    @Test
    @WithMockUser(authorities = "SERVICE")
    void pay_WhenSuccessful_ReturnsOk() {
        PaymentRequest request = new PaymentRequest();
        request.setClientId(1L);
        request.setOrderId("order-1");
        request.setAmount("100.00");
        
        PaymentResponse response = new PaymentResponse();
        response.setStatus(PaymentStatus.SUCCESS);
        response.setOrderId("order-1");
        response.setRemainingBalance("900.00");

        when(paymentService.payOrder(any(PaymentRequest.class)))
                .thenReturn(Mono.just(response));

        webTestClient.mutateWith(csrf()).post()
                .uri("/payments/api")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo("SUCCESS")
                .jsonPath("$.orderId").isEqualTo("order-1")
                .jsonPath("$.remainingBalance").isEqualTo("900.00");
    }

    @Test
    @WithMockUser(authorities = "SERVICE")
    void pay_WhenInsufficientFunds_ReturnsBadRequest() {
        PaymentRequest request = new PaymentRequest();
        request.setClientId(1L);
        request.setOrderId("order-1");
        request.setAmount("1000.00");

        when(paymentService.payOrder(any(PaymentRequest.class)))
                .thenReturn(Mono.error(new ru.yandex.practicum.payment.exception.InsufficientFundsException("Недостаточно средств на счете")));

        webTestClient.mutateWith(csrf()).post()
                .uri("/payments/api")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.status").isEqualTo("ERROR")
                .jsonPath("$.message").isEqualTo("Недостаточно средств на счете");
    }

    @Test
    @WithMockUser(authorities = "SERVICE")
    void getBalance_ReturnsOk() {
        ru.yandex.practicum.payment.model.Balance balance = new ru.yandex.practicum.payment.model.Balance();
        balance.setClientId(1L);
        balance.setBalance("1000.00");

        when(paymentService.getBalance(1L))
                .thenReturn(Mono.just(balance));

        webTestClient.get()
                .uri("/payments/api/balance/1")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.clientId").isEqualTo(1)
                .jsonPath("$.balance").isEqualTo("1000.00");
    }

    @Test
    void pay_WhenUnauthenticated_ReturnsUnauthorized() {
        PaymentRequest request = new PaymentRequest();
        request.setClientId(1L);
        request.setOrderId("order-1");
        request.setAmount("100.00");

        webTestClient.mutateWith(csrf()).post()
                .uri("/payments/api")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    @WithMockUser(authorities = "SERVICE")
    void pay_WhenServiceAuthority_ReturnsOk() {
        PaymentRequest request = new PaymentRequest();
        request.setClientId(1L);
        request.setOrderId("order-1");
        request.setAmount("100.00");

        PaymentResponse response = new PaymentResponse();
        response.setStatus(PaymentStatus.SUCCESS);
        response.setOrderId("order-1");

        when(paymentService.payOrder(any(PaymentRequest.class)))
                .thenReturn(Mono.just(response));

        webTestClient.mutateWith(csrf()).post()
                .uri("/payments/api")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isOk();
    }
}
