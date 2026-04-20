package ru.yandex.practicum.mymarket.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.mymarket.model.CartItem;
import ru.yandex.practicum.mymarket.model.Item;
import ru.yandex.practicum.mymarket.repository.CartRepository;
import ru.yandex.practicum.mymarket.repository.ItemRepository;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ItemService {
    private final ItemRepository itemRepository;
    private final CartRepository cartRepository;

    @Autowired
    public ItemService(ItemRepository itemRepository, CartRepository cartRepository) {
        this.itemRepository = itemRepository;
        this.cartRepository = cartRepository;
    }

    public Page<Item> getItems(String search, String sessionId, Pageable pageable) {
        Page<Item> items;
        if (search == null || search.isBlank()) {
            items = itemRepository.findAll(pageable);
        } else {
            items = itemRepository.findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(search, search, pageable);
        }

        if (sessionId != null && !sessionId.isBlank()) {
            Map<Long, Integer> cartCounts = cartRepository.findBySessionId(UUID.fromString(sessionId)).stream()
                    .collect(Collectors.toMap(ci -> ci.getItem().getId(), CartItem::getCount));

            items.forEach(item -> item.setCount(cartCounts.getOrDefault(item.getId(), 0)));
        } else {
            items.forEach(item -> item.setCount(0));
        }

        return items;
    }

    public Optional<Item> getItemById(Long id, String sessionId) {
        Optional<Item> item = itemRepository.findById(id);
        if (item.isPresent() && sessionId != null && !sessionId.isBlank()) {
            int count = cartRepository.findBySessionIdAndItemId(UUID.fromString(sessionId), id)
                    .map(CartItem::getCount)
                    .orElse(0);
            item.get().setCount(count);
        } else {
            item.ifPresent(i -> i.setCount(0));
        }
        return item;
    }
}
