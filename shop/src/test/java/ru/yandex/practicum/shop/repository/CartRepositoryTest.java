package ru.yandex.practicum.shop.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.test.StepVerifier;
import ru.yandex.practicum.shop.entity.CartItemEntity;
import ru.yandex.practicum.shop.entity.ItemEntity;
import ru.yandex.practicum.shop.entity.UserEntity;

import java.math.BigDecimal;
import java.util.List;

class CartRepositoryTest extends BaseDataR2dbcTest {

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void findByUserIdAndItemId_WhenItemExists_ReturnsCorrectItem() {
        ItemEntity item = ItemEntity.builder()
                .title("Item 1")
                .description("Desc")
                .imgPath("path")
                .price(BigDecimal.TEN)
                .build();
        UserEntity user = UserEntity.builder()
                .login("test")
                .password("pass")
                .authorities("USER")
                .build();

        cartRepository.deleteAll()
                .then(itemRepository.deleteAll())
                .then(userRepository.deleteAll())
                .then(userRepository.save(user))
                .flatMap(savedUser -> itemRepository.save(item)
                        .flatMap(savedItem -> {
                            CartItemEntity cartItem = CartItemEntity.builder()
                                    .userId(savedUser.getId())
                                    .itemId(savedItem.getId())
                                    .count(2)
                                    .build();
                            return cartRepository.save(cartItem);
                        })
                        .flatMap(savedCartItem -> cartRepository.findByUserIdAndItemId(savedUser.getId(), savedCartItem.getItemId())))
                .as(StepVerifier::create)
                .expectNextCount(1)
                .verifyComplete();
    }

    @Test
    void findBySessionId_WhenItemsExist_ReturnsAllItemsForSession() {
        ItemEntity item1 = ItemEntity.builder().title("Item 1").description("Desc").imgPath("path").price(BigDecimal.TEN).build();
        ItemEntity item2 = ItemEntity.builder().title("Item 2").description("Desc").imgPath("path").price(BigDecimal.ONE).build();
        UserEntity user = UserEntity.builder()
                .login("test2")
                .password("pass")
                .authorities("USER")
                .build();

        cartRepository.deleteAll()
                .then(itemRepository.deleteAll())
                .then(userRepository.deleteAll())
                .then(userRepository.save(user))
                .flatMapMany(savedUser -> itemRepository.saveAll(List.of(item1, item2))
                        .collectList()
                        .flatMapMany(savedItems -> {
                            CartItemEntity ci1 = CartItemEntity.builder().userId(savedUser.getId()).itemId(savedItems.get(0).getId()).count(1).build();
                            CartItemEntity ci2 = CartItemEntity.builder().userId(savedUser.getId()).itemId(savedItems.get(1).getId()).count(3).build();
                            return cartRepository.saveAll(List.of(ci1, ci2));
                        })
                        .thenMany(cartRepository.findByUserId(savedUser.getId())))
                .as(StepVerifier::create)
                .expectNextCount(2)
                .verifyComplete();
    }
}
