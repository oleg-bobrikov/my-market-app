package ru.yandex.practicum.shop.integration;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ItemIntegrationTest extends BaseIntegrationTest {

    @Test
    void getItems_WhenSortAlpha_ReturnsItemsSortedByTitle() {
        webTestClient.get()
                .uri("/items?search=&sort=ALPHA&pageSize=100")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.TEXT_HTML)
                .expectBody(String.class)
                .consumeWith(result -> {
                    String body = result.getResponseBody();
                    assertNotNull(body);

                    List<String> titles = extractTitles(body);
                    assertTrue(titles.size() > 1, "Should have more than one item to check sorting");

                    for (int i = 0; i < titles.size() - 1; i++) {
                        String current = titles.get(i).toLowerCase();
                        String next = titles.get(i + 1).toLowerCase();
                        assertTrue(current.compareTo(next) <= 0,
                                String.format("Items not sorted at index %d: '%s' should be before '%s'", i, current, next));
                    }
                });
    }
}
