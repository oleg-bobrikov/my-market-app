package ru.yandex.practicum.shop.service;

import org.springframework.context.annotation.Lazy;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.shop.model.Item;
import ru.yandex.practicum.shop.mapper.ItemMapper;
import ru.yandex.practicum.shop.repository.ItemRepository;

import java.util.Map;
import java.util.UUID;

@Service
public class ItemService {
    private final ItemRepository itemRepository;
    private final CartService cartService;
    private final ItemMapper itemMapper;
    private final ItemService self;

    public ItemService(ItemRepository itemRepository,
                       CartService cartService,
                       ItemMapper itemMapper,
                       @Lazy ItemService self) {
        this.itemRepository = itemRepository;
        this.cartService = cartService;
        this.itemMapper = itemMapper;
        this.self = self;
    }

    @Cacheable(value = "items", key = "#id")
    public Mono<Item> findByItemId(Long id) {
        return itemRepository.findById(id)
                .map(itemMapper::toModel)
                .cache();
    }

    @Cacheable(value = "item-lists", key = "#search + ':' + #pageable.pageNumber + ':' + #pageable.pageSize + ':' + #pageable.sort")
    public Flux<Item> getBaseItems(String search, Pageable pageable) {
        return (search != null && !search.isBlank()
                ? itemRepository.searchByTitleOrDescription("%" + search + "%", pageable)
                : itemRepository.findAll(pageable))
                .map(itemMapper::toModel);
    }

    public Flux<Item> getItems(String search, UUID sessionId, Pageable pageable) {
        return cartService.getCartCounts(sessionId)
                .map(this::normalizeCartCounts)
                .flatMapMany(cartCounts ->
                        self.getBaseItems(search, pageable).map(item -> {
                            Item newItem = item.toBuilder().build();
                            int count = cartCounts.getOrDefault(item.getId(), 0);
                            newItem.setCount(count);
                            return newItem;
                        })
                );
    }

    public Mono<Item> findByItemIdAndSessionId(Long id, UUID sessionId) {
        return self.findByItemId(id)
                .zipWith(cartService.getCartCounts(sessionId)
                        .map(this::normalizeCartCounts)
                        .map(counts -> counts.getOrDefault(id, 0)))
                .map(tuple -> {
                    Item item = tuple.getT1().toBuilder().build();
                    item.setCount(tuple.getT2());
                    return item;
                });
    }

    public Flux<Item> getCartItems(UUID sessionId) {
        return cartService.getCartCounts(sessionId)
                .map(this::normalizeCartCounts)
                .flatMapMany(counts -> {
                    if (counts.isEmpty()) {
                        return Flux.empty();
                    }

                    return Flux.fromIterable(counts.entrySet())
                            .flatMap(entry -> self.findByItemId(entry.getKey())
                                    .map(item -> {
                                        Item newItem = item.toBuilder().build();
                                        newItem.setCount(entry.getValue());
                                        return newItem;
                                    }));
                });
    }

    private Map<Long, Integer> normalizeCartCounts(Map<?, ?> rawMap) {
        if (rawMap == null || rawMap.isEmpty()) {
            return java.util.Map.of();
        }

        return rawMap.entrySet().stream()
                .collect(java.util.stream.Collectors.toMap(
                        entry -> {
                            Object key = entry.getKey();
                            if (key instanceof Long) return (Long) key;
                            if (key instanceof Integer) return ((Integer) key).longValue();
                            return Long.parseLong(key.toString());
                        },
                        entry -> {
                            Object value = entry.getValue();
                            if (value instanceof Integer) return (Integer) value;
                            return Integer.parseInt(value.toString());
                        }
                ));
    }
}
