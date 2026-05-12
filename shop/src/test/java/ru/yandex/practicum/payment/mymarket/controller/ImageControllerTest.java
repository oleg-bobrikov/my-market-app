package ru.yandex.practicum.payment.mymarket.controller;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import reactor.core.publisher.Mono;

import static org.mockito.Mockito.when;

public class ImageControllerTest extends BaseWebFluxTest {

    @Test
    public void getImage_WhenImageExists_ReturnsImageContent() {
        byte[] imageBytes = new byte[]{1, 2, 3};
        when(imageService.getImage("1.jpg")).thenReturn(Mono.just(imageBytes));

        webTestClient.get().uri("/api/images/1.jpg")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.IMAGE_JPEG_VALUE)
                .expectBody(byte[].class).isEqualTo(imageBytes);
    }

    @Test
    public void getImage_WhenImageMissing_ReturnsNotFound() {
        when(imageService.getImage("non_existent.jpg")).thenReturn(Mono.empty());

        webTestClient.get().uri("/api/images/non_existent.jpg")
                .exchange()
                .expectStatus().isNotFound();
    }
}
