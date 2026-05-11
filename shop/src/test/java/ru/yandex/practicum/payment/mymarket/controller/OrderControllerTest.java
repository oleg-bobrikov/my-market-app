package ru.yandex.practicum.payment.mymarket.controller;

import com.github.f4b6a3.uuid.UuidCreator;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.shop.exception.InsufficientFundsException;
import ru.yandex.practicum.shop.exception.PaymentServiceException;
import ru.yandex.practicum.shop.model.Order;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

public class OrderControllerTest extends BaseWebFluxTest {

    @Test
    public void buy_WhenSuccessful_RedirectsToOrder() {
        UUID sessionId = UuidCreator.getTimeOrderedEpoch();
        BigDecimal total = BigDecimal.valueOf(100);
        Order order = Order.builder().id(123L).total(total).build();

        when(orderService.buy(sessionId)).thenReturn(Mono.just(order));

        webTestClient.post().uri("/buy")
                .cookie("SESSION_ID", sessionId.toString())
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().valueEquals("Location", "/orders/123");

        verify(orderService).buy(sessionId);
    }

    @Test
    public void buy_WhenBalanceInsufficient_ReturnsError() {
        UUID sessionId = UuidCreator.getTimeOrderedEpoch();
        BigDecimal total = BigDecimal.valueOf(200);

        when(orderService.buy(sessionId)).thenReturn(Mono.error(new InsufficientFundsException("на балансе недостаточно средств")));
        when(itemService.getCartItems(sessionId)).thenReturn(Flux.just(new ru.yandex.practicum.shop.model.Item()));
        when(itemMapper.toDto(any())).thenReturn(new ru.yandex.practicum.shop.dto.ItemDto());
        when(cartService.getTotalPrice(any())).thenReturn(Mono.just(total));

        webTestClient.post().uri("/buy")
                .cookie("SESSION_ID", sessionId.toString())
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class).value(body ->
                        assertTrue(body.contains("на балансе недостаточно средств"),
                                "Body does not contain expected error message. Body: " + body));
    }

    @Test
    public void buy_WhenServiceUnavailable_ReturnsError() {
        UUID sessionId = UuidCreator.getTimeOrderedEpoch();
        BigDecimal total = BigDecimal.valueOf(100);

        when(orderService.buy(sessionId)).thenReturn(Mono.error(new PaymentServiceException("Service down")));
        when(itemService.getCartItems(sessionId)).thenReturn(Flux.just(new ru.yandex.practicum.shop.model.Item()));
        when(itemMapper.toDto(any())).thenReturn(new ru.yandex.practicum.shop.dto.ItemDto());
        when(cartService.getTotalPrice(any())).thenReturn(Mono.just(total));

        webTestClient.post().uri("/buy")
                .cookie("SESSION_ID", sessionId.toString())
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class).value(body ->
                        assertTrue(body.contains("сервис платежей недоступен"),
                                "Body does not contain expected error message. Body: " + body));
    }

    @Test
    public void getOrder_WhenOrderExists_ReturnsOrderView() {
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
    public void getAllOrders_WhenOrdersExist_ReturnsOrdersView() {
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
