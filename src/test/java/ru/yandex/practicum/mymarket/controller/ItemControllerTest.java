package ru.yandex.practicum.mymarket.controller;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.mymarket.BaseWebFluxTest;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

public class ItemControllerTest extends BaseWebFluxTest {

    @Test
    public void testSortSelectionPersists() {
        when(itemService.getItems(anyString(), any(), any())).thenReturn(Flux.empty());

        webTestClient.get().uri("/items?sort=ALPHA")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class).consumeWith(result -> {
                    String body = result.getResponseBody();
                    assert body != null && body.contains("<option value=\"ALPHA\" selected=\"selected\">по алфавиту</option>");
                });

        webTestClient.get().uri("/items?sort=PRICE")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class).consumeWith(result -> {
                    String body = result.getResponseBody();
                    assert body != null && body.contains("<option value=\"PRICE\" selected=\"selected\">по цене</option>");
                });
    }

    @Test
    public void testUpdateItemCountPreservesSort() {
        when(cartService.updateCartItem(any(), any(), any())).thenReturn(Mono.empty());

        webTestClient.post().uri(uriBuilder -> uriBuilder.path("/items")
                        .queryParam("id", "1")
                        .queryParam("action", "PLUS")
                        .queryParam("sort", "PRICE")
                        .build())
                .cookie("SESSION_ID", "00000000-0000-0000-0000-000000000001")
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().valueEquals("Location", "/items?search=&sort=PRICE&pageSize=5&pageNumber=1#item-1");
    }

    @Test
    public void testUpdateItemCountOnPageWithFormData() {
        when(cartService.updateCartItem(any(), any(), any())).thenReturn(Mono.empty());

        webTestClient.post().uri("/items/1")
                .contentType(org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED)
                .body(org.springframework.web.reactive.function.BodyInserters.fromFormData("action", "PLUS"))
                .cookie("SESSION_ID", "00000000-0000-0000-0000-000000000001")
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().valueEquals("Location", "/items/1");
    }
}
