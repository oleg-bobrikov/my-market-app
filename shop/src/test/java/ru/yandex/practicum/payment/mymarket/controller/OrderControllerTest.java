package ru.yandex.practicum.payment.mymarket.controller;

import com.github.f4b6a3.uuid.UuidCreator;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.shop.model.Order;

import java.math.BigDecimal;
import java.util.UUID;

import static org.mockito.Mockito.*;

public class OrderControllerTest extends BaseWebFluxTest {

    @Test
    public void testBuyRedirectsToOrder() {
        UUID sessionId = UuidCreator.getTimeOrderedEpoch();
        BigDecimal total = BigDecimal.valueOf(100);
        BigDecimal balance = BigDecimal.valueOf(200);
        Order order = Order.builder().id(123L).total(total).build();

        when(orderService.calculateTotal(sessionId)).thenReturn(Mono.just(total));
        when(orderService.getBalance(sessionId)).thenReturn(Mono.just(balance));
        when(orderService.createOrder(sessionId)).thenReturn(Mono.just(order));

        webTestClient.post().uri("/buy")
                .cookie("SESSION_ID", sessionId.toString())
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().valueEquals("Location", "/orders/123");

        verify(orderService).createOrder(sessionId);
    }

    @Test
    public void testBuyInsufficientBalance() {
        UUID sessionId = UuidCreator.getTimeOrderedEpoch();
        BigDecimal total = BigDecimal.valueOf(200);
        BigDecimal balance = BigDecimal.valueOf(100);

        when(orderService.calculateTotal(sessionId)).thenReturn(Mono.just(total));
        when(orderService.getBalance(sessionId)).thenReturn(Mono.just(balance));
        when(cartService.getCartItems(sessionId)).thenReturn(Flux.just(new ru.yandex.practicum.shop.model.Item()));
        when(itemMapper.toDto(any())).thenReturn(new ru.yandex.practicum.shop.dto.ItemDto());
        when(cartService.getTotalPrice(any())).thenReturn(Mono.just(total));

        webTestClient.post().uri("/buy")
                .cookie("SESSION_ID", sessionId.toString())
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class).value(body -> {
                    org.junit.jupiter.api.Assertions.assertTrue(body.contains("на балансе недостаточно средств"), "Body does not contain expected error message. Body: " + body);
                });
    }

    @Test
    public void testBuyServiceUnavailable() {
        UUID sessionId = UuidCreator.getTimeOrderedEpoch();
        BigDecimal total = BigDecimal.valueOf(100);

        when(orderService.calculateTotal(sessionId)).thenReturn(Mono.just(total));
        when(orderService.getBalance(sessionId)).thenReturn(Mono.error(new RuntimeException("Service down")));
        when(cartService.getCartItems(sessionId)).thenReturn(Flux.just(new ru.yandex.practicum.shop.model.Item()));
        when(itemMapper.toDto(any())).thenReturn(new ru.yandex.practicum.shop.dto.ItemDto());
        when(cartService.getTotalPrice(any())).thenReturn(Mono.just(total));

        webTestClient.post().uri("/buy")
                .cookie("SESSION_ID", sessionId.toString())
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class).value(body -> {
                    org.junit.jupiter.api.Assertions.assertTrue(body.contains("сервис платежей недоступен"), "Body does not contain expected error message. Body: " + body);
                });
    }

    @Test
    public void testGetOrderReturnsOrderView() {
        UUID sessionId = UuidCreator.getTimeOrderedEpoch();
        Order order = Order.builder().id(1L).total(BigDecimal.valueOf(100)).build();
        when(orderService.getOrderByIdAndSessionId(1L, sessionId)).thenReturn(Mono.just(order));
        when(orderService.getOrderItems(1L)).thenReturn(Flux.empty());

        webTestClient.get().uri("/orders/1")
                .cookie("SESSION_ID", sessionId.toString())
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    public void testGetAllOrdersReturnsOrdersView() {
        UUID sessionId = UuidCreator.getTimeOrderedEpoch();
        Order order = Order.builder().id(1L).total(BigDecimal.valueOf(100)).build();
        when(orderService.findBySessionId(sessionId)).thenReturn(Flux.just(order));
        when(orderService.getOrderItems(1L)).thenReturn(Flux.empty());

        webTestClient.get().uri("/orders")
                .cookie("SESSION_ID", sessionId.toString())
                .exchange()
                .expectStatus().isOk();
    }
}
