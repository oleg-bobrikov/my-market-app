package ru.yandex.practicum.mymarket;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import ru.yandex.practicum.mymarket.model.CartItem;
import ru.yandex.practicum.mymarket.repository.CartRepository;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class CartIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CartRepository cartRepository;

    @Test
    void testUpdateItemCountPlus() throws Exception {
        UUID sessionId = UUID.randomUUID();
        Long itemId = 1L;

        mockMvc.perform(post("/items")
                        .param("id", itemId.toString())
                        .param("action", "PLUS")
                        .param("search", "test")
                        .param("sort", "PRICE")
                        .param("pageSize", "10")
                        .param("pageNumber", "2")
                        .cookie(new jakarta.servlet.http.Cookie("SESSION_ID", sessionId.toString())))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/items?search=test&sort=PRICE&pageSize=10&pageNumber=2#item-1"));

        var cartItems = cartRepository.findBySessionId(sessionId);
        assertEquals(1, cartItems.size());
        assertEquals(itemId, cartItems.get(0).getItem().getId());
        assertEquals(1, cartItems.get(0).getCount());

        // Increase count
        mockMvc.perform(post("/items")
                        .param("id", itemId.toString())
                        .param("action", "PLUS")
                        .cookie(new jakarta.servlet.http.Cookie("SESSION_ID", sessionId.toString())))
                .andExpect(status().is3xxRedirection());

        cartItems = cartRepository.findBySessionId(sessionId);
        assertEquals(1, cartItems.size());
        assertEquals(2, cartItems.get(0).getCount());
    }

    @Test
    void testUpdateItemCountMinus() throws Exception {
        UUID sessionId = UUID.randomUUID();
        Long itemId = 1L;

        // Add item first
        mockMvc.perform(post("/items")
                        .param("id", itemId.toString())
                        .param("action", "PLUS")
                        .cookie(new jakarta.servlet.http.Cookie("SESSION_ID", sessionId.toString())))
                .andExpect(status().is3xxRedirection());

        // Decrease count
        mockMvc.perform(post("/items")
                        .param("id", itemId.toString())
                        .param("action", "MINUS")
                        .cookie(new jakarta.servlet.http.Cookie("SESSION_ID", sessionId.toString())))
                .andExpect(status().is3xxRedirection());

        var cartItems = cartRepository.findBySessionId(sessionId);
        assertTrue(cartItems.isEmpty());
    }

    @Test
    void testGetItemWithCount() throws Exception {
        UUID sessionId = UUID.randomUUID();
        Long itemId = 1L;

        // Добавляем товар в корзину (2 штуки)
        mockMvc.perform(post("/items")
                        .param("id", itemId.toString())
                        .param("action", "PLUS")
                        .cookie(new jakarta.servlet.http.Cookie("SESSION_ID", sessionId.toString())))
                .andExpect(status().is3xxRedirection());

        mockMvc.perform(post("/items")
                        .param("id", itemId.toString())
                        .param("action", "PLUS")
                        .cookie(new jakarta.servlet.http.Cookie("SESSION_ID", sessionId.toString())))
                .andExpect(status().is3xxRedirection());

        // Проверяем, что getItem возвращает правильный count
        mockMvc.perform(get("/items/" + itemId)
                        .cookie(new jakarta.servlet.http.Cookie("SESSION_ID", sessionId.toString())))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("item"))
                .andExpect(model().attribute("item", org.hamcrest.Matchers.hasProperty("count", org.hamcrest.Matchers.is(2))));
    }

    @Test
    void testGetCartItems() throws Exception {
        UUID sessionId = UUID.randomUUID();
        Long itemId = 1L;

        // Добавляем товар в корзину
        mockMvc.perform(post("/items")
                        .param("id", itemId.toString())
                        .param("action", "PLUS")
                        .cookie(new jakarta.servlet.http.Cookie("SESSION_ID", sessionId.toString())))
                .andExpect(status().is3xxRedirection());

        // Проверяем страницу корзины
        mockMvc.perform(get("/cart/items")
                        .cookie(new jakarta.servlet.http.Cookie("SESSION_ID", sessionId.toString())))
                .andExpect(status().isOk())
                .andExpect(view().name("cart"))
                .andExpect(model().attributeExists("items"))
                .andExpect(model().attributeExists("total"))
                .andExpect(model().attribute("items", org.hamcrest.Matchers.hasSize(1)))
                .andExpect(model().attribute("total", org.hamcrest.Matchers.notNullValue()));
    }
}
