package ru.yandex.practicum.payment.mymarket.controller;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.BodyInserters;

import static org.assertj.core.api.Assertions.assertThat;

public class RegistrationTest extends BaseWebFluxTest {

    @Test
    public void testRegistrationSuccess() {
        org.mockito.BDDMockito.given(inMemoryReactiveUserDetailService.findByUsername(org.mockito.ArgumentMatchers.anyString()))
                .willReturn(reactor.core.publisher.Mono.empty());
        org.mockito.BDDMockito.given(inMemoryReactiveUserDetailService.addUser(org.mockito.ArgumentMatchers.any()))
                .willReturn(reactor.core.publisher.Mono.just(org.springframework.security.core.userdetails.User.withUsername("testuser")
                        .password("encoded")
                        .roles("USER")
                        .build()));

        byte[] sessionCookie = webTestClient.post().uri("/register")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData("username", "testuser")
                        .with("password", "testpass"))
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().valueEquals("Location", "/")
                .expectCookie().exists("SESSION")
                .returnResult(String.class)
                .getResponseCookies().getFirst("SESSION").getValue().getBytes();

        org.mockito.BDDMockito.given(itemService.getItems(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
                .willReturn(reactor.core.publisher.Flux.empty());

        webTestClient.get().uri("/")
                .cookie("SESSION", new String(sessionCookie))
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .value(html -> {
                    assertThat(html).contains("Выйти");
                    assertThat(html).contains("testuser");
                    assertThat(html).doesNotContain("Войти");
                });
    }

    @Test
    public void testRegistrationMissingParams() {
        webTestClient.post().uri("/register")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData("username", "testuser"))
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().valueEquals("Location", "/login");
    }
}
