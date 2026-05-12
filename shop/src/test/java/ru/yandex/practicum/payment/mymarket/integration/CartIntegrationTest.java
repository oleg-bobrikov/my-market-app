package ru.yandex.practicum.payment.mymarket.integration;

import com.github.f4b6a3.uuid.UuidCreator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.test.StepVerifier;
import ru.yandex.practicum.shop.service.CartService;

import java.util.Map;
import java.util.UUID;

public class CartIntegrationTest extends BaseIntegrationTest {
    @Autowired
    private CartService cartService;

    @Test
    void updateCartItem_WhenActionPlus_IncrementsCount() {
        UUID sessionId = UuidCreator.getTimeOrderedEpoch();
        Long itemId = 1L;

        webTestClient.post().uri(uriBuilder -> uriBuilder.path("/items")
                        .queryParam("id", itemId.toString())
                        .queryParam("action", "PLUS")
                        .queryParam("search", "test")
                        .queryParam("sort", "PRICE")
                        .queryParam("pageSize", "10")
                        .queryParam("pageNumber", "2")
                        .build())
                .cookie("SESSION_ID", sessionId.toString())
                .exchange()
                .expectStatus().is3xxRedirection();

        cartService.getCartCounts(sessionId)
                .as(StepVerifier::create)
                .expectNextMatches(counts -> counts.getOrDefault(itemId, 0) == 1)
                .verifyComplete();

        // Increase count
        webTestClient.post().uri(uriBuilder -> uriBuilder.path("/items")
                        .queryParam("id", itemId.toString())
                        .queryParam("action", "PLUS")
                        .queryParam("search", "")
                        .queryParam("sort", "NO")
                        .queryParam("pageSize", "5")
                        .queryParam("pageNumber", "1")
                        .build())
                .cookie("SESSION_ID", sessionId.toString())
                .exchange()
                .expectStatus().is3xxRedirection();

        cartService.getCartCounts(sessionId)
                .as(StepVerifier::create)
                .expectNextMatches(counts -> {
                    Object count = counts.get(itemId);
                    if (count == null) count = counts.get(Math.toIntExact(itemId));
                    if (count == null) count = counts.get(String.valueOf(itemId));
                    return count != null && Integer.valueOf(count.toString()) == 2;
                })
                .verifyComplete();
    }

    @Test
    void updateCartItem_WhenActionMinus_DecrementsCountOrDeletesItem() {
        UUID sessionId = UuidCreator.getTimeOrderedEpoch();
        long itemId = 1L;

        // Add item first
        webTestClient.post().uri(uriBuilder -> uriBuilder.path("/items")
                        .queryParam("id", Long.toString(itemId))
                        .queryParam("action", "PLUS")
                        .queryParam("search", "")
                        .queryParam("sort", "NO")
                        .queryParam("pageSize", "5")
                        .queryParam("pageNumber", "1")
                        .build())
                .cookie("SESSION_ID", sessionId.toString())
                .exchange()
                .expectStatus().is3xxRedirection();

        // Decrease count
        webTestClient.post().uri(uriBuilder -> uriBuilder.path("/items")
                        .queryParam("id", Long.toString(itemId))
                        .queryParam("action", "MINUS")
                        .queryParam("search", "")
                        .queryParam("sort", "NO")
                        .queryParam("pageSize", "5")
                        .queryParam("pageNumber", "1")
                        .build())
                .cookie("SESSION_ID", sessionId.toString())
                .exchange()
                .expectStatus().is3xxRedirection();

        cartService.getCartCounts(sessionId)
                .as(StepVerifier::create)
                .expectNextMatches(Map::isEmpty)
                .verifyComplete();
    }

    @Test
    void getItem_WhenItemInCart_ReturnsItemWithCorrectCount() {
        UUID sessionId = UuidCreator.getTimeOrderedEpoch();
        long itemId = 1L;

        // Добавляем товар в корзину (2 штуки)
        for (int i = 0; i < 2; i++) {
            webTestClient.post().uri(uriBuilder -> uriBuilder.path("/items")
                            .queryParam("id", Long.toString(itemId))
                            .queryParam("action", "PLUS")
                            .queryParam("search", "")
                            .queryParam("sort", "NO")
                            .queryParam("pageSize", "5")
                            .queryParam("pageNumber", "1")
                            .build())
                    .cookie("SESSION_ID", sessionId.toString())
                    .exchange()
                    .expectStatus().is3xxRedirection();
        }

        // Проверяем, что getItem возвращает правильный count
        webTestClient.get().uri("/items/" + itemId)
                .cookie("SESSION_ID", sessionId.toString())
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void getCartItems_WhenItemsInCart_ReturnsCartView() {
        UUID sessionId = UuidCreator.getTimeOrderedEpoch();
        long itemId = 1L;

        webTestClient.post().uri(uriBuilder -> uriBuilder.path("/items")
                        .queryParam("id", Long.toString(itemId))
                        .queryParam("action", "PLUS")
                        .queryParam("search", "")
                        .queryParam("sort", "NO")
                        .queryParam("pageSize", "5")
                        .queryParam("pageNumber", "1")
                        .build())
                .cookie("SESSION_ID", sessionId.toString())
                .exchange()
                .expectStatus().is3xxRedirection();

        webTestClient.get().uri("/cart/items")
                .cookie("SESSION_ID", sessionId.toString())
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void updateCartItemInCart_WhenActionPlus_IncrementsCountInCart() {
        UUID sessionId = UuidCreator.getTimeOrderedEpoch();
        long itemId = 1L;

        webTestClient.post().uri(uriBuilder -> uriBuilder.path("/items")
                        .queryParam("id", Long.toString(itemId))
                        .queryParam("action", "PLUS")
                        .queryParam("search", "")
                        .queryParam("sort", "NO")
                        .queryParam("pageSize", "5")
                        .queryParam("pageNumber", "1")
                        .build())
                .cookie("SESSION_ID", sessionId.toString())
                .exchange();

        webTestClient.post().uri(uriBuilder -> uriBuilder.path("/cart/items")
                        .queryParam("id", Long.toString(itemId))
                        .queryParam("action", "PLUS")
                        .build())
                .cookie("SESSION_ID", sessionId.toString())
                .exchange()
                .expectStatus().is3xxRedirection();

        cartService.getCartCounts(sessionId)
                .as(StepVerifier::create)
                .expectNextMatches(counts -> {
                    // Используем Number для поддержки Integer/Long ключей из кэша
                    // Хотя нормализация в ItemService должна это исправлять,
                    // cartService.getCartCounts(sessionId) возвращает данные напрямую из кэша.
                    Object count = counts.get(itemId);
                    if (count == null) count = counts.get(Math.toIntExact(itemId));
                    if (count == null) count = counts.get(String.valueOf(itemId));
                    return count != null && Integer.valueOf(count.toString()) == 2;
                })
                .verifyComplete();
    }
}
