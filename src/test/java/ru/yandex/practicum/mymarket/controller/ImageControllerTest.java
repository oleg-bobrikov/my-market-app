package ru.yandex.practicum.mymarket.controller;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import ru.yandex.practicum.mymarket.BaseWebMvcTest;

import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class ImageControllerTest extends BaseWebMvcTest {

    @Test
    public void testGetImageSuccess() throws Exception {
        byte[] imageBytes = new byte[]{1, 2, 3};
        when(imageService.getImage("1.jpg")).thenReturn(Optional.of(imageBytes));

        mockMvc.perform(get("/api/images/1.jpg"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_JPEG_VALUE))
                .andExpect(content().bytes(imageBytes));
    }

    @Test
    public void testGetImageNotFound() throws Exception {
        when(imageService.getImage("non_existent.jpg")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/images/non_existent.jpg"))
                .andExpect(status().isNotFound());
    }
}
