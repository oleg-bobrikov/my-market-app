package ru.yandex.practicum.mymarket.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.mymarket.dto.ItemDto;
import ru.yandex.practicum.mymarket.mapper.ItemMapper;
import ru.yandex.practicum.mymarket.model.CartAction;
import ru.yandex.practicum.mymarket.model.CartItem;
import ru.yandex.practicum.mymarket.model.Item;
import ru.yandex.practicum.mymarket.repository.CartRepository;
import ru.yandex.practicum.mymarket.repository.ItemRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
public class CartService {
    private final CartRepository cartRepository;
    private final ItemRepository itemRepository;
    private final ItemMapper itemMapper;

    @Autowired
    public CartService(CartRepository cartRepository, ItemRepository itemRepository, ItemMapper itemMapper) {
        this.cartRepository = cartRepository;
        this.itemRepository = itemRepository;
        this.itemMapper = itemMapper;
    }

    @Transactional
    public void updateCartItem(UUID sessionId, Long itemId, CartAction action) {
        cartRepository.findBySessionIdAndItemId(sessionId, itemId).ifPresentOrElse(
                cartItem -> {
                    switch (action) {
                        case PLUS -> {
                            cartItem.setCount(cartItem.getCount() + 1);
                            cartRepository.save(cartItem);
                        }
                        case MINUS -> {
                            if (cartItem.getCount() > 1) {
                                cartItem.setCount(cartItem.getCount() - 1);
                                cartRepository.save(cartItem);
                            } else {
                                cartRepository.delete(cartItem);
                            }
                        }
                        case DELETE -> cartRepository.delete(cartItem);
                        default -> throw new IllegalArgumentException("Unexpected value: " + action);
                    }
                },
                () -> {
                    if (action == CartAction.PLUS) {
                        Item item = itemRepository.findById(itemId)
                                .orElseThrow(() -> new IllegalArgumentException("Item not found"));
                        CartItem cartItem = CartItem.builder()
                                .sessionId(sessionId)
                                .item(item)
                                .count(1)
                                .build();
                        cartRepository.save(cartItem);
                    }
                }
        );
    }

    public List<ItemDto> getCartItems(UUID sessionId) {
        return cartRepository.findBySessionId(sessionId).stream()
                .map(cartItem -> {
                    ItemDto itemDto = itemMapper.toDto(cartItem.getItem());
                    itemDto.setCount(cartItem.getCount());
                    return itemDto;
                })
                .toList();
    }

    public BigDecimal getTotalPrice(List<ItemDto> items) {
        return items.stream()
                .map(item -> item.getPrice()
                        .multiply(BigDecimal.valueOf(item.getCount())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
