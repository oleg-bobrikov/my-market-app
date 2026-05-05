package ru.yandex.practicum.mymarket.integration;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ItemPaginationTest extends BaseIntegrationTest {

    @Test
    void testPaginationPageSize2() {
        webTestClient.get()
                .uri("/items?search=&sort=ALPHA&pageSize=2&pageNumber=1")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.TEXT_HTML)
                .expectBody(String.class)
                .consumeWith(result -> {
                    String body = result.getResponseBody();
                    List<String> titles = extractTitles(body);
                    assertEquals(2, titles.size(), "Should return exactly 2 items for pageSize=2 on page 1");
                });
    }

    @Test
    void testPaginationSecondPage() {
        // Сначала получаем первую страницу, чтобы знать, какие там товары
        final List<String> firstPageTitles = new java.util.ArrayList<>();
        webTestClient.get()
                .uri("/items?search=&sort=ALPHA&pageSize=2&pageNumber=1")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .consumeWith(result -> firstPageTitles.addAll(extractTitles(result.getResponseBody())));

        // Получаем вторую страницу
        webTestClient.get()
                .uri("/items?search=&sort=ALPHA&pageSize=2&pageNumber=2")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .consumeWith(result -> {
                    String body = result.getResponseBody();
                    List<String> secondPageTitles = extractTitles(body);
                    assertEquals(2, secondPageTitles.size(), "Should return exactly 2 items for pageSize=2 on page 2");
                    
                    // Убеждаемся, что товары на второй странице отличаются от первой
                    for (String title : secondPageTitles) {
                        org.junit.jupiter.api.Assertions.assertFalse(firstPageTitles.contains(title), 
                            "Item '" + title + "' from second page should not be on first page");
                    }
                });
    }

    private List<String> extractTitles(String html) {
        List<String> titles = new java.util.ArrayList<>();
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("<h5[^>]*class=\"card-title\"[^>]*>([^<]+)</h5>");
        java.util.regex.Matcher matcher = pattern.matcher(html);
        while (matcher.find()) {
            String title = matcher.group(1).trim();
            if (!title.isEmpty()) {
                titles.add(title);
            }
        }
        return titles;
    }
}
