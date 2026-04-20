package ru.yandex.practicum.mymarket.service;

import com.github.f4b6a3.uuid.UuidCreator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import ru.yandex.practicum.mymarket.dto.ItemDto;
import ru.yandex.practicum.mymarket.mapper.ItemMapper;
import ru.yandex.practicum.mymarket.model.CartItem;
import ru.yandex.practicum.mymarket.model.Item;
import ru.yandex.practicum.mymarket.repository.CartRepository;
import ru.yandex.practicum.mymarket.repository.ItemRepository;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
        
        Item item = new Item();
        item.setId(1L);
        
        ItemDto itemDto = new ItemDto();
        itemDto.setId(1L);
        
        CartItem cartItem = CartItem.builder()
                .item(item)
                .count(5)
                .build();

        when(itemRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(item)));
        when(cartRepository.findBySessionId(sessionId)).thenReturn(List.of(cartItem));
        when(itemMapper.toDto(item)).thenReturn(itemDto);

        Page<ItemDto> result = itemService.getItems(search, sessionId.toString(), pageable);

        assertEquals(1, result.getContent().size());
        assertEquals(5, result.getContent().getFirst().getCount());
    }
}
