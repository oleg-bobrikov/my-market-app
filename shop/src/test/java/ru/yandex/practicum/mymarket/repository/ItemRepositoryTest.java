package ru.yandex.practicum.mymarket.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import reactor.test.StepVerifier;
import ru.yandex.practicum.mymarket.entity.ItemEntity;

import java.math.BigDecimal;

class ItemRepositoryTest extends BaseDataR2dbcTest {

    @Autowired
    private ItemRepository itemRepository;

    @Test
    void searchByTitleOrDescription_shouldFindItemByTitle() {
        ItemEntity item = ItemEntity.builder()
                .title("Apple iPhone 15")
                .description("Latest smartphone")
                .imgPath("path")
                .price(new BigDecimal("999.99"))
                .build();
        
        itemRepository.save(item)
                .as(StepVerifier::create)
                .expectNextCount(1)
                .verifyComplete();

        itemRepository.searchByTitleOrDescription("%iPhone%", PageRequest.of(0, 10))
                .as(StepVerifier::create)
                .expectNextMatches(result -> result.getTitle().equals("Apple iPhone 15"))
                .verifyComplete();
    }

    @Test
    void searchByTitleOrDescription_shouldFindItemByDescription() {
        ItemEntity item = ItemEntity.builder()
                .title("Laptop")
                .description("Powerful device with M2 chip")
                .imgPath("path")
                .price(new BigDecimal("1299.99"))
                .build();
        
        itemRepository.save(item)
                .as(StepVerifier::create)
                .expectNextCount(1)
                .verifyComplete();

        itemRepository.searchByTitleOrDescription("%M2%", PageRequest.of(0, 10))
                .as(StepVerifier::create)
                .expectNextMatches(result -> result.getDescription().contains("M2 chip"))
                .verifyComplete();
    }

    @Test
    void searchByTitleOrDescription_shouldBeCaseInsensitive() {
        ItemEntity item = ItemEntity.builder()
                .title("Samsung Galaxy")
                .description("Android phone")
                .imgPath("path")
                .price(new BigDecimal("799.99"))
                .build();
        
        itemRepository.save(item)
                .as(StepVerifier::create)
                .expectNextCount(1)
                .verifyComplete();

        itemRepository.searchByTitleOrDescription("%SAMSUNG%", PageRequest.of(0, 10))
                .as(StepVerifier::create)
                .expectNextMatches(result -> result.getTitle().equalsIgnoreCase("Samsung Galaxy"))
                .verifyComplete();
    }
}
