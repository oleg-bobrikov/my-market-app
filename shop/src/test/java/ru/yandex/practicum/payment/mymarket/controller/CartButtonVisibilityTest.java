package ru.yandex.practicum.payment.mymarket.controller;

import com.github.f4b6a3.uuid.UuidCreator;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.shop.dto.ItemDto;
import ru.yandex.practicum.shop.model.Item;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class CartButtonVisibilityTest extends BaseWebFluxTest {

    @Test
    public void getCartItems_WhenInsufficientFunds_ShowsErrorAndHidesButton() {
        UUID sessionId = UuidCreator.getTimeOrderedEpoch();
        Item item = Item.builder().id(1L).title("Item 1").price(BigDecimal.valueOf(100)).count(1).build();
        List<Item> items = List.of(item);
        ItemDto itemDto = ItemDto.builder().id(1L).title("Item 1").price(BigDecimal.valueOf(100)).count(1).build();

        when(cartService.getCartItems(sessionId)).thenReturn(Flux.fromIterable(items));
        when(cartService.getTotalPrice(items)).thenReturn(Mono.just(BigDecimal.valueOf(100)));
        when(itemMapper.toDto(item)).thenReturn(itemDto);
        // Баланс 50, нужно 100
        when(paymentClient.getBalance(sessionId)).thenReturn(Mono.just(BigDecimal.valueOf(50)));

        webTestClient.get().uri("/cart/items")
                .cookie("SESSION_ID", sessionId.toString())
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
        UUID sessionId = UuidCreator.getTimeOrderedEpoch();
        Item item = Item.builder().id(1L).title("Item 1").price(BigDecimal.valueOf(100)).count(1).build();
        List<Item> items = List.of(item);
        ItemDto itemDto = ItemDto.builder().id(1L).title("Item 1").price(BigDecimal.valueOf(100)).count(1).build();

        when(cartService.getCartItems(sessionId)).thenReturn(Flux.fromIterable(items));
        when(cartService.getTotalPrice(items)).thenReturn(Mono.just(BigDecimal.valueOf(100)));
        when(itemMapper.toDto(item)).thenReturn(itemDto);
        // Сервис платежей возвращает ошибку
        when(paymentClient.getBalance(sessionId)).thenReturn(Mono.error(new RuntimeException("Service Down")));

        webTestClient.get().uri("/cart/items")
                .cookie("SESSION_ID", sessionId.toString())
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
        UUID sessionId = UuidCreator.getTimeOrderedEpoch();
        BigDecimal total = BigDecimal.valueOf(200);

        when(orderService.buy(sessionId)).thenReturn(Mono.error(new ru.yandex.practicum.shop.exception.InsufficientFundsException("на балансе недостаточно средств")));
        when(cartService.getCartItems(sessionId)).thenReturn(Flux.just(new ru.yandex.practicum.shop.model.Item()));
        when(itemMapper.toDto(any())).thenReturn(new ru.yandex.practicum.shop.dto.ItemDto());
        when(cartService.getTotalPrice(any())).thenReturn(Mono.just(total));

        webTestClient.post().uri("/buy")
                .cookie("SESSION_ID", sessionId.toString())
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
        UUID sessionId = UuidCreator.getTimeOrderedEpoch();
        BigDecimal total = BigDecimal.valueOf(100);

        when(orderService.buy(sessionId)).thenReturn(Mono.error(new ru.yandex.practicum.shop.exception.PaymentServiceException("Service down")));
        when(cartService.getCartItems(sessionId)).thenReturn(Flux.just(new ru.yandex.practicum.shop.model.Item()));
        when(itemMapper.toDto(any())).thenReturn(new ru.yandex.practicum.shop.dto.ItemDto());
        when(cartService.getTotalPrice(any())).thenReturn(Mono.just(total));

        webTestClient.post().uri("/buy")
                .cookie("SESSION_ID", sessionId.toString())
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .value(html -> {
                    assert html.contains("сервис платежей недоступен");
                    assert !html.contains("Купить");
                });
    }
}
