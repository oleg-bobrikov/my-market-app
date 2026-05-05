package ru.yandex.practicum.mymarket.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.mymarket.model.Item;
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

    public Flux<Item> getItems(String search, UUID sessionId, Pageable pageable) {
        Flux<ItemEntity> items;

        if (search != null && !search.isBlank()) {
            String pattern = "%" + search + "%";
            items = itemRepository.searchByTitleOrDescription(pattern, pageable);
        } else {
            items = itemRepository.findAll(pageable);
        }

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

        return cartCountByItemId.flatMapMany(cartCounts ->
                items.map(item -> {
                    Item model = itemMapper.toModel(item);
                    model.setCount(cartCounts.getOrDefault(item.getId(), 0));
                    return model;
                })
        );
    }

    public Mono<Item> findByItemIdAndSessionId(Long id, UUID sessionId) {
        if (sessionId != null) {
            return itemRepository.findByItemIdAndSessionId(id, sessionId)
                    .map(itemMapper::toModel);
        } else {
            return Mono.empty();
        }
    }
}
