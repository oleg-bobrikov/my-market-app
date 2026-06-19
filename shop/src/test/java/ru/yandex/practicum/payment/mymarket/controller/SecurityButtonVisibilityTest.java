package ru.yandex.practicum.payment.mymarket.controller;

import org.junit.jupiter.api.Test;
import org.springframework.security.test.context.support.WithMockUser;
import ru.yandex.practicum.shop.model.Item;
import reactor.core.publisher.Flux;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class SecurityButtonVisibilityTest extends BaseWebFluxTest {

    @Test
    public void mainPage_WhenAnonymous_ShowsLoginButton() {
        when(itemService.getItems(any(), any(), any())).thenReturn(Flux.empty());

        webTestClient.get().uri("/")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .value(html -> {
                    System.out.println("[DEBUG_LOG] HTML Content: " + html);
                    // Проверяем наличие кнопки "Войти"
                    assert html.contains("Войти") : "Login button not found for anonymous user";
                    // Проверяем отсутствие кнопки "Выйти"
                    assert !html.contains("Выйти") : "Logout button found for anonymous user";
                });
    }

    @Test
    @WithMockUser(username = "testUser")
    public void mainPage_WhenAuthenticated_ShowsLogoutButtonAndUsername() {
        when(itemService.getItems(any(), any(), any())).thenReturn(Flux.empty());

        webTestClient.get().uri("/")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .value(html -> {
                    // Проверяем наличие кнопки "Выйти"
                    assert html.contains("Выйти") : "Logout button not found for authenticated user";
                    // Проверяем наличие имени пользователя
                    assert html.contains("testUser") : "Username not found for authenticated user";
                    // Проверяем отсутствие кнопки "Войти"
                    assert !html.contains("Войти") : "Login button found for authenticated user";
                });
    }
}
