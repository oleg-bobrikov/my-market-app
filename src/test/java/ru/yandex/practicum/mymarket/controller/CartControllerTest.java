package ru.yandex.practicum.mymarket.controller;

import com.github.f4b6a3.uuid.UuidCreator;
import org.junit.jupiter.api.Test;
import ru.yandex.practicum.mymarket.BaseWebMvcTest;
import ru.yandex.practicum.mymarket.dto.ItemDto;
import ru.yandex.practicum.mymarket.model.CartAction;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class CartControllerTest extends BaseWebMvcTest {

    @Test
    public void testGetCartItemsReturnsCartView() throws Exception {
        UUID sessionId =  UuidCreator.getTimeOrderedEpoch();
        List<ItemDto> items = List.of(
                ItemDto.builder().id(1L).title("Item 1").price(BigDecimal.TEN).count(1).build()
        );
        when(cartService.getCartItems(sessionId)).thenReturn(items);
        when(cartService.getTotalPrice(items)).thenReturn(BigDecimal.TEN);

        mockMvc.perform(get("/cart/items")
                        .cookie(new jakarta.servlet.http.Cookie("SESSION_ID", sessionId.toString())))
                .andExpect(status().isOk())
                .andExpect(view().name("cart"))
                .andExpect(model().attribute("items", items))
                .andExpect(model().attribute("total", BigDecimal.TEN));
    }

    @Test
    public void testGetCartItemsRedirectsToItemsWhenNoSession() throws Exception {
        mockMvc.perform(get("/cart/items"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/items"));
    }

    @Test
    public void testUpdateCartItemRedirectsToCart() throws Exception {
        UUID sessionId = UuidCreator.getTimeOrderedEpoch();
        when(cartService.getCartItems(sessionId)).thenReturn(List.of(new ItemDto()));

        mockMvc.perform(post("/cart/items")
                        .cookie(new jakarta.servlet.http.Cookie("SESSION_ID", sessionId.toString()))
                        .param("id", "1")
                        .param("action", "PLUS"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/cart/items"));

        verify(cartService).updateCartItem(eq(sessionId), eq(1L), eq(CartAction.PLUS));
    }

    @Test
    void testRedirectToItemsWhenCartIsEmptyOnGet() throws Exception {
        UUID sessionId = UuidCreator.getTimeOrderedEpoch();
        when(cartService.getCartItems(sessionId)).thenReturn(Collections.emptyList());

        // Доступ к корзине без товаров должен перенаправлять на /items
        mockMvc.perform(get("/cart/items")
                        .cookie(new jakarta.servlet.http.Cookie("SESSION_ID", sessionId.toString())))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/items"));
    }

    @Test
    void testRedirectToItemsWhenCartBecomesEmptyOnUpdate() throws Exception {
        UUID sessionId = UuidCreator.getTimeOrderedEpoch();
        long itemId = 1L;

        when(cartService.getCartItems(sessionId)).thenReturn(Collections.emptyList());

        // Удаляем товар (action=DELETE)
        mockMvc.perform(post("/cart/items")
                        .param("id", Long.toString(itemId))
                        .param("action", "DELETE")
                        .cookie(new jakarta.servlet.http.Cookie("SESSION_ID", sessionId.toString())))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/items"));
    }

    @Test
    void testRedirectToItemsWhenCartBecomesEmptyOnMinus() throws Exception {
        UUID sessionId = UuidCreator.getTimeOrderedEpoch();
        long itemId = 1L;

        when(cartService.getCartItems(sessionId)).thenReturn(Collections.emptyList());

        // Уменьшаем количество до 0 (action=MINUS для товара в количестве 1 удаляет его)
        mockMvc.perform(post("/cart/items")
                        .param("id", Long.toString(itemId))
                        .param("action", "MINUS")
                        .cookie(new jakarta.servlet.http.Cookie("SESSION_ID", sessionId.toString())))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/items"));
    }
}
