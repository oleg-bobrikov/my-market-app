package ru.yandex.practicum.mymarket.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.mymarket.model.CartAction;
import ru.yandex.practicum.mymarket.model.CartItem;
import ru.yandex.practicum.mymarket.model.Item;
import ru.yandex.practicum.mymarket.repository.CartRepository;
import ru.yandex.practicum.mymarket.repository.ItemRepository;

import java.util.UUID;

@Service
public class CartService {
    private final CartRepository cartRepository;
    private final ItemRepository itemRepository;

    @Autowired
    public CartService(CartRepository cartRepository, ItemRepository itemRepository) {
        this.cartRepository = cartRepository;
        this.itemRepository = itemRepository;
    }

    @Transactional
    public void updateCartItem(UUID sessionId, Long itemId, CartAction action) {
        cartRepository.findBySessionIdAndItemId(sessionId, itemId).ifPresentOrElse(
                cartItem -> {
                    if (CartAction.PLUS.equals(action)) {
                        cartItem.setCount(cartItem.getCount() + 1);
                        cartRepository.save(cartItem);
                    } else if (CartAction.MINUS.equals(action)) {
                        if (cartItem.getCount() > 1) {
                            cartItem.setCount(cartItem.getCount() - 1);
                            cartRepository.save(cartItem);
                        } else {
                            cartRepository.delete(cartItem);
                        }
                    } else if (CartAction.DELETE.equals(action)) {
                        cartRepository.delete(cartItem);
                    }
                },
                () -> {
                    if (CartAction.PLUS.equals(action)) {
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
}
