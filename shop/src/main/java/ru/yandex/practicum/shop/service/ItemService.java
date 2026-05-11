package ru.yandex.practicum.shop.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.shop.model.Item;
import ru.yandex.practicum.shop.entity.ItemEntity;
import ru.yandex.practicum.shop.mapper.ItemMapper;
import ru.yandex.practicum.shop.repository.ItemRepository;

import java.time.Duration;
import java.util.UUID;

@Service
public class ItemService {
    private final ItemRepository itemRepository;
    private final CartService cartService;
    private final ItemMapper itemMapper;
    private final ReactiveRedisTemplate<String, Item> itemRedisTemplate;

    private static final String ITEM_CACHE_PREFIX = "item:";
    private static final Duration CACHE_TTL = Duration.ofHours(1);

    @Autowired
    public ItemService(ItemRepository itemRepository,
                       CartService cartService,
                       ItemMapper itemMapper,
                       ReactiveRedisTemplate<String, Item> itemRedisTemplate) {
        this.itemRepository = itemRepository;
        this.cartService = cartService;
        this.itemMapper = itemMapper;
        this.itemRedisTemplate = itemRedisTemplate;
    }

    public Mono<Item> findByItemId(Long id) {
        String cacheKey = ITEM_CACHE_PREFIX + id;
        return itemRedisTemplate.opsForValue().get(cacheKey)
                .switchIfEmpty(
                        itemRepository.findById(id)
                                .map(itemMapper::toModel)
                                .flatMap(item -> itemRedisTemplate.opsForValue().set(cacheKey, item, CACHE_TTL)
                                        .thenReturn(item))
                );
    }

    public Flux<Item> getItems(String search, UUID sessionId, Pageable pageable) {
        String cacheKey = String.format("items:all:%s:%d:%d:%s",
                search != null ? search : "",
                pageable.getPageNumber(),
                pageable.getPageSize(),
                pageable.getSort().toString());

        Flux<Item> itemsFlux = itemRedisTemplate.opsForList().range(cacheKey, 0, -1)
                .switchIfEmpty(
                        (search != null && !search.isBlank()
                                ? itemRepository.searchByTitleOrDescription("%" + search + "%", pageable)
                                : itemRepository.findAll(pageable))
                                .map(itemMapper::toModel)
                                .collectList()
                                .flatMapMany(list -> {
                                    if (list.isEmpty()) {
                                        return Flux.empty();
                                    }
                                    return itemRedisTemplate.opsForList().rightPushAll(cacheKey, list)
                                            .then(itemRedisTemplate.expire(cacheKey, CACHE_TTL))
                                            .thenMany(Flux.fromIterable(list));
                                })
                );

        return cartService.getCartCounts(sessionId).flatMapMany(cartCounts ->
                itemsFlux.map(item -> {
                    item.setCount(cartCounts.getOrDefault(item.getId(), 0));
                    return item;
                })
        );
    }

    public Mono<Item> findByItemIdAndSessionId(Long id, UUID sessionId) {
        return findByItemId(id)
                .zipWith(cartService.getCartCounts(sessionId)
                        .map(counts -> counts.getOrDefault(id, 0)))
                .map(tuple -> {
                    Item item = tuple.getT1();
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

                    return Flux.fromIterable(counts.keySet())
                            .flatMap(itemId -> findByItemId(itemId)
                                    .map(item -> {
                                        item.setCount(counts.getOrDefault(itemId, 0));
                                        return item;
                                    }));
                });
    }
}
