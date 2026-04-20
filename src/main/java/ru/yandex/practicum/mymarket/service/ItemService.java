package ru.yandex.practicum.mymarket.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.mymarket.dto.ItemDto;
import ru.yandex.practicum.mymarket.mapper.ItemMapper;
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
    private final ItemMapper itemMapper;

    @Autowired
    public ItemService(ItemRepository itemRepository, CartRepository cartRepository, ItemMapper itemMapper) {
        this.itemRepository = itemRepository;
        this.cartRepository = cartRepository;
        this.itemMapper = itemMapper;
    }

    public Page<ItemDto> getItems(String search, String sessionId, Pageable pageable) {
        Page<Item> items;
        if (search == null || search.isBlank()) {
            items = itemRepository.findAll(pageable);
        } else {
            items = itemRepository.findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(search, search, pageable);
        }

        Map<Long, Integer> cartCounts;
        if (sessionId != null && !sessionId.isBlank()) {
            cartCounts = cartRepository.findBySessionId(UUID.fromString(sessionId)).stream()
                    .collect(Collectors.toMap(ci -> ci.getItem().getId(), CartItem::getCount));
        } else {
            cartCounts = Map.of();
        }

        return items.map(item -> {
            ItemDto itemDto = itemMapper.toDto(item);
            itemDto.setCount(cartCounts.getOrDefault(item.getId(), 0));
            return itemDto;
        });
    }

    public Optional<ItemDto> getItemById(Long id, String sessionId) {
        return itemRepository.findById(id).map(item -> {
            ItemDto itemDto = itemMapper.toDto(item);
            if (sessionId != null && !sessionId.isBlank()) {
                int count = cartRepository.findBySessionIdAndItemId(UUID.fromString(sessionId), id)
                        .map(CartItem::getCount)
                        .orElse(0);
                itemDto.setCount(count);
            }

            return itemDto;
        });
    }
}
