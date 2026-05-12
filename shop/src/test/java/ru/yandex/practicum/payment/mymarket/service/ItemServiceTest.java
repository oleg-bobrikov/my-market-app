package ru.yandex.practicum.payment.mymarket.service;

import com.github.f4b6a3.uuid.UuidCreator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.ReactiveListOperations;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.ReactiveValueOperations;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import ru.yandex.practicum.shop.entity.ItemEntity;
import ru.yandex.practicum.shop.mapper.ItemMapper;
import ru.yandex.practicum.shop.model.Item;
import ru.yandex.practicum.shop.repository.ItemRepository;
import ru.yandex.practicum.shop.service.CartService;
import ru.yandex.practicum.shop.service.ItemService;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static reactor.core.publisher.Mono.just;

@ExtendWith(MockitoExtension.class)
class ItemServiceTest {

    @Mock
    private ItemRepository itemRepository;

    @Mock
    private CartService cartService;

    @Mock
    private ItemMapper itemMapper;

    @Mock
    private ReactiveRedisTemplate<String, Item> itemRedisTemplate;

    @Mock
    private ReactiveListOperations<String, Item> reactiveListOperations;

    @Mock
    private ReactiveValueOperations<String, Item> reactiveValueOperations;

    private ItemService itemService;

    @BeforeEach
    void setUp() {
        lenient().when(itemRedisTemplate.opsForList()).thenReturn(reactiveListOperations);
        lenient().when(itemRedisTemplate.opsForValue()).thenReturn(reactiveValueOperations);
        lenient().when(reactiveListOperations.range(anyString(), anyLong(), anyLong())).thenReturn(Flux.empty());
        lenient().when(reactiveValueOperations.get(anyString())).thenReturn(Mono.empty());
        lenient().when(reactiveListOperations.rightPushAll(anyString(), anyCollection())).thenAnswer(invocation -> {
            Collection<?> collection = invocation.getArgument(1);
            if (collection == null || collection.isEmpty()) {
                throw new IllegalArgumentException("Values must not be null or empty");
            }
            return Mono.just(1L);
        });
        lenient().when(reactiveValueOperations.set(anyString(), any(Item.class), any())).thenReturn(Mono.just(true));
        lenient().when(itemRedisTemplate.expire(anyString(), any())).thenReturn(Mono.just(true));

        itemService = new ItemService(itemRepository, cartService, itemMapper, itemRedisTemplate);
    }

    @Test
    void getItems_WhenSessionExists_ReturnsItemsWithCartCounts() {
        String search = "";
        UUID sessionId = UuidCreator.getTimeOrderedEpoch();
        Pageable pageable = PageRequest.of(0, 10);
        
        ItemEntity itemEntity = ItemEntity.builder().id(1L).build();
        
        Item itemModel = new Item();
        itemModel.setId(1L);

        when(itemRepository.findAll(pageable)).thenReturn(Flux.just(itemEntity));
        when(cartService.getCartCounts(sessionId)).thenReturn(just(Map.of(1L, 5)));
        when(itemMapper.toModel(itemEntity)).thenReturn(itemModel);

        itemService.getItems(search, sessionId, pageable)
                .as(StepVerifier::create)
                .expectNextMatches(result -> result.getId().equals(1L) && result.getCount() == 5)
                .verifyComplete();
    }

    @Test
    void getItems_WhenSearchAndSortByPrice_ReturnsMatchingItems() {
        String search = "phone";
        UUID sessionId = UUID.randomUUID();
        Pageable pageable = PageRequest.of(0, 10, Sort.by("price"));

        ItemEntity itemEntity = ItemEntity.builder().id(1L).price(BigDecimal.valueOf(100)).build();
        Item itemModel = new Item();
        itemModel.setId(1L);

        when(itemRepository.searchByTitleOrDescription(eq("%phone%"), eq(pageable)))
                .thenReturn(Flux.just(itemEntity));
        when(cartService.getCartCounts(sessionId)).thenReturn(just(Map.of()));
        when(itemMapper.toModel(itemEntity)).thenReturn(itemModel);

        itemService.getItems(search, sessionId, pageable)
                .as(StepVerifier::create)
                .expectNextCount(1)
                .verifyComplete();

        verify(itemRepository).searchByTitleOrDescription(eq("%phone%"), eq(pageable));
    }

    @Test
    void getItems_WhenSearchAndSortByTitle_ReturnsMatchingItems() {
        String search = "phone";
        UUID sessionId = UUID.randomUUID();
        Pageable pageable = PageRequest.of(0, 10, Sort.by("title"));

        ItemEntity itemEntity = ItemEntity.builder().id(1L).title("iPhone").build();
        Item itemModel = new Item();
        itemModel.setId(1L);

        when(itemRepository.searchByTitleOrDescription(eq("%phone%"), eq(pageable)))
                .thenReturn(Flux.just(itemEntity));
        when(cartService.getCartCounts(sessionId)).thenReturn(just(Map.of()));
        when(itemMapper.toModel(itemEntity)).thenReturn(itemModel);

        itemService.getItems(search, sessionId, pageable)
                .as(StepVerifier::create)
                .expectNextCount(1)
                .verifyComplete();

        verify(itemRepository).searchByTitleOrDescription(eq("%phone%"), eq(pageable));
    }

    @Test
    void getItems_WhenNoResults_ReturnsEmptyFlux() {
        String search = "nonexistent";
        UUID sessionId = UUID.randomUUID();
        Pageable pageable = PageRequest.of(0, 10);

        when(itemRepository.searchByTitleOrDescription(anyString(), any(Pageable.class)))
                .thenReturn(Flux.empty());
        when(cartService.getCartCounts(sessionId)).thenReturn(just(Map.of()));

        itemService.getItems(search, sessionId, pageable)
                .as(StepVerifier::create)
                .expectNextCount(0)
                .verifyComplete();
    }
}
