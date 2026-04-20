package ru.yandex.practicum.mymarket.service;

import com.github.f4b6a3.uuid.UuidCreator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.yandex.practicum.mymarket.model.CartItem;
import ru.yandex.practicum.mymarket.model.Item;
import ru.yandex.practicum.mymarket.model.Order;
import ru.yandex.practicum.mymarket.repository.CartRepository;
import ru.yandex.practicum.mymarket.repository.OrderRepository;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private CartRepository cartRepository;

    @InjectMocks
    private OrderService orderService;

    @Test
    void createOrder_Success() {
        UUID sessionId = UuidCreator.getTimeOrderedEpoch();
        Item item = new Item();
        item.setId(1L);
        item.setPrice(new BigDecimal("100.00"));
        
        CartItem cartItem = CartItem.builder()
                .item(item)
                .count(2)
                .build();
        
        when(cartRepository.findBySessionId(sessionId)).thenReturn(List.of(cartItem));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order o = invocation.getArgument(0);
            o.setId(10L);
            return o;
        });

        Long orderId = orderService.createOrder(sessionId);

        assertEquals(10L, orderId);
        verify(orderRepository).save(any(Order.class));
        verify(cartRepository).deleteAll(anyList());
    }

    @Test
    void createOrder_ThrowsException_WhenCartIsEmpty() {
        UUID sessionId = UuidCreator.getTimeOrderedEpoch();
        when(cartRepository.findBySessionId(sessionId)).thenReturn(Collections.emptyList());

        assertThrows(IllegalStateException.class, () -> orderService.createOrder(sessionId));
        verify(orderRepository, never()).save(any());
    }
}
