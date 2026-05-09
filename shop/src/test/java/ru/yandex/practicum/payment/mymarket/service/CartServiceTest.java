package ru.yandex.practicum.payment.mymarket.service;

import com.github.f4b6a3.uuid.UuidCreator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import ru.yandex.practicum.shop.entity.CartItemEntity;
import ru.yandex.practicum.shop.entity.ItemEntity;
import ru.yandex.practicum.shop.mapper.CartItemMapper;
import ru.yandex.practicum.shop.mapper.ItemMapper;
import ru.yandex.practicum.shop.model.CartAction;
import ru.yandex.practicum.shop.model.CartItem;
import ru.yandex.practicum.shop.model.Item;
import ru.yandex.practicum.shop.repository.CartRepository;
import ru.yandex.practicum.shop.repository.ItemRepository;
import ru.yandex.practicum.shop.service.CartService;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock
    private CartRepository cartRepository;

    @Mock
    private ItemRepository itemRepository;

    @Mock
    private ItemMapper itemMapper;

    @Mock
    private CartItemMapper cartItemMapper;

    private CartService cartService;

    @BeforeEach
    void setUp() {
        cartService = new CartService(cartRepository, itemRepository, itemMapper, cartItemMapper);
    }

    @Test
    void updateCartItem_Plus_NewItem() {
        UUID sessionId = UuidCreator.getTimeOrderedEpoch();
        Long itemId = 1L;
        ItemEntity itemEntity = ItemEntity.builder().id(itemId).price(BigDecimal.TEN).build();
        CartItem model = CartItem.builder().sessionId(sessionId).itemId(itemId).count(1).build();
        CartItemEntity entity = CartItemEntity.builder().sessionId(sessionId).itemId(itemId).count(1).build();

        when(cartRepository.findBySessionIdAndItemId(sessionId, itemId)).thenReturn(Mono.empty());
        when(itemRepository.findById(itemId)).thenReturn(Mono.just(itemEntity));
        when(cartItemMapper.toEntity(any(CartItem.class))).thenReturn(entity);
        when(cartRepository.save(any(CartItemEntity.class))).thenReturn(Mono.just(entity));
        when(cartItemMapper.toModel(any(CartItemEntity.class))).thenReturn(model);

        cartService.updateCartItem(sessionId, itemId, CartAction.PLUS)
                .as(StepVerifier::create)
                .expectNextMatches(saved -> saved.getSessionId().equals(sessionId) &&
                        saved.getItemId().equals(itemId) &&
                        saved.getCount() == 1)
                .verifyComplete();
    }

    @Test
    void updateCartItem_Plus_ExistingItem() {
        UUID sessionId = UUID.randomUUID();
        Long itemId = 1L;
        CartItem model = CartItem.builder().sessionId(sessionId).itemId(itemId).count(2).build();
        CartItemEntity entity = CartItemEntity.builder().sessionId(sessionId).itemId(itemId).count(2).build();
        CartItem updatedModel = CartItem.builder().sessionId(sessionId).itemId(itemId).count(3).build();
        CartItemEntity updatedEntity = CartItemEntity.builder().sessionId(sessionId).itemId(itemId).count(3).build();

        when(cartRepository.findBySessionIdAndItemId(any(UUID.class), anyLong())).thenReturn(Mono.just(entity));
        when(cartItemMapper.toModel(entity)).thenReturn(model);
        when(cartItemMapper.toEntity(any(CartItem.class))).thenReturn(updatedEntity);
        when(cartRepository.save(any(CartItemEntity.class))).thenReturn(Mono.just(updatedEntity));
        when(cartItemMapper.toModel(updatedEntity)).thenReturn(updatedModel);
        when(itemRepository.findById(anyLong())).thenReturn(Mono.empty());

        cartService.updateCartItem(sessionId, itemId, CartAction.PLUS)
                .as(StepVerifier::create)
                .expectNextMatches(saved -> saved.getCount() == 3)
                .verifyComplete();
    }

    @Test
    void updateCartItem_Minus_MoreThanOne() {
        UUID sessionId = UuidCreator.getTimeOrderedEpoch();
        Long itemId = 1L;
        CartItem model = CartItem.builder().sessionId(sessionId).itemId(itemId).count(2).build();
        CartItemEntity entity = CartItemEntity.builder().sessionId(sessionId).itemId(itemId).count(2).build();
        CartItem updatedModel = CartItem.builder().sessionId(sessionId).itemId(itemId).count(1).build();
        CartItemEntity updatedEntity = CartItemEntity.builder().sessionId(sessionId).itemId(itemId).count(1).build();

        when(cartRepository.findBySessionIdAndItemId(sessionId, itemId)).thenReturn(Mono.just(entity));
        when(cartItemMapper.toModel(entity)).thenReturn(model);
        when(cartItemMapper.toEntity(any(CartItem.class))).thenReturn(updatedEntity);
        when(cartRepository.save(any(CartItemEntity.class))).thenReturn(Mono.just(updatedEntity));
        when(cartItemMapper.toModel(updatedEntity)).thenReturn(updatedModel);

        cartService.updateCartItem(sessionId, itemId, CartAction.MINUS)
                .as(StepVerifier::create)
                .expectNextMatches(saved -> saved.getCount() == 1)
                .verifyComplete();
    }

    @Test
    void updateCartItem_Minus_One() {
        UUID sessionId = UUID.randomUUID();
        Long itemId = 1L;
        CartItem model = CartItem.builder().sessionId(sessionId).itemId(itemId).count(1).build();
        CartItemEntity entity = CartItemEntity.builder().sessionId(sessionId).itemId(itemId).count(1).build();

        when(cartRepository.findBySessionIdAndItemId(any(UUID.class), any(Long.class))).thenReturn(Mono.just(entity));
        when(cartItemMapper.toModel(entity)).thenReturn(model);
        when(cartItemMapper.toEntity(model)).thenReturn(entity);
        when(cartRepository.delete(any(CartItemEntity.class))).thenReturn(Mono.empty());

        cartService.updateCartItem(sessionId, itemId, CartAction.MINUS)
                .as(StepVerifier::create)
                .verifyComplete();

        verify(cartRepository).delete(any(CartItemEntity.class));
    }

    @Test
    void updateCartItem_Delete() {
        UUID sessionId = UUID.randomUUID();
        Long itemId = 1L;
        CartItem model = CartItem.builder().sessionId(sessionId).itemId(itemId).count(1).build();
        CartItemEntity entity = CartItemEntity.builder().sessionId(sessionId).itemId(itemId).count(1).build();

        when(cartRepository.findBySessionIdAndItemId(any(UUID.class), any(Long.class))).thenReturn(Mono.just(entity));
        when(cartItemMapper.toModel(entity)).thenReturn(model);
        when(cartItemMapper.toEntity(model)).thenReturn(entity);
        when(cartRepository.delete(any(CartItemEntity.class))).thenReturn(Mono.empty());

        cartService.updateCartItem(sessionId, itemId, CartAction.DELETE)
                .as(StepVerifier::create)
                .verifyComplete();

        verify(cartRepository).delete(any(CartItemEntity.class));
    }

    @Test
    void getTotalPrice() {
        Item item1 = Item.builder().price(new BigDecimal("100.00")).count(2).build();
        Item item2 = Item.builder().price(new BigDecimal("50.00")).count(1).build();

        cartService.getTotalPrice(List.of(item1, item2))
                .as(StepVerifier::create)
                .expectNext(new BigDecimal("250.00"))
                .verifyComplete();
    }

    @Test
    void getTotalPrice_NoNPE_AfterFix() {
        Item itemWithNullCount = Item.builder()
                .price(new BigDecimal("100.00"))
                .count(null)
                .build();

        cartService.getTotalPrice(List.of(itemWithNullCount))
                .as(StepVerifier::create)
                .expectNextMatches(total -> total.compareTo(BigDecimal.ZERO) == 0)
                .verifyComplete();
    }
}
