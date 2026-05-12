package ru.yandex.practicum.payment.mymarket.service;

import com.github.f4b6a3.uuid.UuidCreator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.cache.CacheManager;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import ru.yandex.practicum.shop.model.CartAction;
import ru.yandex.practicum.shop.model.Item;
import ru.yandex.practicum.shop.repository.CartRepository;
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
    private CacheManager cacheManager;

    private CartService cartService;

    @BeforeEach
    void setUp() {
        cartService = new CartService(cartRepository);
    }

    @Test
    void updateCartItem_WhenPlusForNewItem_CreatesItem() {
        UUID sessionId = UuidCreator.getTimeOrderedEpoch();
        Long itemId = 1L;

        when(cartRepository.findBySessionIdAndItemId(sessionId, itemId)).thenReturn(Mono.empty());
        when(cartRepository.save(any())).thenReturn(Mono.just(new ru.yandex.practicum.shop.entity.CartItemEntity()));

        cartService.updateCartItem(sessionId, itemId, CartAction.PLUS)
                .as(StepVerifier::create)
                .verifyComplete();

        verify(cartRepository).save(any());
    }

    @Test
    void updateCartItem_WhenPlusForExistingItem_IncrementsCount() {
        UUID sessionId = UUID.randomUUID();
        Long itemId = 1L;

        ru.yandex.practicum.shop.entity.CartItemEntity entity = ru.yandex.practicum.shop.entity.CartItemEntity.builder()
                .sessionId(sessionId).itemId(itemId).count(1).build();

        when(cartRepository.findBySessionIdAndItemId(sessionId, itemId)).thenReturn(Mono.just(entity));
        when(cartRepository.save(any())).thenReturn(Mono.just(entity));

        cartService.updateCartItem(sessionId, itemId, CartAction.PLUS)
                .as(StepVerifier::create)
                .verifyComplete();

        verify(cartRepository).save(argThat(e -> e.getCount() == 2));
    }

    @Test
    void updateCartItem_WhenMinusAndCountMoreThanOne_DecrementsCount() {
        UUID sessionId = UuidCreator.getTimeOrderedEpoch();
        Long itemId = 1L;

        ru.yandex.practicum.shop.entity.CartItemEntity entity = ru.yandex.practicum.shop.entity.CartItemEntity.builder()
                .sessionId(sessionId).itemId(itemId).count(2).build();

        when(cartRepository.findBySessionIdAndItemId(sessionId, itemId)).thenReturn(Mono.just(entity));
        when(cartRepository.save(any())).thenReturn(Mono.just(entity));

        cartService.updateCartItem(sessionId, itemId, CartAction.MINUS)
                .as(StepVerifier::create)
                .verifyComplete();

        verify(cartRepository).save(argThat(e -> e.getCount() == 1));
    }

    @Test
    void updateCartItem_WhenMinusAndCountEqualsOne_DeletesItem() {
        UUID sessionId = UUID.randomUUID();
        Long itemId = 1L;

        ru.yandex.practicum.shop.entity.CartItemEntity entity = ru.yandex.practicum.shop.entity.CartItemEntity.builder()
                .sessionId(sessionId).itemId(itemId).count(1).build();

        when(cartRepository.findBySessionIdAndItemId(sessionId, itemId)).thenReturn(Mono.just(entity));
        when(cartRepository.delete(any())).thenReturn(Mono.empty());

        cartService.updateCartItem(sessionId, itemId, CartAction.MINUS)
                .as(StepVerifier::create)
                .verifyComplete();

        verify(cartRepository).delete(any());
    }

    @Test
    void updateCartItem_WhenActionDelete_DeletesItem() {
        UUID sessionId = UUID.randomUUID();
        Long itemId = 1L;

        ru.yandex.practicum.shop.entity.CartItemEntity entity = ru.yandex.practicum.shop.entity.CartItemEntity.builder()
                .sessionId(sessionId).itemId(itemId).count(5).build();

        when(cartRepository.findBySessionIdAndItemId(sessionId, itemId)).thenReturn(Mono.just(entity));
        when(cartRepository.delete(any())).thenReturn(Mono.empty());

        cartService.updateCartItem(sessionId, itemId, CartAction.DELETE)
                .as(StepVerifier::create)
                .verifyComplete();

        verify(cartRepository).delete(any());
    }

    @Test
    void getTotalPrice_WhenListOfItemsProvided_CalculatesTotalPrice() {
        Item item1 = Item.builder().price(new BigDecimal("100.00")).count(2).build();
        Item item2 = Item.builder().price(new BigDecimal("50.00")).count(1).build();

        cartService.getTotalPrice(List.of(item1, item2))
                .as(StepVerifier::create)
                .expectNext(new BigDecimal("250.00"))
                .verifyComplete();
    }
}
