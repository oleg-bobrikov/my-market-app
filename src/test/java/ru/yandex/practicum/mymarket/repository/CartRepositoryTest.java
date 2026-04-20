package ru.yandex.practicum.mymarket.repository;

import com.github.f4b6a3.uuid.UuidCreator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ru.yandex.practicum.mymarket.BaseDataJpaTest;
import ru.yandex.practicum.mymarket.model.CartItem;
import ru.yandex.practicum.mymarket.model.Item;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CartRepositoryTest extends BaseDataJpaTest {

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private ItemRepository itemRepository;

    @Test
    void findBySessionIdAndItemId_shouldFindCorrectItem() {
        Item item = itemRepository.save(new Item(null, "Item 1", "Desc", "path", BigDecimal.TEN));
        UUID sessionId = UuidCreator.getTimeOrderedEpoch();
        CartItem cartItem = CartItem.builder()
                .sessionId(sessionId)
                .item(item)
                .count(2)
                .build();
        cartRepository.save(cartItem);

        Optional<CartItem> found = cartRepository.findBySessionIdAndItemId(sessionId, item.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getCount()).isEqualTo(2);
        assertThat(found.get().getItem().getTitle()).isEqualTo("Item 1");
    }

    @Test
    void findBySessionId_shouldReturnAllItemsForSession() {
        Item item1 = itemRepository.save(new Item(null, "Item 1", "Desc", "path", BigDecimal.TEN));
        Item item2 = itemRepository.save(new Item(null, "Item 2", "Desc", "path", BigDecimal.ONE));
        UUID sessionId = UuidCreator.getTimeOrderedEpoch();

        cartRepository.save(CartItem.builder().sessionId(sessionId).item(item1).count(1).build());
        cartRepository.save(CartItem.builder().sessionId(sessionId).item(item2).count(3).build());
        cartRepository.save(CartItem.builder().sessionId(UuidCreator.getTimeOrderedEpoch()).item(item1).count(5).build());

        List<CartItem> results = cartRepository.findBySessionId(sessionId);

        assertThat(results).hasSize(2);
        assertThat(results).extracting(ci -> ci.getItem().getTitle()).containsExactlyInAnyOrder("Item 1", "Item 2");
    }
}
