package ru.yandex.practicum.payment.mymarket.controller;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

public class LoginTemplateTest extends BaseWebFluxTest {

    @Test
    public void testLoginWithoutParamsDoesNotShowSuccessMessage() {
        webTestClient.get().uri("/login")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .value(body -> {
                    // Проверяем, что сообщение об успехе НЕ отображается
                    if (body.contains("Регистрация прошла успешно! Теперь вы можете войти.")) {
                        throw new AssertionError("Success message should not be visible when no 'registered' param is present");
                    }
                });
    }

    @Test
    public void testLoginWithRegisteredParamShowsSuccessMessage() {
        webTestClient.get().uri("/login?registered=true")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .value(body -> {
                    // Проверяем, что сообщение об успехе отображается
                    if (!body.contains("Регистрация прошла успешно! Теперь вы можете войти.")) {
                        throw new AssertionError("Success message should be visible when 'registered' param is present");
                    }
                });
    }

    @Test
    public void testLoginWithLogoutParamShowsLogoutMessage() {
        webTestClient.get().uri("/login?logout=true")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .value(body -> {
                    if (!body.contains("Вы успешно вышли из приложения.")) {
                        throw new AssertionError("Logout message should be visible when 'logout' param is present");
                    }
                });
    }

    @Test
    public void testLoginWithoutParamsDoesNotShowLogoutMessage() {
        webTestClient.get().uri("/login")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .value(body -> {
                    if (body.contains("Вы успешно вышли из приложения.")) {
                        throw new AssertionError("Logout message should not be visible when no 'logout' param is present");
                    }
                });
    }
}
