package ru.yandex.practicum.payment.mymarket.integration;

import com.github.f4b6a3.uuid.UuidCreator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.shop.client.PaymentClient;
import ru.yandex.practicum.shop.model.CartAction;
import ru.yandex.practicum.shop.repository.ItemRepository;

import java.math.BigDecimal;
import java.util.UUID;

import ru.yandex.practicum.shop.exception.InsufficientFundsException;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class InsufficientFundsIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private ItemRepository itemRepository;

    @MockitoBean
    private PaymentClient paymentClient;

    @Test
    void buy_WhenInsufficientFunds_ShowsErrorMessage() {
        UUID sessionId = UuidCreator.getTimeOrderedEpoch();
        
        var itemEntity = itemRepository.findAll().blockFirst();
        long itemId = itemEntity != null ? itemEntity.getId() : 1L;

        when(paymentClient.getBalance(any())).thenReturn(Mono.just(new BigDecimal("1000.00")));
        // Имитируем выброс исключения о нехватке средств, которое теперь должен кидать PaymentClient
        when(paymentClient.pay(any(), any())).thenReturn(Mono.error(new InsufficientFundsException("Недостаточно средств на счете")));

        // 1. Добавляем товар в корзину
        webTestClient.post().uri(uriBuilder -> uriBuilder.path("/items")
                        .queryParam("id", Long.toString(itemId))
                        .queryParam("action", CartAction.PLUS.name())
                        .queryParam("search", "")
                        .queryParam("sort", "NO")
                        .queryParam("pageSize", "5")
                        .queryParam("pageNumber", "1")
                        .build())
                .cookie("SESSION_ID", sessionId.toString())
                .exchange()
                .expectStatus().is3xxRedirection();

        // 2. Совершаем покупку и проверяем, что на странице корзины есть ошибка
        webTestClient.post().uri("/buy")
                .cookie("SESSION_ID", sessionId.toString())
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .value(body -> {
                    // Теперь ожидаем правильное сообщение
                    assert body.contains("Недостаточно средств на счете");
                    assert !body.contains("сервис платежей недоступен");
                });
    }
}
