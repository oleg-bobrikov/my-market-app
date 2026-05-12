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
        return cartService.getCartCounts(sessionId).flatMapMany(cartCounts ->
                self.getBaseItems(search, pageable).map(item -> {
                    Item newItem = item.toBuilder().build();
                    int count;
                    Object countObj = cartCounts.get(item.getId());
                    if (countObj == null) {
                        countObj = ((Map<?, ?>) cartCounts).get(item.getId().toString());
                    }
                    if (countObj instanceof Integer i) {
                        count = i;
                    } else if (countObj != null) {
                        count = Integer.parseInt(countObj.toString());
                    } else {
                        count = 0;
                    }
                    newItem.setCount(count);
                    return newItem;
                })
        );
    }

    public Mono<Item> findByItemIdAndSessionId(Long id, UUID sessionId) {
        return self.findByItemId(id)
                .zipWith(cartService.getCartCounts(sessionId)
                        .map(counts -> {
                            int count;
                            Object countObj = counts.get(id);
                            if (countObj == null) {
                                countObj = ((Map<?, ?>) counts).get(id.toString());
                            }
                            if (countObj instanceof Integer i) {
                                count = i;
                            } else if (countObj != null) {
                                count = Integer.parseInt(countObj.toString());
                            } else {
                                count = 0;
                            }
                            return count;
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

                    return Flux.fromIterable(new java.util.ArrayList<>(counts.entrySet()))
                            .flatMap(entry -> {
                                long itemId;
                                Object key = entry.getKey();
                                if (key instanceof Long l) {
                                    itemId = l;
                                } else {
                                    return Mono.empty();
                                }

                                return self.findByItemId(itemId)
                                        .map(item -> {
                                            Item newItem = item.toBuilder().build();
                                            int count;
                                            Object value = entry.getValue();
                                            if (value instanceof Integer i) {
                                                count = i;
                                            } else {
                                                count = 0;
                                            }
                                            newItem.setCount(count);
                                            return newItem;
                                        });
                            });
                });
    }
}
