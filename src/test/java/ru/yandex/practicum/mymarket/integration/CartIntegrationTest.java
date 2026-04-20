package ru.yandex.practicum.mymarket.integration;

import com.github.f4b6a3.uuid.UuidCreator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import ru.yandex.practicum.mymarket.BaseIntegrationTest;
import ru.yandex.practicum.mymarket.repository.CartRepository;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class CartIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CartRepository cartRepository;

    @Test
    void testUpdateItemCountPlus() throws Exception {
        UUID sessionId = UuidCreator.getTimeOrderedEpoch();
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
        assertEquals(itemId, cartItems.getFirst().getItem().getId());
        assertEquals(1, cartItems.getFirst().getCount());

        // Increase count
        mockMvc.perform(post("/items")
                        .param("id", itemId.toString())
                        .param("action", "PLUS")
                        .cookie(new jakarta.servlet.http.Cookie("SESSION_ID", sessionId.toString())))
                .andExpect(status().is3xxRedirection());

        cartItems = cartRepository.findBySessionId(sessionId);
        assertEquals(1, cartItems.size());
        assertEquals(2, cartItems.getFirst().getCount());
    }

    @Test
    void testUpdateItemCountMinus() throws Exception {
        UUID sessionId = UuidCreator.getTimeOrderedEpoch();
        long itemId = 1L;

        // Add item first
        mockMvc.perform(post("/items")
                        .param("id", Long.toString(itemId))
                        .param("action", "PLUS")
                        .cookie(new jakarta.servlet.http.Cookie("SESSION_ID", sessionId.toString())))
                .andExpect(status().is3xxRedirection());

        // Decrease count
        mockMvc.perform(post("/items")
                        .param("id", Long.toString(itemId))
                        .param("action", "MINUS")
                        .cookie(new jakarta.servlet.http.Cookie("SESSION_ID", sessionId.toString())))
                .andExpect(status().is3xxRedirection());

        var cartItems = cartRepository.findBySessionId(sessionId);
        assertTrue(cartItems.isEmpty());
    }

    @Test
    void testGetItemWithCount() throws Exception {
        UUID sessionId = UuidCreator.getTimeOrderedEpoch();
        long itemId = 1L;

        // Добавляем товар в корзину (2 штуки)
        mockMvc.perform(post("/items")
                        .param("id", Long.toString(itemId))
                        .param("action", "PLUS")
                        .cookie(new jakarta.servlet.http.Cookie("SESSION_ID", sessionId.toString())))
                .andExpect(status().is3xxRedirection());

        mockMvc.perform(post("/items")
                        .param("id", Long.toString(itemId))
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
        UUID sessionId = UuidCreator.getTimeOrderedEpoch();
        long itemId = 1L;

        // Добавляем товар в корзину
        mockMvc.perform(post("/items")
                        .param("id", Long.toString(itemId))
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

    @Test
    void testUpdateItemCountOnPage() throws Exception {
        UUID sessionId = UuidCreator.getTimeOrderedEpoch();
        long itemId = 1L;

        // Увеличиваем количество со страницы товара
        mockMvc.perform(post("/items/" + itemId)
                        .param("action", "PLUS")
                        .cookie(new jakarta.servlet.http.Cookie("SESSION_ID", sessionId.toString())))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/items/" + itemId));

        var cartItems = cartRepository.findBySessionId(sessionId);
        assertEquals(1, cartItems.size());
        assertEquals(1, cartItems.getFirst().getCount());

        // Уменьшаем количество со страницы товара
        mockMvc.perform(post("/items/" + itemId)
                        .param("action", "MINUS")
                        .cookie(new jakarta.servlet.http.Cookie("SESSION_ID", sessionId.toString())))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/items/" + itemId));

        assertTrue(cartRepository.findBySessionId(sessionId).isEmpty());
    }

    @Test
    void testUpdateCartItemInCart() throws Exception {
        UUID sessionId = UuidCreator.getTimeOrderedEpoch();
        long itemId = 1L;

        // Добавляем товар через витрину
        mockMvc.perform(post("/items")
                        .param("id", Long.toString(itemId))
                        .param("action", "PLUS")
                        .cookie(new jakarta.servlet.http.Cookie("SESSION_ID", sessionId.toString())));

        // Увеличиваем количество в корзине
        mockMvc.perform(post("/cart/items")
                        .param("id", Long.toString(itemId))
                        .param("action", "PLUS")
                        .cookie(new jakarta.servlet.http.Cookie("SESSION_ID", sessionId.toString())))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/cart/items"));

        var cartItems = cartRepository.findBySessionId(sessionId);
        assertEquals(2, cartItems.getFirst().getCount());

        // Удаляем товар из корзины (DELETE)
        mockMvc.perform(post("/cart/items")
                        .param("id", Long.toString(itemId))
                        .param("action", "DELETE")
                        .cookie(new jakarta.servlet.http.Cookie("SESSION_ID", sessionId.toString())))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/items")); // Редирект на /items т.к. корзина пуста

        assertTrue(cartRepository.findBySessionId(sessionId).isEmpty());
    }
}
