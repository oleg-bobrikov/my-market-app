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

import java.util.List;
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
        return cartService.getCartCounts(sessionId).flatMapMany(cartCounts ->
                self.getBaseItems(search, pageable).map(item -> {
                    Item newItem = item.toBuilder().build();
                    Integer count = cartCounts.get(item.getId());
                    newItem.setCount(count != null ? count : 0);
                    return newItem;
                })
        );
    }

    public Mono<Item> findByItemIdAndSessionId(Long id, UUID sessionId) {
        return self.findByItemId(id)
                .zipWith(cartService.getCartCounts(sessionId)
                        .map(counts -> {
                            Integer count = counts.get(id);
                            return count != null ? count : 0;
                        }))
                .map(tuple -> {
                    Item item = tuple.getT1().toBuilder().build();
                    item.setCount(tuple.getT2());
                    return item;
                });
    }

    public Flux<Item> getCartItems(UUID sessionId) {
        return cartService.getCartCounts(sessionId)
                .flatMapMany(counts -> {
                    if (counts.isEmpty()) {
                        return Flux.empty();
                    }

                    List<Long> keys = new java.util.ArrayList<>(counts.keySet());
                    return Flux.fromIterable(keys)
                            .flatMap(itemId -> self.findByItemId(itemId)
                                    .map(item -> {
                                        Item newItem = item.toBuilder().build();
                                        Integer count = counts.get(itemId);
                                        newItem.setCount(count != null ? count : 0);
                                        return newItem;
                                    }));
                });
    }
}
