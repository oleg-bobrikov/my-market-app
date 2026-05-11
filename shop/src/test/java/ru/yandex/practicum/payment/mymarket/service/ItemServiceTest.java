package ru.yandex.practicum.payment.mymarket.service;

import com.github.f4b6a3.uuid.UuidCreator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;
import ru.yandex.practicum.shop.entity.CartItemEntity;
import ru.yandex.practicum.shop.entity.ItemEntity;
import ru.yandex.practicum.shop.mapper.ItemMapper;
import ru.yandex.practicum.shop.model.Item;
import ru.yandex.practicum.shop.repository.ItemRepository;
import ru.yandex.practicum.shop.service.CartService;
import ru.yandex.practicum.shop.service.ItemService;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static reactor.core.publisher.Mono.just;

@ExtendWith(MockitoExtension.class)
class ItemServiceTest {

    @Mock
    private ItemRepository itemRepository;

    @Mock
    private CartService cartService;

    @Mock
    private ItemMapper itemMapper;

    @InjectMocks
    private ItemService itemService;

    @Test
    void getItems_WhenSessionExists_ReturnsItemsWithCartCounts() {
        String search = "";
        UUID sessionId = UuidCreator.getTimeOrderedEpoch();
        Pageable pageable = PageRequest.of(0, 10);
        
        ItemEntity itemEntity = ItemEntity.builder().id(1L).build();
        
        Item itemModel = new Item();
        itemModel.setId(1L);
        
        CartItemEntity cartItemEntity = CartItemEntity.builder()
                .itemId(1L)
                .count(5)
                .build();

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
}
