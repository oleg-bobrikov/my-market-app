package ru.yandex.practicum.shop.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.payment.client.ApiClient;
import ru.yandex.practicum.payment.client.api.DefaultApi;
import ru.yandex.practicum.payment.client.model.PaymentRequest;
import ru.yandex.practicum.shop.exception.InsufficientFundsException;
import ru.yandex.practicum.shop.exception.PaymentServiceException;

import java.math.BigDecimal;
import java.util.UUID;

@Component
public class PaymentClient {
    private final DefaultApi paymentApi;

    public PaymentClient(@Value("${payment.service.url}") String baseUrl) {
        ApiClient apiClient = new ApiClient();
        apiClient.setBasePath(baseUrl);
        this.paymentApi = new DefaultApi(apiClient);
    }

    public Mono<BigDecimal> getBalance(UUID sessionId) {
        paymentApi.getApiClient().addDefaultHeader("session_id", sessionId.toString());
        return paymentApi.getBalance()
                .map(balance -> {
                    if (balance.getBalance() == null) {
                        return BigDecimal.ZERO;
                    }
                    return new BigDecimal(balance.getBalance());
                })
                .onErrorResume(e -> {
                    if (e instanceof WebClientResponseException webEx) {
                        return Mono.error(new PaymentServiceException("Ошибка при получении баланса: " + webEx.getMessage()));
                    }
                    return Mono.error(e);
                });
    }

    public Mono<Void> pay(ru.yandex.practicum.shop.client.model.PaymentRequest paymentRequest, UUID sessionId) {
        paymentApi.getApiClient().addDefaultHeader("session_id", sessionId.toString());
        
        PaymentRequest apiRequest = new PaymentRequest();
        apiRequest.setOrderId(paymentRequest.orderId());
        apiRequest.setAmount(paymentRequest.amount().toString());

        return paymentApi.payOrder(apiRequest)
                .onErrorResume(e -> {
                    if (e instanceof WebClientResponseException webEx) {
                        if (webEx.getStatusCode().is4xxClientError()) {
                            return Mono.error(new InsufficientFundsException("Недостаточно средств или ошибка клиента"));
                        }
                        return Mono.error(new PaymentServiceException("Ошибка сервиса платежей: " + webEx.getMessage()));
                    }
                    return Mono.error(e);
                })
                .then();
    }
}
