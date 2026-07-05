package ru.yandex.practicum.shop.controller;

import org.junit.jupiter.api.Test;
import org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.shop.dto.ItemDto;
import ru.yandex.practicum.shop.model.CartAction;
import ru.yandex.practicum.shop.model.Item;

import java.math.BigDecimal;
import java.util.List;

import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.csrf;

import static org.mockito.Mockito.*;

public class CartControllerTest extends BaseWebFluxTest {

    @Test
    public void getCartItems_WhenPaymentServiceError_ReturnsCartViewWithErrorMessage() {
        Long userId = 1L;
        Item item = Item.builder().id(1L).title("Item 1").price(BigDecimal.TEN).count(1).build();
        List<Item> items = List.of(item);
        ItemDto itemDto = ItemDto.builder().id(1L).title("Item 1").price(BigDecimal.TEN).count(1).build();

        when(itemService.getCartItems(userId)).thenReturn(Flux.fromIterable(items));
        when(cartService.getTotalPrice(items)).thenReturn(Mono.just(BigDecimal.TEN));
        when(itemMapper.toDto(item)).thenReturn(itemDto);
        when(paymentClient.getBalance(userId)).thenReturn(Mono.error(new RuntimeException("Service Unavailable")));

        webTestClient
                .mutateWith(SecurityMockServerConfigurers.mockAuthentication(auth(1L)))
                .get().uri("/cart/items")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .value(body -> {
                    assert body.contains("сервис платежей недоступен");
                });
    }

    @Test
    public void getCartItems_WhenItemsExist_ReturnsCartView() {
        Long userId = 1L;
        Item item = Item.builder().id(1L).title("Item 1").price(BigDecimal.TEN).count(1).build();
        List<Item> items = List.of(item);
        ItemDto itemDto = ItemDto.builder().id(1L).title("Item 1").price(BigDecimal.TEN).count(1).build();

        when(itemService.getCartItems(userId)).thenReturn(Flux.fromIterable(items));
        when(cartService.getTotalPrice(items)).thenReturn(Mono.just(BigDecimal.TEN));
        when(itemMapper.toDto(item)).thenReturn(itemDto);
        when(paymentClient.getBalance(userId)).thenReturn(Mono.just(BigDecimal.valueOf(100)));

        webTestClient
                .mutateWith(SecurityMockServerConfigurers.mockAuthentication(auth(1L)))
                .get().uri("/cart/items")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    public void getCartItems_WhenNoSession_RedirectsToItemsAndCreatesSession() {
        when(itemService.getCartItems(any())).thenReturn(Flux.empty());

        webTestClient
                .mutateWith(SecurityMockServerConfigurers.mockAuthentication(auth(1L)))
                .get().uri("/cart/items")
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().valueEquals("Location", "/items");
    }

    @Test
    public void updateCartItem_WhenActionPlus_RedirectsToCart() {
        Long userId = 1L;
        Item item = Item.builder().id(1L).title("Item 1").price(BigDecimal.TEN).count(1).build();

        when(cartService.updateCartItem(eq(userId), eq(1L), eq(CartAction.PLUS))).thenReturn(Mono.empty());
        when(itemService.getCartItems(userId)).thenReturn(Flux.just(item));

        webTestClient.mutateWith(csrf())
                .mutateWith(SecurityMockServerConfigurers.mockAuthentication(auth(1L)))
                .post().uri(uriBuilder -> uriBuilder.path("/cart/items")
                        .queryParam("id", "1")
                        .queryParam("action", "PLUS")
                        .build())
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().valueEquals("Location", "/cart/items");

        verify(cartService).updateCartItem(eq(userId), eq(1L), eq(CartAction.PLUS));
    }

    @Test
    void getCartItems_WhenCartIsEmpty_RedirectsToItems() {
        Long userId = 1L;
        when(itemService.getCartItems(userId)).thenReturn(Flux.empty());

        webTestClient
                .mutateWith(SecurityMockServerConfigurers.mockAuthentication(auth(1L)))
                .get().uri("/cart/items")
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().valueEquals("Location", "/items");
    }

    @Test
    public void updateCartItem_WhenActionPlusByFormData_RedirectsToCart() {
        Long userId = 1L;
        Item item = Item.builder().id(1L).title("Item 1").price(BigDecimal.TEN).count(1).build();

        when(cartService.updateCartItem(eq(userId), eq(1L), eq(CartAction.PLUS))).thenReturn(Mono.empty());
        when(itemService.getCartItems(userId)).thenReturn(Flux.just(item));

        webTestClient.mutateWith(csrf())
                .mutateWith(SecurityMockServerConfigurers.mockAuthentication(auth(1L)))
                .post().uri("/cart/items")
                .contentType(org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED)
                .body(org.springframework.web.reactive.function.BodyInserters.fromFormData("id", "1")
                        .with("action", "PLUS"))
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().valueEquals("Location", "/cart/items");

        verify(cartService).updateCartItem(eq(userId), eq(1L), eq(CartAction.PLUS));
    }
}
