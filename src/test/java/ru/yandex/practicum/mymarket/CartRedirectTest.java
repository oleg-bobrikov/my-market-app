package ru.yandex.practicum.mymarket;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class CartRedirectTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void testRedirectToItemsWhenCartIsEmptyOnGet() throws Exception {
        UUID sessionId = UUID.randomUUID();

        // Доступ к корзине без товаров должен перенаправлять на /items
        mockMvc.perform(get("/cart/items")
                        .cookie(new jakarta.servlet.http.Cookie("SESSION_ID", sessionId.toString())))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/items"));
    }

    @Test
    void testRedirectToItemsWhenCartBecomesEmptyOnUpdate() throws Exception {
        UUID sessionId = UUID.randomUUID();
        Long itemId = 1L;

        // Добавляем товар
        mockMvc.perform(post("/items")
                        .param("id", itemId.toString())
                        .param("action", "PLUS")
                        .cookie(new jakarta.servlet.http.Cookie("SESSION_ID", sessionId.toString())))
                .andExpect(status().is3xxRedirection());

        // Удаляем товар (action=DELETE)
        mockMvc.perform(post("/cart/items")
                        .param("id", itemId.toString())
                        .param("action", "DELETE")
                        .cookie(new jakarta.servlet.http.Cookie("SESSION_ID", sessionId.toString())))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/items"));
    }

    @Test
    void testRedirectToItemsWhenCartBecomesEmptyOnMinus() throws Exception {
        UUID sessionId = UUID.randomUUID();
        Long itemId = 1L;

        // Добавляем товар
        mockMvc.perform(post("/items")
                        .param("id", itemId.toString())
                        .param("action", "PLUS")
                        .cookie(new jakarta.servlet.http.Cookie("SESSION_ID", sessionId.toString())))
                .andExpect(status().is3xxRedirection());

        // Уменьшаем количество до 0 (action=MINUS для товара в количестве 1 удаляет его)
        mockMvc.perform(post("/cart/items")
                        .param("id", itemId.toString())
                        .param("action", "MINUS")
                        .cookie(new jakarta.servlet.http.Cookie("SESSION_ID", sessionId.toString())))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/items"));
    }
}
