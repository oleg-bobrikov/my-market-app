package ru.yandex.practicum.payment.mymarket.controller;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.csrf;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.mockUser;

public class ItemControllerTest extends BaseWebFluxTest {

    @Test
    public void getItems_WhenSortSelected_PersistsSortInView() {
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
    public void updateCartItem_WhenActionPlus_RedirectsToItemsWithPreservedSort() {
        when(cartService.updateCartItem(any(), any(), any())).thenReturn(Mono.empty());

        webTestClient.mutateWith(csrf()).mutateWith(mockUser()).post().uri(uriBuilder -> uriBuilder.path("/items")
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
    public void updateCartItem_WhenActionPlusByFormData_RedirectsToItemDetails() {
        when(cartService.updateCartItem(any(), any(), any())).thenReturn(Mono.empty());

        webTestClient.mutateWith(csrf()).mutateWith(mockUser()).post().uri("/items/1")
                .contentType(org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED)
                .body(org.springframework.web.reactive.function.BodyInserters.fromFormData("action", "PLUS"))
                .cookie("SESSION_ID", "00000000-0000-0000-0000-000000000001")
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().valueEquals("Location", "/items/1");
    }
}
