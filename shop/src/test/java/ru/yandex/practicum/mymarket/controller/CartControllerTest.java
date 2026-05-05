package ru.yandex.practicum.mymarket.controller;

import com.github.f4b6a3.uuid.UuidCreator;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.mymarket.dto.ItemDto;
import ru.yandex.practicum.mymarket.model.CartAction;
import ru.yandex.practicum.mymarket.model.Item;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.*;

public class CartControllerTest extends BaseWebFluxTest {

    @Test
    public void testGetCartItemsReturnsCartView() {
        UUID sessionId = UuidCreator.getTimeOrderedEpoch();
        Item item = Item.builder().id(1L).title("Item 1").price(BigDecimal.TEN).count(1).build();
        List<Item> items = List.of(item);
        ItemDto itemDto = ItemDto.builder().id(1L).title("Item 1").price(BigDecimal.TEN).count(1).build();

        when(cartService.getCartItems(sessionId)).thenReturn(Flux.fromIterable(items));
        when(cartService.getTotalPrice(items)).thenReturn(Mono.just(BigDecimal.TEN));
        when(itemMapper.toDto(item)).thenReturn(itemDto);

        webTestClient.get().uri("/cart/items")
                .cookie("SESSION_ID", sessionId.toString())
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    public void testGetCartItemsCreatesSessionWhenNoSession() {
        when(cartService.getCartItems(any())).thenReturn(Flux.empty());

        webTestClient.get().uri("/cart/items")
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().valueEquals("Location", "/items")
                .expectCookie().exists("SESSION_ID");
    }

    @Test
    public void testUpdateCartItemRedirectsToCart() {
        UUID sessionId = UuidCreator.getTimeOrderedEpoch();
        Item item = Item.builder().id(1L).title("Item 1").price(BigDecimal.TEN).count(1).build();

        when(cartService.updateCartItem(eq(sessionId), eq(1L), eq(CartAction.PLUS))).thenReturn(Mono.empty());
        when(cartService.getCartItems(sessionId)).thenReturn(Flux.just(item));

        webTestClient.post().uri(uriBuilder -> uriBuilder.path("/cart/items")
                        .queryParam("id", "1")
                        .queryParam("action", "PLUS")
                        .build())
                .cookie("SESSION_ID", sessionId.toString())
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().valueEquals("Location", "/cart/items");

        verify(cartService).updateCartItem(eq(sessionId), eq(1L), eq(CartAction.PLUS));
    }

    @Test
    void testRedirectToItemsWhenCartIsEmptyOnGet() {
        UUID sessionId = UuidCreator.getTimeOrderedEpoch();
        when(cartService.getCartItems(sessionId)).thenReturn(Flux.empty());

        webTestClient.get().uri("/cart/items")
                .cookie("SESSION_ID", sessionId.toString())
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().valueEquals("Location", "/items");
    }

    @Test
    public void testUpdateCartItemWithFormData() {
        UUID sessionId = UuidCreator.getTimeOrderedEpoch();
        Item item = Item.builder().id(1L).title("Item 1").price(BigDecimal.TEN).count(1).build();

        when(cartService.updateCartItem(eq(sessionId), eq(1L), eq(CartAction.PLUS))).thenReturn(Mono.empty());
        when(cartService.getCartItems(sessionId)).thenReturn(Flux.just(item));

        webTestClient.post().uri("/cart/items")
                .contentType(org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED)
                .body(org.springframework.web.reactive.function.BodyInserters.fromFormData("id", "1")
                        .with("action", "PLUS"))
                .cookie("SESSION_ID", sessionId.toString())
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().valueEquals("Location", "/cart/items");

        verify(cartService).updateCartItem(eq(sessionId), eq(1L), eq(CartAction.PLUS));
    }
}
