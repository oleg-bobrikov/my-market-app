package ru.yandex.practicum.shop.controller;

import org.junit.jupiter.api.Test;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.shop.dto.ItemDto;
import ru.yandex.practicum.shop.model.Item;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.csrf;

public class CartButtonVisibilityTest extends BaseWebFluxTest {

    @Test
    public void getCartItems_WhenInsufficientFunds_ShowsErrorAndHidesButton() {
        Long userId = 1L;
        Item item = Item.builder().id(1L).title("Item 1").price(BigDecimal.valueOf(100)).count(1).build();
        List<Item> items = List.of(item);
        ItemDto itemDto = ItemDto.builder().id(1L).title("Item 1").price(BigDecimal.valueOf(100)).count(1).build();

        when(itemService.getCartItems(userId)).thenReturn(Flux.fromIterable(items));
        when(cartService.getTotalPrice(items)).thenReturn(Mono.just(BigDecimal.valueOf(100)));
        when(itemMapper.toDto(item)).thenReturn(itemDto);
        // Баланс 50, нужно 100
        when(paymentClient.getBalance(userId)).thenReturn(Mono.just(BigDecimal.valueOf(50)));

        webTestClient
                .mutateWith(SecurityMockServerConfigurers.mockAuthentication(auth(1L)))
                .get().uri("/cart/items")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .value(html -> {
                    assert html.contains("на балансе недостаточно средств");
                    assert !html.contains("Купить");
                });
    }

    @Test
    public void getCartItems_WhenPaymentServiceUnavailable_ShowsErrorAndHidesButton() {
        Long userId = 1L;
        Item item = Item.builder().id(1L).title("Item 1").price(BigDecimal.valueOf(100)).count(1).build();
        List<Item> items = List.of(item);
        ItemDto itemDto = ItemDto.builder().id(1L).title("Item 1").price(BigDecimal.valueOf(100)).count(1).build();

        when(itemService.getCartItems(userId)).thenReturn(Flux.fromIterable(items));
        when(cartService.getTotalPrice(items)).thenReturn(Mono.just(BigDecimal.valueOf(100)));
        when(itemMapper.toDto(item)).thenReturn(itemDto);
        // Сервис платежей возвращает ошибку
        when(paymentClient.getBalance(userId)).thenReturn(Mono.error(new RuntimeException("Service Down")));

        webTestClient
                .mutateWith(SecurityMockServerConfigurers.mockAuthentication(auth(1L)))
                .get().uri("/cart/items")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .value(html -> {
                    assert html.contains("сервис платежей недоступен");
                    assert !html.contains("Купить");
                });
    }

    @Test
    public void buy_WhenInsufficientFunds_ShowsErrorAndHidesButton() {
        Long userId = 1L;
        BigDecimal total = BigDecimal.valueOf(200);

        when(orderService.buy(userId)).thenReturn(Mono.error(new ru.yandex.practicum.shop.exception.InsufficientFundsException("на балансе недостаточно средств")));
        when(itemService.getCartItems(userId)).thenReturn(Flux.just(new ru.yandex.practicum.shop.model.Item()));
        when(itemMapper.toDto(any())).thenReturn(new ru.yandex.practicum.shop.dto.ItemDto());
        when(cartService.getTotalPrice(any())).thenReturn(Mono.just(total));

        webTestClient.mutateWith(csrf())
                .mutateWith(SecurityMockServerConfigurers.mockAuthentication(auth(1L)))
                .post().uri("/buy")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .value(html -> {
                    assert html.contains("на балансе недостаточно средств");
                    assert !html.contains("Купить");
                });
    }

    @Test
    public void buy_WhenPaymentServiceUnavailable_ShowsErrorAndHidesButton() {
        Long userId = 1L;
        BigDecimal total = BigDecimal.valueOf(100);

        when(orderService.buy(userId)).thenReturn(Mono.error(new ru.yandex.practicum.shop.exception.PaymentServiceException("Service down")));
        when(itemService.getCartItems(userId)).thenReturn(Flux.just(new ru.yandex.practicum.shop.model.Item()));
        when(itemMapper.toDto(any())).thenReturn(new ru.yandex.practicum.shop.dto.ItemDto());
        when(cartService.getTotalPrice(any())).thenReturn(Mono.just(total));

        webTestClient.mutateWith(csrf())
                .mutateWith(SecurityMockServerConfigurers.mockAuthentication(auth(1L)))
                .post().uri("/buy")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .value(html -> {
                    assert html.contains("сервис платежей недоступен");
                    assert !html.contains("Купить");
                });
    }
}
