package ru.yandex.practicum.mymarket.integration;

import com.github.f4b6a3.uuid.UuidCreator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import ru.yandex.practicum.mymarket.BaseIntegrationTest;
import ru.yandex.practicum.mymarket.model.CartAction;
import ru.yandex.practicum.mymarket.repository.CartRepository;
import ru.yandex.practicum.mymarket.repository.OrderRepository;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class OrderIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Test
    void testBuyAndRedirect() throws Exception {
        UUID sessionId = UuidCreator.getTimeOrderedEpoch();
        long itemId = 1L;

        // 1. Добавляем товар в корзину
        mockMvc.perform(post("/items")
                        .param("id", Long.toString(itemId))
                        .param("action", CartAction.PLUS.name())
                        .cookie(new jakarta.servlet.http.Cookie("SESSION_ID", sessionId.toString())))
                .andExpect(status().is3xxRedirection());

        // Проверяем, что товар в корзине
        assertEquals(1, cartRepository.findBySessionId(sessionId).size());

        // 2. Совершаем покупку
        mockMvc.perform(post("/buy")
                        .cookie(new jakarta.servlet.http.Cookie("SESSION_ID", sessionId.toString())))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", org.hamcrest.Matchers.containsString("/orders/")))
                .andExpect(header().string("Location", org.hamcrest.Matchers.containsString("newOrder=true")));

        // 3. Проверяем, что корзина пуста
        assertTrue(cartRepository.findBySessionId(sessionId).isEmpty());

        // 4. Проверяем, что заказ создался
        var orders = orderRepository.findAll();
        assertFalse(orders.isEmpty());
        var savedOrder = orders.getLast();
        Long orderId = savedOrder.getId();
        assertEquals(sessionId, savedOrder.getSessionId());

        // 5. Проверяем эндпоинт получения заказа
        mockMvc.perform(get("/orders/" + orderId)
                        .param("newOrder", "true"))
                .andExpect(status().isOk())
                .andExpect(view().name("order"))
                .andExpect(model().attributeExists("order"))
                .andExpect(model().attribute("newOrder", true))
                .andExpect(model().attribute("order", org.hamcrest.Matchers.hasEntry("id", orderId)));
    }

    @Test
    void testGetOrderWithoutNewOrderParam() throws Exception {
        // Создаем заказ вручную или через /buy
        UUID sessionId = UuidCreator.getTimeOrderedEpoch();
        mockMvc.perform(post("/items")
                        .param("id", "1")
                        .param("action", "PLUS")
                        .cookie(new jakarta.servlet.http.Cookie("SESSION_ID", sessionId.toString())));
        
        String location = mockMvc.perform(post("/buy")
                        .cookie(new jakarta.servlet.http.Cookie("SESSION_ID", sessionId.toString())))
                .andReturn().getResponse().getHeader("Location");

        assertNotNull(location);
        String orderId = location.substring(location.lastIndexOf("/") + 1, location.indexOf("?"));

        // Проверяем GET /orders/{id} без параметра newOrder
        mockMvc.perform(get("/orders/" + orderId))
                .andExpect(status().isOk())
                .andExpect(model().attribute("newOrder", false));
    }

    @Test
    void testGetAllOrders() throws Exception {
        mockMvc.perform(get("/orders"))
                .andExpect(status().isOk())
                .andExpect(view().name("orders"))
                .andExpect(model().attributeExists("orders"));
    }

    @Test
    void testBuyWithoutSessionRedirects() throws Exception {
        mockMvc.perform(post("/buy"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/items"));
    }
}
