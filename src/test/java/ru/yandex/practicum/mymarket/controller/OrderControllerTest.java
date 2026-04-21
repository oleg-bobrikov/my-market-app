package ru.yandex.practicum.mymarket.controller;

import com.github.f4b6a3.uuid.UuidCreator;
import org.junit.jupiter.api.Test;
import ru.yandex.practicum.mymarket.BaseWebMvcTest;
import ru.yandex.practicum.mymarket.model.Order;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class OrderControllerTest extends BaseWebMvcTest {

    @Test
    public void testBuyRedirectsToOrder() throws Exception {
        UUID sessionId = UuidCreator.getTimeOrderedEpoch();
        when(orderService.createOrder(sessionId)).thenReturn(123L);

        mockMvc.perform(post("/buy")
                        .cookie(new jakarta.servlet.http.Cookie("SESSION_ID", sessionId.toString())))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/orders/123?newOrder=true"));

        verify(orderService).createOrder(sessionId);
    }

    @Test
    public void testGetOrderReturnsOrderView() throws Exception {
        Order order = Order.builder().id(1L).total(100L).build();
        when(orderService.getOrderById(1L)).thenReturn(Optional.of(order));
        when(orderService.getOrderItemsDto(order)).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/orders/1"))
                .andExpect(status().isOk())
                .andExpect(view().name("order"))
                .andExpect(model().attributeExists("order"))
                .andExpect(model().attribute("newOrder", false));
    }

    @Test
    public void testGetAllOrdersReturnsOrdersView() throws Exception {
        Order order = Order.builder().id(1L).total(100L).build();
        when(orderService.getAllOrders()).thenReturn(List.of(order));
        when(orderService.getOrderItemsDto(order)).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/orders"))
                .andExpect(status().isOk())
                .andExpect(view().name("orders"))
                .andExpect(model().attributeExists("orders"));
    }
}
