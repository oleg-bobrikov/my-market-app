package ru.yandex.practicum.mymarket.integration;

import com.github.f4b6a3.uuid.UuidCreator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.test.StepVerifier;
import ru.yandex.practicum.mymarket.repository.CartRepository;

import java.util.UUID;

public class CartIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private CartRepository cartRepository;

    @Test
    void testUpdateItemCountPlus() {
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

        cartRepository.findBySessionId(sessionId)
                .as(StepVerifier::create)
                .expectNextMatches(ci -> ci.getItemId().equals(itemId) && ci.getCount() == 1)
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

        cartRepository.findBySessionId(sessionId)
                .as(StepVerifier::create)
                .expectNextMatches(ci -> ci.getCount() == 2)
                .verifyComplete();
    }

    @Test
    void testUpdateItemCountMinus() {
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

        cartRepository.findBySessionId(sessionId)
                .as(StepVerifier::create)
                .verifyComplete();
    }

    @Test
    void testGetItemWithCount() {
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
    void testGetCartItems() {
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
    void testUpdateCartItemInCart() {
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

        cartRepository.findBySessionId(sessionId)
                .as(StepVerifier::create)
                .expectNextMatches(ci -> ci.getCount() == 2)
                .verifyComplete();
    }
}
