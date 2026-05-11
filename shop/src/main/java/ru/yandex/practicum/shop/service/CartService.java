package ru.yandex.practicum.shop.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.stream.ObjectRecord;
import org.springframework.data.redis.core.ReactiveHashOperations;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.ReactiveStreamOperations;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.shop.dto.CartEventDto;
import ru.yandex.practicum.shop.mapper.ItemMapper;
import ru.yandex.practicum.shop.model.CartAction;
import ru.yandex.practicum.shop.model.Item;
import ru.yandex.practicum.shop.repository.ItemRepository;


import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class CartService {
    private final ItemMapper itemMapper;
    private final ReactiveRedisTemplate<String, String> redisTemplate;
    private final ReactiveHashOperations<String, String, String> hashOps;
    private final ReactiveStreamOperations<String, String, String> streamOps;
    private final ItemService itemService;

    private static final String CART_PREFIX = "cart:";
    private static final String CART_STREAM = "cart-events";

    @Autowired
    public CartService(ItemMapper itemMapper,
                       ReactiveRedisTemplate<String, String> redisTemplate,
                       ItemService itemService) {
        this.itemMapper = itemMapper;
        this.redisTemplate = redisTemplate;
        this.hashOps = redisTemplate.opsForHash();
        this.streamOps = redisTemplate.opsForStream();
        this.itemService = itemService;
    }

    private static final String CART_ITEMS_SUFFIX = ":items";
    private static final String CART_VERSION_SUFFIX = ":version";

    private String itemsKey(UUID sessionId) {
        return CART_PREFIX + sessionId + CART_ITEMS_SUFFIX;
    }

    private String versionKey(UUID sessionId) {
        return CART_PREFIX + sessionId + CART_VERSION_SUFFIX;
    }

    public Mono<Void> updateCartItem(UUID sessionId, Long itemId, CartAction action) {
        String itemsKey = itemsKey(sessionId);
        String versionKey = versionKey(sessionId);
        String itemIdStr = itemId.toString();

        String script = """
                   local items_key = KEYS[1]
                   local version_key = KEYS[2]
                
                   local item_id = ARGV[1]
                   local action = ARGV[2]
                
                   local count = tonumber(redis.call('HGET', items_key, item_id)) or 0
                   local version = tonumber(redis.call('GET', version_key)) or 0
                
                   local new_version = version + 1
                   local new_count = count
                
                   local should_delete = false
                
                   if action == 'PLUS' then
                       new_count = count + 1
                   elseif action == 'MINUS' then
                       new_count = count - 1
                   elseif action == 'DELETE' then
                       should_delete = true
                   end
                
                   if should_delete or new_count <= 0 then
                       redis.call('HDEL', items_key, item_id)
                       redis.call('SET', version_key, new_version)
                       return {0, new_version}
                   end
                
                   redis.call('HSET', items_key, item_id, new_count)
                   redis.call('SET', version_key, new_version)
                
                   return {new_count, new_version}
                """;

        return redisTemplate.execute(
                        org.springframework.data.redis.core.script.RedisScript.of(script, List.class),
                        List.of(itemsKey, versionKey),
                        List.of(itemIdStr, action.name())
                )
                .next()
                .flatMap(result -> {
                    if (result == null || result.size() < 2) {
                        return Mono.error(new RuntimeException("Invalid Lua script result"));
                    }
                    Long newVersion = ((Number) result.get(1)).longValue();
                    CartEventDto event = new CartEventDto(sessionId, itemId, action, newVersion, Instant.now());
                    String vKey = versionKey(sessionId);
                    return redisTemplate.expire(itemsKey, Duration.ofDays(7))
                            .then(redisTemplate.expire(vKey, Duration.ofDays(7)))
                            .onErrorResume(e -> Mono.just(true))
                            .then(streamOps.add(ObjectRecord.create(CART_STREAM, event)));
                })
                .then();
    }

    public Flux<Item> getCartItems(UUID sessionId) {
        return getCartCounts(sessionId)
                .flatMapMany(counts -> {
                    if (counts.isEmpty()) {
                        return Flux.empty();
                    }

                    return Flux.fromIterable(counts.keySet())
                            .flatMap(itemId -> itemService.findByItemId(itemId)
                                    .map(item -> {
                                        item.setCount(counts.getOrDefault(itemId, 0));
                                        return item;
                                    }));
                });
    }

    public Mono<java.util.Map<Long, Integer>> getCartCounts(UUID sessionId) {
        if (sessionId == null) {
            return Mono.just(java.util.Map.of());
        }
        String key = itemsKey(sessionId);

        return hashOps.entries(key)
                .collectMap(
                        entry -> Long.parseLong(entry.getKey()),
                        entry -> Integer.parseInt(entry.getValue())
                );
    }

    public Mono<BigDecimal> getTotalPrice(List<Item> items) {
        return Mono.fromSupplier(() ->
                items.stream()
                        .map(item -> {
                            BigDecimal price = item.getPrice() != null ? item.getPrice() : BigDecimal.ZERO;
                            int count = item.getCount() != null ? item.getCount() : 0;
                            return price.multiply(BigDecimal.valueOf(count));
                        })
                        .reduce(BigDecimal.ZERO, BigDecimal::add)
        );
    }

    public Mono<Void> clearCart(UUID sessionId) {
        String itemsKey = itemsKey(sessionId);
        String vKey = versionKey(sessionId);
        return redisTemplate.delete(itemsKey, vKey)
                .then(redisTemplate.keys("items:all:*").flatMap(redisTemplate::delete).collectList())
                .then();
    }
}
