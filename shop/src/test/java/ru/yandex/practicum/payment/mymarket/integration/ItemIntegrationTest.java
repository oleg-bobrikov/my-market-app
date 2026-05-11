package ru.yandex.practicum.payment.mymarket.integration;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
                    
                   /*
                    Извлекаем названия товаров из HTML.
                    В шаблоне items.html товары отображаются в карточках.
                    */

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

    private List<String> extractTitles(String html) {
        List<String> titles = new ArrayList<>();

        /*
        Ищем заголовки в карточках товаров.
        Обычно это <h5 class="card-title">Название</h5>
         */
        Pattern pattern = Pattern.compile("<h5[^>]*class=\"card-title\"[^>]*>([^<]+)</h5>");
        Matcher matcher = pattern.matcher(html);
        while (matcher.find()) {
            String title = matcher.group(1).trim();
            if (!title.isEmpty()) {
                titles.add(title);
            }
        }
        return titles;
    }
}
