package ru.yandex.practicum.shop.controller;

import org.junit.jupiter.api.Test;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.shop.dto.ItemDto;
import ru.yandex.practicum.shop.exception.InsufficientFundsException;
import ru.yandex.practicum.shop.exception.PaymentServiceException;
import ru.yandex.practicum.shop.model.Order;

import java.math.BigDecimal;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.csrf;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

public class OrderControllerTest extends BaseWebFluxTest {

    @Test
    public void buy_WhenSuccessful_RedirectsToOrder() {
        Long userId = 1L;
        BigDecimal total = BigDecimal.valueOf(100);
        Order order = Order.builder().id(123L).total(total).build();

        when(orderService.buy(userId)).thenReturn(Mono.just(order));

        webTestClient
                .mutateWith(csrf())
                .mutateWith(SecurityMockServerConfigurers.mockAuthentication(auth(1L)))
                .post().uri("/buy")
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().valueEquals("Location", "/orders/123");

        verify(orderService).buy(userId);
    }

    @Test
    public void buy_WhenBalanceInsufficient_ReturnsError() {
        Long userId = 1L;
        BigDecimal total = BigDecimal.valueOf(200);

        when(orderService.buy(userId)).thenReturn(Mono.error(new InsufficientFundsException("на балансе недостаточно средств")));
        when(itemService.getCartItems(userId)).thenReturn(Flux.just(new ru.yandex.practicum.shop.model.Item()));
        when(itemMapper.toDto(any())).thenReturn(new ItemDto());
        when(cartService.getTotalPrice(any())).thenReturn(Mono.just(total));

        webTestClient
                .mutateWith(csrf())
                .mutateWith(SecurityMockServerConfigurers.mockAuthentication(auth(1L)))
                .post().uri("/buy")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class).value(body ->
                        assertTrue(body.contains("на балансе недостаточно средств"),
                                "Body does not contain expected error message. Body: " + body));
    }

    @Test
    public void buy_WhenServiceUnavailable_ReturnsError() {
        Long userId = 1L;
        BigDecimal total = BigDecimal.valueOf(100);

        when(orderService.buy(userId)).thenReturn(Mono.error(new PaymentServiceException("Service down")));
        when(itemService.getCartItems(userId)).thenReturn(Flux.just(new ru.yandex.practicum.shop.model.Item()));
        when(itemMapper.toDto(any())).thenReturn(new ItemDto());
        when(cartService.getTotalPrice(any())).thenReturn(Mono.just(total));

        webTestClient
                .mutateWith(csrf())
                .mutateWith(SecurityMockServerConfigurers.mockAuthentication(auth(1L)))
                .post().uri("/buy")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class).value(body ->
                        assertTrue(body.contains("сервис платежей недоступен"),
                                "Body does not contain expected error message. Body: " + body));
    }

    @Test
    public void getOrder_WhenOrderExists_ReturnsOrderView() {
        Long userId = 1L;
        Order order = Order.builder().id(1L).total(BigDecimal.valueOf(100)).build();
        when(orderService.getOrderByIdAndSessionId(1L, userId)).thenReturn(Mono.just(order));
        when(orderService.getOrderItems(1L)).thenReturn(Flux.empty());

        webTestClient
                .mutateWith(SecurityMockServerConfigurers.mockAuthentication(auth(1L)))
                .get().uri("/orders/1")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    public void getAllOrders_WhenOrdersExist_ReturnsOrdersView() {
        Long userId = 1L;
        Order order = Order.builder().id(1L).total(BigDecimal.valueOf(100)).build();
        when(orderService.findByUserId(userId)).thenReturn(Flux.just(order));
        when(orderService.getOrderItems(1L)).thenReturn(Flux.empty());

        webTestClient
                .mutateWith(SecurityMockServerConfigurers.mockAuthentication(auth(1L)))
                .get().uri("/orders")
                .exchange()
                .expectStatus().isOk();
    }
}
