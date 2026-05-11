package ru.yandex.practicum.payment.mymarket.integration;

import com.github.f4b6a3.uuid.UuidCreator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.test.StepVerifier;
import ru.yandex.practicum.shop.model.CartAction;
import ru.yandex.practicum.shop.repository.CartRepository;
import ru.yandex.practicum.shop.repository.OrderRepository;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import ru.yandex.practicum.shop.client.PaymentClient;
import reactor.core.publisher.Mono;
import java.math.BigDecimal;

import java.util.UUID;
import java.util.concurrent.TimeUnit;
import static org.awaitility.Awaitility.await;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class OrderIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private ru.yandex.practicum.shop.service.CartService cartService;

    @Autowired
    private ru.yandex.practicum.shop.repository.ItemRepository itemRepository;

    @Autowired
    private OrderRepository orderRepository;

    @MockitoBean
    private PaymentClient paymentClient;

    @Test
    void buy_WhenOrderCreated_RedirectsToOrderPage() {
        UUID sessionId = UuidCreator.getTimeOrderedEpoch();
        
        ru.yandex.practicum.shop.entity.ItemEntity itemEntity = itemRepository.findAll().blockFirst();
        long itemId = itemEntity != null ? itemEntity.getId() : 1L;

        when(paymentClient.getBalance(any())).thenReturn(Mono.just(new BigDecimal("1000.00")));
        when(paymentClient.pay(any(), any())).thenReturn(Mono.empty());

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

        // Проверяем, что товар в корзине
        await().atMost(10, TimeUnit.SECONDS).until(() -> {
            var counts = cartService.getCartCounts(sessionId).block();
            return counts != null && !counts.isEmpty();
        });

        // 2. Совершаем покупку
        webTestClient.post().uri("/buy")
                .cookie("SESSION_ID", sessionId.toString())
                .exchange()
                .expectStatus().is3xxRedirection();

        // 3. Проверяем, что заказ создался
        await().atMost(10, TimeUnit.SECONDS).until(() -> {
            var orders = orderRepository.findBySessionId(sessionId).collectList().block();
            return orders != null && !orders.isEmpty();
        });

        orderRepository.findBySessionId(sessionId)
                .as(StepVerifier::create)
                .expectNextMatches(order -> order.getSessionId().equals(sessionId))
                .verifyComplete();
    }

    @Test
    void buy_WhenNoSession_RedirectsToItems() {
        when(paymentClient.getBalance(any())).thenReturn(Mono.just(new BigDecimal("1000.00")));
        when(paymentClient.pay(any(), any())).thenReturn(Mono.empty());

        webTestClient.post().uri("/buy")
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().valueEquals("Location", "/items");
    }
}
