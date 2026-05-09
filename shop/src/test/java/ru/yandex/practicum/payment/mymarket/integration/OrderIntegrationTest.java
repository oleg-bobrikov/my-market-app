package ru.yandex.practicum.payment.mymarket.integration;

import com.github.f4b6a3.uuid.UuidCreator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.test.StepVerifier;
import ru.yandex.practicum.shop.model.CartAction;
import ru.yandex.practicum.shop.repository.CartRepository;
import ru.yandex.practicum.shop.repository.OrderRepository;

import java.util.UUID;

public class OrderIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Test
    void testBuyAndRedirect() {
        UUID sessionId = UuidCreator.getTimeOrderedEpoch();
        long itemId = 1L;

        // 1. Добавляем товар в корзину
        webTestClient.post().uri(uriBuilder -> uriBuilder.path("/items")
                        .queryParam("id", Long.toString(itemId))
                        .queryParam("action", CartAction.PLUS.name())
                        .build())
                .cookie("SESSION_ID", sessionId.toString())
                .exchange()
                .expectStatus().is3xxRedirection();

        // Проверяем, что товар в корзине
        cartRepository.findBySessionId(sessionId)
                .as(StepVerifier::create)
                .expectNextCount(1)
                .verifyComplete();

        // 2. Совершаем покупку
        webTestClient.post().uri("/buy")
                .cookie("SESSION_ID", sessionId.toString())
                .exchange()
                .expectStatus().is3xxRedirection();

        // 3. Проверяем, что корзина пуста
        cartRepository.findBySessionId(sessionId)
                .as(StepVerifier::create)
                .verifyComplete();

        // 4. Проверяем, что заказ создался
        orderRepository.findBySessionId(sessionId)
                .as(StepVerifier::create)
                .expectNextMatches(order -> order.getSessionId().equals(sessionId))
                .verifyComplete();
    }

    @Test
    void testBuyWithoutSessionRedirects() {
        webTestClient.post().uri("/buy")
                .exchange()
                .expectStatus().is3xxRedirection();
    }
}
