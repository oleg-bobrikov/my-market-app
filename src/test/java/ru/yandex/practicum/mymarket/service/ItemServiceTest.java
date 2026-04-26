package ru.yandex.practicum.mymarket.service;

import com.github.f4b6a3.uuid.UuidCreator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;
import ru.yandex.practicum.mymarket.dto.ItemDto;
import ru.yandex.practicum.mymarket.entity.CartItemEntity;
import ru.yandex.practicum.mymarket.entity.ItemEntity;
import ru.yandex.practicum.mymarket.mapper.ItemMapper;
import ru.yandex.practicum.mymarket.repository.CartRepository;
import ru.yandex.practicum.mymarket.repository.ItemRepository;

import java.util.UUID;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ItemServiceTest {

    @Mock
    private ItemRepository itemRepository;

    @Mock
    private CartRepository cartRepository;

    @Mock
    private ItemMapper itemMapper;

    @InjectMocks
    private ItemService itemService;

    @Test
    void getItems_WithCartCounts() {
        String search = "";
        UUID sessionId = UuidCreator.getTimeOrderedEpoch();
        Pageable pageable = PageRequest.of(0, 10);
        
        ItemEntity itemEntity = ItemEntity.builder().id(1L).build();
        
        ItemDto itemDto = new ItemDto();
        itemDto.setId(1L);
        
        CartItemEntity cartItemEntity = CartItemEntity.builder()
                .itemId(1L)
                .count(5)
                .build();

        when(itemRepository.findAll(10, 0)).thenReturn(Flux.just(itemEntity));
        when(cartRepository.findBySessionId(sessionId)).thenReturn(Flux.just(cartItemEntity));
        when(itemMapper.toDto(itemEntity)).thenReturn(itemDto);

        itemService.getItems(search, sessionId, pageable)
                .as(StepVerifier::create)
                .expectNextMatches(result -> result.getId().equals(1L) && result.getCount() == 5)
                .verifyComplete();
    }
}
