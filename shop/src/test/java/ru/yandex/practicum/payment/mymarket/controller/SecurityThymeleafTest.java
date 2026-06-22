package ru.yandex.practicum.payment.mymarket.controller;

import org.junit.jupiter.api.Test;
import org.springframework.security.test.context.support.WithMockUser;
import reactor.core.publisher.Flux;
import ru.yandex.practicum.shop.dto.ItemDto;
import ru.yandex.practicum.shop.model.SortType;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class SecurityThymeleafTest extends BaseWebFluxTest {

    @Test
    @WithMockUser(username = "testUser")
    public void getItems_WhenAuthenticated_ShowsUserInfoAndCartControls() {
        ItemDto itemDto = new ItemDto(1L, "Title", "Desc", "img", BigDecimal.TEN, 0);
        ru.yandex.practicum.shop.model.Item item = ru.yandex.practicum.shop.model.Item.builder()
                .id(1L).title("Title").description("Desc").imgPath("img").price(BigDecimal.TEN).count(0).build();

        when(itemService.getItems(any(), any(), any())).thenReturn(Flux.just(item));
        when(itemMapper.toDto(any())).thenReturn(itemDto);

        webTestClient.get().uri("/")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .value(html -> {
                    assert html.contains("testUser");
                    assert html.contains("Выйти");
                    assert html.contains("Корзина");
                    assert html.contains("Заказы");
                    // Проверяем наличие формы управления товаром
                    assert html.contains("action=\"/items\"");
                    assert html.contains("value=\"MINUS\"");
                    assert html.contains("value=\"PLUS\"");
                });
    }

    @Test
    public void getItems_WhenAnonymous_ShowsLoginButtonAndHidesCartControls() {
        when(itemService.getItems(any(), any(), any())).thenReturn(Flux.empty());

        webTestClient.get().uri("/")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .value(html -> {
                    assert !html.contains("Выйти");
                    assert html.contains("Войти");
                    
                    // Проверяем, что элементы корзины скрыты
                    assert !html.contains("Корзина");
                    assert !html.contains("Заказы");
                    assert !html.contains("action=\"/items\"");
                    assert !html.contains("value=\"MINUS\"");
                    assert !html.contains("value=\"PLUS\"");
                });
    }
}
