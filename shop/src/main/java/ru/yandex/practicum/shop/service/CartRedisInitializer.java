package ru.yandex.practicum.shop.service;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.shop.repository.CartRepository;
import org.springframework.data.redis.core.ReactiveRedisTemplate;

import java.time.Duration;
import java.util.List;
import java.util.UUID;


@Service
@RequiredArgsConstructor
public class CartRedisInitializer {

    private final CartRepository cartRepository;
    private final ReactiveRedisTemplate<String, String> redisTemplate;

    private static final String CART_PREFIX = "cart:";

    private static final String CART_ITEMS_SUFFIX = ":items";
    private static final String CART_VERSION_SUFFIX = ":version";

    private String itemsKey(UUID sessionId) {
        return CART_PREFIX + sessionId + CART_ITEMS_SUFFIX;
    }

    private String versionKey(UUID sessionId) {
        return CART_PREFIX + sessionId + CART_VERSION_SUFFIX;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void initRedisCache() {
        String script = """
                   local items_key = KEYS[1]
                   local version_key = KEYS[2]
                
                   local item_id = ARGV[1]
                   local count = ARGV[2]
                   local new_version = ARGV[3]
                   local ttl = ARGV[4]
                
                   local current_v = redis.call('GET', version_key)
                
                   if not current_v or tonumber(current_v) < tonumber(new_version) then
                       redis.call('HSET', items_key, item_id, count)
                       redis.call('SET', version_key, new_version)
                
                       redis.call('EXPIRE', items_key, ttl)
                       redis.call('EXPIRE', version_key, ttl)
                   end
                """;

        cartRepository.findAll()
                .flatMap(entity -> {
                    String itemsKey = itemsKey(entity.getSessionId());
                    String versionKey = versionKey(entity.getSessionId());
                    String itemIdStr = entity.getItemId().toString();
                    long version = entity.getVersion() != null ? entity.getVersion() : 0L;

                    return redisTemplate.execute(
                                    RedisScript.of(script),
                                    List.of(itemsKey, versionKey),
                                    List.of(itemIdStr, entity.getCount().toString(), String.valueOf(version))
                            )
                            .then(Mono.when(
                                    redisTemplate.expire(itemsKey, Duration.ofDays(7)),
                                    redisTemplate.expire(versionKey, Duration.ofDays(7))
                            ));
                })
                .subscribe();
    }
}