package ru.yandex.practicum.payment.mymarket.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import reactor.test.StepVerifier;
import ru.yandex.practicum.shop.entity.ItemEntity;
import ru.yandex.practicum.shop.model.Item;
import ru.yandex.practicum.shop.repository.ItemRepository;
import ru.yandex.practicum.shop.service.ItemService;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class RedisCachingIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private ItemService itemService;

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private ReactiveRedisTemplate<String, Item> itemRedisTemplate;

    @Test
    void findByItemId_WhenCalled_CachesResult() {
        // 1. Создаем товар в БД
        ItemEntity entity = ItemEntity.builder()
                .title("Cached Item")
                .description("Cache Test")
                .price(new BigDecimal("100.00"))
                .imgPath("img.jpg")
                .count(10)
                .build();
        ItemEntity savedEntity = itemRepository.save(entity).block();
        assertNotNull(savedEntity);
        Long id = savedEntity.getId();
        String cacheKey = "item:" + id;

        // Убеждаемся, что в кэше пусто
        itemRedisTemplate.delete(cacheKey).block();

        // 2. Первый вызов - должен загрузить из БД и положить в кэш
        Item firstCall = itemService.findByItemId(id).block();
        assertNotNull(firstCall);
        assertEquals("Cached Item", firstCall.getTitle());

        // Проверяем наличие в кэше
        Item cachedItem = itemRedisTemplate.opsForValue().get(cacheKey).block();
        assertNotNull(cachedItem, "Item should be in Redis cache after first call");
        assertEquals("Cached Item", cachedItem.getTitle());

        // 3. Изменяем товар в БД напрямую (в обход сервиса)
        savedEntity.setTitle("Updated in DB");
        itemRepository.save(savedEntity).block();

        // 4. Второй вызов - должен вернуть данные ИЗ КЭША (старое название)
        Item secondCall = itemService.findByItemId(id).block();
        assertNotNull(secondCall);
        assertEquals("Cached Item", secondCall.getTitle(), "Should return cached value, not from DB");

        // 5. Очищаем кэш и проверяем, что теперь подтянется из БД
        itemRedisTemplate.delete(cacheKey).block();
        Item thirdCall = itemService.findByItemId(id).block();
        assertNotNull(thirdCall);
        assertEquals("Updated in DB", thirdCall.getTitle());
    }

    @Test
    void getItems_WhenCalled_CachesList() {
        UUID sessionId = UUID.randomUUID();
        String search = "unique_search";
        
        // Очищаем потенциальные старые ключи
        itemRedisTemplate.keys("items:all:*").flatMap(k -> itemRedisTemplate.delete(k)).collectList().block();

        // 1. Создаем товар
        ItemEntity entity = ItemEntity.builder()
                .title("List Item")
                .description("unique_search")
                .price(new BigDecimal("200.00"))
                .imgPath("img2.jpg")
                .count(5)
                .build();
        itemRepository.save(entity).block();

        // 2. Запрашиваем список
        var pageable = org.springframework.data.domain.PageRequest.of(0, 10);
        itemService.getItems(search, sessionId, pageable).collectList().block();

        // 3. Проверяем, что в Redis появился ключ для этого поиска
        // Ключ формируется как items:all:search:page:size:sort
        String expectedCacheKey = String.format("items:all:%s:%d:%d:%s",
                search, pageable.getPageNumber(), pageable.getPageSize(), pageable.getSort().toString());
        
        Boolean hasKey = itemRedisTemplate.hasKey(expectedCacheKey).block();
        assertTrue(Boolean.TRUE.equals(hasKey), "Cache key for list should exist: " + expectedCacheKey);
        
        // Проверяем TTL (должен быть около 1 часа согласно ItemService.CACHE_TTL)
        Duration expire = itemRedisTemplate.getExpire(expectedCacheKey).block();
        assertNotNull(expire);
        assertTrue(!expire.isNegative() && expire.getSeconds() <= 3600, "TTL should be set for cached list");
    }
}
