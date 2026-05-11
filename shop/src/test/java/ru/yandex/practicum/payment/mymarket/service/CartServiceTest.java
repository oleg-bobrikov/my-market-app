package ru.yandex.practicum.payment.mymarket.service;

import com.github.f4b6a3.uuid.UuidCreator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.connection.stream.ObjectRecord;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.core.ReactiveHashOperations;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.ReactiveStreamOperations;
import org.springframework.data.redis.core.script.RedisScript;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import ru.yandex.practicum.shop.entity.ItemEntity;
import ru.yandex.practicum.shop.mapper.ItemMapper;
import ru.yandex.practicum.shop.model.CartAction;
import ru.yandex.practicum.shop.model.Item;
import ru.yandex.practicum.shop.repository.ItemRepository;
import ru.yandex.practicum.shop.service.CartService;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {
    @Mock
    private ItemMapper itemMapper;

    @Mock
    private ReactiveRedisTemplate<String, String> redisTemplate;

    @Mock
    private ReactiveHashOperations<String, String, String> hashOperations;

    @Mock
    private ReactiveStreamOperations<String, String, String> streamOperations;

    @Mock
    private ru.yandex.practicum.shop.service.ItemService itemService;

    private CartService cartService;

    @SuppressWarnings("unchecked")
    @BeforeEach
    void setUp() {
        lenient().doReturn(hashOperations).when(redisTemplate).opsForHash();
        lenient().doReturn(streamOperations).when(redisTemplate).opsForStream();
        lenient().when(redisTemplate.execute(any(RedisScript.class), anyList(), anyList())).thenReturn(Flux.just(List.of(1L, 1L)));
        lenient().when(redisTemplate.expire(anyString(), any(Duration.class))).thenReturn(Mono.just(true));
        lenient().when(streamOperations.add(any(ObjectRecord.class))).thenReturn(Mono.just(RecordId.of("0-1")));
        lenient().when(hashOperations.put(anyString(), anyString(), anyString())).thenReturn(Mono.just(true));
        lenient().when(hashOperations.entries(anyString())).thenReturn(Flux.empty());
        lenient().when(hashOperations.increment(anyString(), anyString(), anyLong())).thenReturn(Mono.just(1L));
        lenient().when(hashOperations.remove(anyString(), any())).thenReturn(Mono.just(1L));
        lenient().when(redisTemplate.delete(anyString())).thenReturn(Mono.just(1L));
        lenient().when(redisTemplate.delete(any(String[].class))).thenReturn(Mono.just(1L));
        lenient().when(hashOperations.putAll(anyString(), anyMap())).thenReturn(Mono.empty());
        cartService = new CartService(itemMapper, redisTemplate, itemService);
    }

    @Test
    void updateCartItem_WhenPlusForNewItem_CreatesItem() {
        UUID sessionId = UuidCreator.getTimeOrderedEpoch();
        Long itemId = 1L;

        cartService.updateCartItem(sessionId, itemId, CartAction.PLUS)
                .as(StepVerifier::create)
                .verifyComplete();
    }

    @Test
    void updateCartItem_WhenPlusForExistingItem_IncrementsCount() {
        UUID sessionId = UUID.randomUUID();
        Long itemId = 1L;

        cartService.updateCartItem(sessionId, itemId, CartAction.PLUS)
                .as(StepVerifier::create)
                .verifyComplete();
    }

    @Test
    void updateCartItem_WhenMinusAndCountMoreThanOne_DecrementsCount() {
        UUID sessionId = UuidCreator.getTimeOrderedEpoch();
        Long itemId = 1L;

        cartService.updateCartItem(sessionId, itemId, CartAction.MINUS)
                .as(StepVerifier::create)
                .verifyComplete();
    }

    @Test
    void updateCartItem_WhenMinusAndCountEqualsOne_DeletesItem() {
        UUID sessionId = UUID.randomUUID();
        Long itemId = 1L;

        cartService.updateCartItem(sessionId, itemId, CartAction.MINUS)
                .as(StepVerifier::create)
                .verifyComplete();
    }

    @Test
    void updateCartItem_WhenActionDelete_DeletesItem() {
        UUID sessionId = UUID.randomUUID();
        Long itemId = 1L;

        cartService.updateCartItem(sessionId, itemId, CartAction.DELETE)
                .as(StepVerifier::create)
                .verifyComplete();
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

    @Test
    void getCartItems_WhenItemsInRedis_ReturnsCartItems() {
        UUID sessionId = UUID.randomUUID();
        Long itemId = 1L;
        Item itemModel = Item.builder().id(itemId).title("Test Item").build();

        lenient().when(hashOperations.entries(anyString())).thenReturn(Flux.just(new java.util.AbstractMap.SimpleEntry<>(itemId.toString(), "1")));
        when(itemService.findByItemId(itemId)).thenReturn(Mono.just(itemModel));

        cartService.getCartItems(sessionId)
                .as(StepVerifier::create)
                .expectNextMatches(item -> item.getId().equals(itemId) && item.getCount() == 1)
                .verifyComplete();

        verify(itemService).findByItemId(itemId);
    }

    @Test
    void getCartItems_WhenRedisEmpty_LoadsItemsFromDatabase() {
        UUID sessionId = UUID.randomUUID();

        lenient().when(hashOperations.entries(anyString())).thenReturn(Flux.empty());

        cartService.getCartItems(sessionId)
                .as(StepVerifier::create)
                .expectNextCount(0)
                .verifyComplete();
    }
}
