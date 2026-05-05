package ru.yandex.practicum.mymarket.controller;

import com.github.f4b6a3.uuid.UuidCreator;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.mymarket.model.Order;

import java.math.BigDecimal;
import java.util.UUID;

import static org.mockito.Mockito.*;

public class OrderControllerTest extends BaseWebFluxTest {

    @Test
    public void testBuyRedirectsToOrder() {
        UUID sessionId = UuidCreator.getTimeOrderedEpoch();
        Order order = Order.builder().id(123L).total(BigDecimal.ZERO).build();
        when(orderService.createOrder(sessionId)).thenReturn(Mono.just(order));

        webTestClient.post().uri("/buy")
                .cookie("SESSION_ID", sessionId.toString())
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().valueEquals("Location", "/orders/123");

        verify(orderService).createOrder(sessionId);
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
