package ru.yandex.practicum.shop.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.shop.model.Item;
import ru.yandex.practicum.shop.entity.ItemEntity;
import ru.yandex.practicum.shop.mapper.ItemMapper;
import ru.yandex.practicum.shop.repository.ItemRepository;

import java.util.UUID;

@Service
public class ItemService {
    private final ItemRepository itemRepository;
    private final CartService cartService;
    private final ItemMapper itemMapper;

    @Autowired
    public ItemService(ItemRepository itemRepository, CartService cartService, ItemMapper itemMapper) {
        this.itemRepository = itemRepository;
        this.cartService = cartService;
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

        return cartService.getCartCounts(sessionId).flatMapMany(cartCounts ->
                items.map(item -> {
                    Item model = itemMapper.toModel(item);
                    model.setCount(cartCounts.getOrDefault(item.getId(), 0));
                    return model;
                })
        );
    }

    public Mono<Item> findByItemIdAndSessionId(Long id, UUID sessionId) {
        Mono<Integer> countMono = cartService.getCartCounts(sessionId)
                .map(counts -> counts.getOrDefault(id, 0));

        return itemRepository.findById(id)
                .zipWith(countMono)
                .map(tuple -> {
                    Item model = itemMapper.toModel(tuple.getT1());
                    model.setCount(tuple.getT2());
                    return model;
                });
    }
}
