package ru.yandex.practicum.mymarket.service;

import com.github.f4b6a3.uuid.UuidCreator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.yandex.practicum.mymarket.dto.ItemDto;
import ru.yandex.practicum.mymarket.model.CartAction;
import ru.yandex.practicum.mymarket.model.CartItem;
import ru.yandex.practicum.mymarket.model.Item;
import ru.yandex.practicum.mymarket.repository.CartRepository;
import ru.yandex.practicum.mymarket.repository.ItemRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock
    private CartRepository cartRepository;

    @Mock
    private ItemRepository itemRepository;

    @InjectMocks
    private CartService cartService;

    @Test
    void updateCartItem_Plus_NewItem() {
        UUID sessionId = UuidCreator.getTimeOrderedEpoch();
        Long itemId = 1L;
        Item item = new Item();
        item.setId(itemId);
        item.setPrice(BigDecimal.TEN);

        when(cartRepository.findBySessionIdAndItemId(sessionId, itemId)).thenReturn(Optional.empty());
        when(itemRepository.findById(itemId)).thenReturn(Optional.of(item));

        cartService.updateCartItem(sessionId, itemId, CartAction.PLUS);

        ArgumentCaptor<CartItem> cartItemCaptor = ArgumentCaptor.forClass(CartItem.class);
        verify(cartRepository).save(cartItemCaptor.capture());
        CartItem saved = cartItemCaptor.getValue();
        assertEquals(sessionId, saved.getSessionId());
        assertEquals(item, saved.getItem());
        assertEquals(1, saved.getCount());
    }

    @Test
    void updateCartItem_Plus_ExistingItem() {
        UUID sessionId = UuidCreator.getTimeOrderedEpoch();
        Long itemId = 1L;
        CartItem existingCartItem = new CartItem();
        existingCartItem.setCount(2);

        when(cartRepository.findBySessionIdAndItemId(sessionId, itemId)).thenReturn(Optional.of(existingCartItem));

        cartService.updateCartItem(sessionId, itemId, CartAction.PLUS);

        verify(cartRepository).save(existingCartItem);
        assertEquals(3, existingCartItem.getCount());
    }

    @Test
    void updateCartItem_Minus_MoreThanOne() {
        UUID sessionId = UuidCreator.getTimeOrderedEpoch();
        Long itemId = 1L;
        CartItem existingCartItem = new CartItem();
        existingCartItem.setCount(2);

        when(cartRepository.findBySessionIdAndItemId(sessionId, itemId)).thenReturn(Optional.of(existingCartItem));

        cartService.updateCartItem(sessionId, itemId, CartAction.MINUS);

        verify(cartRepository).save(existingCartItem);
        assertEquals(1, existingCartItem.getCount());
    }

    @Test
    void updateCartItem_Minus_One() {
        UUID sessionId = UuidCreator.getTimeOrderedEpoch();
        Long itemId = 1L;
        CartItem existingCartItem = new CartItem();
        existingCartItem.setCount(1);

        when(cartRepository.findBySessionIdAndItemId(sessionId, itemId)).thenReturn(Optional.of(existingCartItem));

        cartService.updateCartItem(sessionId, itemId, CartAction.MINUS);

        verify(cartRepository).delete(existingCartItem);
        verify(cartRepository, never()).save(any());
    }

    @Test
    void updateCartItem_Delete() {
        UUID sessionId = UuidCreator.getTimeOrderedEpoch();
        Long itemId = 1L;
        CartItem existingCartItem = new CartItem();

        when(cartRepository.findBySessionIdAndItemId(sessionId, itemId)).thenReturn(Optional.of(existingCartItem));

        cartService.updateCartItem(sessionId, itemId, CartAction.DELETE);

        verify(cartRepository).delete(existingCartItem);
    }

    @Test
    void getTotalPrice() {
        ItemDto item1 = ItemDto.builder().price(new BigDecimal("100.00")).count(2).build();
        ItemDto item2 = ItemDto.builder().price(new BigDecimal("50.00")).count(1).build();

        BigDecimal total = cartService.getTotalPrice(List.of(item1, item2));

        assertEquals(new BigDecimal("250.00"), total);
    }
}
