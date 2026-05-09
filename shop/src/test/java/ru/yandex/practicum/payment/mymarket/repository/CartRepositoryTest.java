package ru.yandex.practicum.payment.mymarket.repository;

import com.github.f4b6a3.uuid.UuidCreator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.test.StepVerifier;
import ru.yandex.practicum.shop.entity.CartItemEntity;
import ru.yandex.practicum.shop.entity.ItemEntity;
import ru.yandex.practicum.shop.repository.CartRepository;
import ru.yandex.practicum.shop.repository.ItemRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

class CartRepositoryTest extends BaseDataR2dbcTest {

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private ItemRepository itemRepository;

    @Test
    void findBySessionIdAndItemId_shouldFindCorrectItem() {
        ItemEntity item = ItemEntity.builder()
                .title("Item 1")
                .description("Desc")
                .imgPath("path")
                .price(BigDecimal.TEN)
                .build();
        UUID sessionId = UuidCreator.getTimeOrderedEpoch();

        itemRepository.save(item)
                .flatMap(savedItem -> {
                    CartItemEntity cartItem = CartItemEntity.builder()
                            .sessionId(sessionId)
                            .itemId(savedItem.getId())
                            .count(2)
                            .build();
                    return cartRepository.save(cartItem).thenReturn(savedItem);
                })
                .flatMap(savedItem -> cartRepository.findBySessionIdAndItemId(sessionId, savedItem.getId()))
                .as(StepVerifier::create)
                .expectNextMatches(found -> found.getCount() == 2)
                .verifyComplete();
    }

    @Test
    void findBySessionId_shouldReturnAllItemsForSession() {
        ItemEntity item1 = ItemEntity.builder().title("Item 1").description("Desc").imgPath("path").price(BigDecimal.TEN).build();
        ItemEntity item2 = ItemEntity.builder().title("Item 2").description("Desc").imgPath("path").price(BigDecimal.ONE).build();
        UUID sessionId = UuidCreator.getTimeOrderedEpoch();

        itemRepository.saveAll(List.of(item1, item2))
                .collectList()
                .flatMapMany(savedItems -> {
                    CartItemEntity ci1 = CartItemEntity.builder().sessionId(sessionId).itemId(savedItems.get(0).getId()).count(1).build();
                    CartItemEntity ci2 = CartItemEntity.builder().sessionId(sessionId).itemId(savedItems.get(1).getId()).count(3).build();
                    CartItemEntity ci3 = CartItemEntity.builder().sessionId(UuidCreator.getTimeOrderedEpoch()).itemId(savedItems.get(0).getId()).count(5).build();
                    return cartRepository.saveAll(List.of(ci1, ci2, ci3));
                })
                .thenMany(cartRepository.findBySessionId(sessionId))
                .as(StepVerifier::create)
                .expectNextCount(2)
                .verifyComplete();
    }
}
