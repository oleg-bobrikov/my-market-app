package ru.yandex.practicum.shop.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers;
import reactor.test.StepVerifier;
import ru.yandex.practicum.shop.entity.ItemEntity;
import ru.yandex.practicum.shop.model.CartAction;
import ru.yandex.practicum.shop.repository.OrderRepository;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import ru.yandex.practicum.shop.client.PaymentClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.csrf;

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
        long userId = 1L;

        ItemEntity itemEntity = itemRepository.findAll().blockFirst();
        long itemId = itemEntity != null ? itemEntity.getId() : 1L;

        when(paymentClient.getBalance(any())).thenReturn(Mono.just(new BigDecimal("1000.00")));
        when(paymentClient.pay(any())).thenReturn(Mono.empty());

        // 1. Добавляем товар в корзину
        webTestClient
                .mutateWith(csrf())
                .mutateWith(SecurityMockServerConfigurers.mockAuthentication(auth(1L)))
                .post().uri(uriBuilder -> uriBuilder.path("/items")
                        .queryParam("id", Long.toString(itemId))
                        .queryParam("action", CartAction.PLUS.name())
                        .queryParam("search", "")
                        .queryParam("sort", "NO")
                        .queryParam("pageSize", "5")
                        .queryParam("pageNumber", "1")
                        .build())
                .exchange()
                .expectStatus().is3xxRedirection();

        // 2. Совершаем покупку
        webTestClient
                .mutateWith(csrf())
                .mutateWith(SecurityMockServerConfigurers.mockAuthentication(auth(1L)))
                .post().uri("/buy")
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().valueMatches("Location", "/orders/\\d+");

        // 3. Проверяем, что заказ создался
        var orders = orderRepository.findByUserId(userId).collectList().block();
        assert orders != null && !orders.isEmpty();

        orderRepository.findByUserId(userId)
                .as(StepVerifier::create)
                .expectNextMatches(order -> order.getUserId().equals(userId))
                .verifyComplete();

        // 4. Проверяем, что корзина очистилась
        var finalCounts = cartService.getCartCounts(userId).block();
        assert finalCounts == null || finalCounts.isEmpty();
    }

    @Test
    void buy_WhenIsAnonymous_RedirectsToLoginPage() {
        when(paymentClient.getBalance(any())).thenReturn(Mono.just(new BigDecimal("1000.00")));
        when(paymentClient.pay(any())).thenReturn(Mono.empty());

        webTestClient
                .mutateWith(csrf())
                .post().uri("/buy")
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().valueEquals("Location", "/login");
    }
}
