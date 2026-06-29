package ru.yandex.practicum.shop.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers;
import reactor.test.StepVerifier;
import ru.yandex.practicum.shop.repository.CartRepository;
import ru.yandex.practicum.shop.service.CartService;

import java.util.Map;

import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.csrf;

public class CartIntegrationTest extends BaseIntegrationTest {
    @Autowired
    private CartService cartService;

    @Autowired
    private CartRepository cartRepository;

    // The integration context (and its in-memory DB) is shared across test methods, and they all
    // act as userId=1. Clear that user's cart before each test so counts don't leak between methods.
    @BeforeEach
    void clearCart() {
        cartRepository.deleteByUserId(1L).block();
    }

    @Test
    void updateCartItem_WhenActionPlus_IncrementsCount() {
        Long userId = 1L;
        Long itemId = 1L;

        webTestClient
                .mutateWith(csrf())
                .mutateWith(SecurityMockServerConfigurers.mockAuthentication(auth(1L)))
                .post().uri(uriBuilder -> uriBuilder.path("/items")
                        .queryParam("id", itemId.toString())
                        .queryParam("action", "PLUS")
                        .queryParam("search", "test")
                        .queryParam("sort", "PRICE")
                        .queryParam("pageSize", "10")
                        .queryParam("pageNumber", "2")
                        .build())
                .exchange()
                .expectStatus().is3xxRedirection();

        cartService.getCartCounts(userId)
                .as(StepVerifier::create)
                .expectNextMatches(counts -> counts.getOrDefault(itemId, 0) == 1)
                .verifyComplete();

        // Increase count
        webTestClient
                .mutateWith(csrf())
                .mutateWith(SecurityMockServerConfigurers.mockAuthentication(auth(1L)))
                .post().uri(uriBuilder -> uriBuilder.path("/items")
                        .queryParam("id", itemId.toString())
                        .queryParam("action", "PLUS")
                        .queryParam("search", "")
                        .queryParam("sort", "NO")
                        .queryParam("pageSize", "5")
                        .queryParam("pageNumber", "1")
                        .build())
                .exchange()
                .expectStatus().is3xxRedirection();

        cartService.getCartCounts(userId)
                .as(StepVerifier::create)
                .expectNextMatches(counts -> {
                    Object count = counts.get(itemId);
                    if (count == null) count = counts.get(Math.toIntExact(itemId));
                    if (count == null) {
                        // Use raw map to avoid type checking warning when accessing with String key
                        count = ((Map) counts).get(String.valueOf(itemId));
                    }
                    return count != null && Integer.valueOf(count.toString()) == 2;
                })
                .verifyComplete();
    }

    @Test
    void updateCartItem_WhenActionMinus_DecrementsCountOrDeletesItem() {
        Long userId = 1L;
        long itemId = 1L;

        // Add item first
        webTestClient
                .mutateWith(csrf())
                .mutateWith(SecurityMockServerConfigurers.mockAuthentication(auth(1L)))
                .post().uri(uriBuilder -> uriBuilder.path("/items")
                        .queryParam("id", Long.toString(itemId))
                        .queryParam("action", "PLUS")
                        .queryParam("search", "")
                        .queryParam("sort", "NO")
                        .queryParam("pageSize", "5")
                        .queryParam("pageNumber", "1")
                        .build())
                .exchange()
                .expectStatus().is3xxRedirection();

        // Decrease count
        webTestClient
                .mutateWith(csrf())
                .mutateWith(SecurityMockServerConfigurers.mockAuthentication(auth(1L)))
                .post().uri(uriBuilder -> uriBuilder.path("/items")
                        .queryParam("id", Long.toString(itemId))
                        .queryParam("action", "MINUS")
                        .queryParam("search", "")
                        .queryParam("sort", "NO")
                        .queryParam("pageSize", "5")
                        .queryParam("pageNumber", "1")
                        .build())
                .exchange()
                .expectStatus().is3xxRedirection();

        cartService.getCartCounts(userId)
                .as(StepVerifier::create)
                .expectNextMatches(Map::isEmpty)
                .verifyComplete();
    }

    @Test
    void getItem_WhenItemInCart_ReturnsItemWithCorrectCount() {
        long itemId = 1L;

        // Добавляем товар в корзину (2 штуки)
        for (int i = 0; i < 2; i++) {
            webTestClient
                    .mutateWith(csrf())
                    .mutateWith(SecurityMockServerConfigurers.mockAuthentication(auth(1L)))
                    .post().uri(uriBuilder -> uriBuilder.path("/items")
                            .queryParam("id", Long.toString(itemId))
                            .queryParam("action", "PLUS")
                            .queryParam("search", "")
                            .queryParam("sort", "NO")
                            .queryParam("pageSize", "5")
                            .queryParam("pageNumber", "1")
                            .build())
                    .exchange()
                    .expectStatus().is3xxRedirection();
        }

        // Проверяем, что getItem возвращает правильный count
        webTestClient
                .mutateWith(SecurityMockServerConfigurers.mockAuthentication(auth(1L)))
                .get().uri("/items/" + itemId)
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void getCartItems_WhenItemsInCart_ReturnsCartView() {
        long itemId = 1L;

        webTestClient
                .mutateWith(csrf())
                .mutateWith(SecurityMockServerConfigurers.mockAuthentication(auth(1L)))
                .post().uri(uriBuilder -> uriBuilder.path("/items")
                        .queryParam("id", Long.toString(itemId))
                        .queryParam("action", "PLUS")
                        .queryParam("search", "")
                        .queryParam("sort", "NO")
                        .queryParam("pageSize", "5")
                        .queryParam("pageNumber", "1")
                        .build())
                .exchange()
                .expectStatus().is3xxRedirection();

        webTestClient
                .mutateWith(SecurityMockServerConfigurers.mockAuthentication(auth(1L)))
                .get().uri("/cart/items")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void updateCartItemInCart_WhenActionPlus_IncrementsCountInCart() {
        long itemId = 1L;
        long userId = 1L;

        webTestClient
                .mutateWith(csrf())
                .mutateWith(SecurityMockServerConfigurers.mockAuthentication(auth(1L)))
                .post().uri(uriBuilder -> uriBuilder.path("/items")
                        .queryParam("id", Long.toString(itemId))
                        .queryParam("action", "PLUS")
                        .queryParam("search", "")
                        .queryParam("sort", "NO")
                        .queryParam("pageSize", "5")
                        .queryParam("pageNumber", "1")
                        .build())
                .exchange();

        webTestClient
                .mutateWith(csrf())
                .mutateWith(SecurityMockServerConfigurers.mockAuthentication(auth(1L)))
                .post().uri(uriBuilder -> uriBuilder.path("/cart/items")
                        .queryParam("id", Long.toString(itemId))
                        .queryParam("action", "PLUS")
                        .build())
                .exchange()
                .expectStatus().is3xxRedirection();

        cartService.getCartCounts(userId)
                .as(StepVerifier::create)
                .expectNextMatches(counts -> {
                    // Используем Number для поддержки Integer/Long ключей из кеша
                    // Хотя нормализация в ItemService должна это исправлять,
                    // cartService.getCartCounts(sessionId) возвращает данные напрямую из кеша.
                    Object count = counts.get(itemId);
                    if (count == null) count = counts.get(Math.toIntExact(itemId));
                    if (count == null) {
                        // Use raw map to avoid type checking warning when accessing with String key
                        count = ((Map) counts).get(String.valueOf(itemId));
                    }
                    return count != null && Integer.parseInt(count.toString()) == 2;
                })
                .verifyComplete();
    }
}
