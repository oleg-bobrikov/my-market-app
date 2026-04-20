package ru.yandex.practicum.mymarket;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import ru.yandex.practicum.mymarket.model.CartAction;
import ru.yandex.practicum.mymarket.repository.CartRepository;
import ru.yandex.practicum.mymarket.repository.OrderRepository;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class OrderEndpointTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Test
    void testBuyAndRedirect() throws Exception {
        UUID sessionId = UUID.randomUUID();
        Long itemId = 1L;

        // 1. Добавляем товар в корзину
        mockMvc.perform(post("/items")
                        .param("id", itemId.toString())
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
        var savedOrder = orders.get(orders.size() - 1);
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
        UUID sessionId = UUID.randomUUID();
        mockMvc.perform(post("/items")
                        .param("id", "1")
                        .param("action", "PLUS")
                        .cookie(new jakarta.servlet.http.Cookie("SESSION_ID", sessionId.toString())));
        
        String location = mockMvc.perform(post("/buy")
                        .cookie(new jakarta.servlet.http.Cookie("SESSION_ID", sessionId.toString())))
                .andReturn().getResponse().getHeader("Location");
        
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
}
