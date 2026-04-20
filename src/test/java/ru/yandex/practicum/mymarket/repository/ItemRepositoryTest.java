package ru.yandex.practicum.mymarket.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import ru.yandex.practicum.mymarket.BaseDataJpaTest;
import ru.yandex.practicum.mymarket.model.Item;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class ItemRepositoryTest extends BaseDataJpaTest {

    @Autowired
    private ItemRepository itemRepository;

    @Test
    void findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase_shouldFindItemByTitle() {
        Item item = new Item(null, "Apple iPhone 15", "Latest smartphone", "path", new BigDecimal("999.99"));
        itemRepository.save(item);

        Page<Item> result = itemRepository.findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(
                "iPhone", "iPhone", PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().getFirst().getTitle()).isEqualTo("Apple iPhone 15");
    }

    @Test
    void findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase_shouldFindItemByDescription() {
        Item item = new Item(null, "Laptop", "Powerful device with M2 chip", "path", new BigDecimal("1299.99"));
        itemRepository.save(item);

        Page<Item> result = itemRepository.findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(
                "M2", "M2", PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().getFirst().getDescription()).contains("M2 chip");
    }

    @Test
    void findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase_shouldBeCaseInsensitive() {
        Item item = new Item(null, "Samsung Galaxy", "Android phone", "path", new BigDecimal("799.99"));
        itemRepository.save(item);

        Page<Item> result = itemRepository.findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(
                "SAMSUNG", "SAMSUNG", PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().getFirst().getTitle()).isEqualTo("Samsung Galaxy");
    }
}
