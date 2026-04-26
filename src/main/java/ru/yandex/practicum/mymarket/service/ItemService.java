package ru.yandex.practicum.mymarket.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.mymarket.dto.ItemDto;
import ru.yandex.practicum.mymarket.entity.CartItemEntity;
import ru.yandex.practicum.mymarket.entity.ItemEntity;
import ru.yandex.practicum.mymarket.mapper.ItemMapper;
import ru.yandex.practicum.mymarket.repository.CartRepository;
import ru.yandex.practicum.mymarket.repository.ItemRepository;

import java.util.Map;
import java.util.UUID;

@Service
public class ItemService {
    private final ItemRepository itemRepository;
    private final CartRepository cartRepository;
    private final ItemMapper itemMapper;

    @Autowired
    public ItemService(ItemRepository itemRepository, CartRepository cartRepository, ItemMapper itemMapper) {
        this.itemRepository = itemRepository;
        this.cartRepository = cartRepository;
        this.itemMapper = itemMapper;
    }

    public Flux<ItemDto> getItems(String search, UUID sessionId, Pageable pageable) {
        int limit = pageable.getPageSize();
        long offset = pageable.getOffset();
        Sort sort = pageable.getSort();

        // 1. Получаем items (Flux)
        Flux<ItemEntity> items;
        if (search == null || search.isBlank()) {
            if (sort.isUnsorted()) {
                items = itemRepository.findAll(limit, offset);
            } else if (sort.getOrderFor("title") != null) {
                items = itemRepository.findAllOrderedByTitle(limit, offset);
            } else if (sort.getOrderFor("price") != null) {
                items = itemRepository.findAllOrderedByPrice(limit, offset);
            } else {
                items = itemRepository.findAll(limit, offset);
            }
        } else {
            String pattern = "%" + search + "%";
            items = itemRepository.searchByTitleOrDescription(pattern, limit, offset);
        }

        // 2. Получаем cartCounts реактивно
        Mono<Map<Long, Integer>> cartCountByItemId;

        if (sessionId != null ) {
            cartCountByItemId = cartRepository.findBySessionId(sessionId)
                    .collectMap(
                            CartItemEntity::getItemId,
                            CartItemEntity::getCount
                    );
        } else {
            cartCountByItemId = Mono.just(Map.of());
        }

        // 3. Объединяем items + cartCounts
        return cartCountByItemId.flatMapMany(cartCounts ->
                items.map(item -> {
                    ItemDto dto = itemMapper.toDto(item);
                    dto.setCount(cartCounts.getOrDefault(item.getId(), 0));
                    return dto;
                })
        );
    }

    public Mono<ItemDto> findByItemIdAndSessionId(Long id, UUID sessionId) {
        if (sessionId != null) {
            return itemRepository.findByItemIdAndSessionId(id, sessionId)
                    .map(itemMapper::toDto);
        } else {
            return Mono.empty();
        }
    }
}
