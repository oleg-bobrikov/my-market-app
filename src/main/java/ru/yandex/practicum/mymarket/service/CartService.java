package ru.yandex.practicum.mymarket.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.mymarket.entity.CartItemEntity;
import ru.yandex.practicum.mymarket.mapper.CartItemMapper;
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
    private final CartItemMapper cartItemMapper;

    @Autowired
    public CartService(CartRepository cartRepository, ItemRepository itemRepository, ItemMapper itemMapper, CartItemMapper cartItemMapper) {
        this.cartRepository = cartRepository;
        this.itemRepository = itemRepository;
        this.itemMapper = itemMapper;
        this.cartItemMapper = cartItemMapper;
    }

    @Transactional
    public Mono<CartItem> updateCartItem(UUID sessionId, Long itemId, CartAction action) {
        return cartRepository.findBySessionIdAndItemId(sessionId, itemId)
                .map(cartItemMapper::toModel)
                .flatMap(cartItem -> handleExisting(cartItem, action))
                .switchIfEmpty(handleNew(sessionId, itemId, action));
    }

    private Mono<CartItem> handleExisting(CartItem cartItem, CartAction action) {
        return switch (action) {
            case PLUS -> {
                cartItem.setCount(cartItem.getCount() + 1);
                yield cartRepository.save(cartItemMapper.toEntity(cartItem)).map(cartItemMapper::toModel);
            }
            case MINUS -> {
                if (cartItem.getCount() > 1) {
                    cartItem.setCount(cartItem.getCount() - 1);
                    yield cartRepository.save(cartItemMapper.toEntity(cartItem)).map(cartItemMapper::toModel);
                } else {
                    yield cartRepository.delete(cartItemMapper.toEntity(cartItem)).then(Mono.empty());
                }
            }
            case DELETE -> cartRepository.delete(cartItemMapper.toEntity(cartItem)).then(Mono.empty());
        };
    }

    private Mono<CartItem> handleNew(UUID sessionId, Long itemId, CartAction action) {
        if (action != CartAction.PLUS) {
            return Mono.empty();
        }

        return itemRepository.findById(itemId)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Item not found")))
                .flatMap(item -> {
                    CartItem cartItem = CartItem.builder()
                            .sessionId(sessionId)
                            .itemId(itemId)
                            .count(1)
                            .build();

                    return cartRepository.save(cartItemMapper.toEntity(cartItem)).map(cartItemMapper::toModel);
                });
    }

    public Flux<Item> getCartItems(UUID sessionId) {
        return cartRepository.findBySessionId(sessionId)
                .collectMap(CartItemEntity::getItemId, CartItemEntity::getCount)
                .flatMapMany(cartCounts ->
                        itemRepository.findAllById(cartCounts.keySet())
                                .map(item -> {
                                    Item model = itemMapper.toModel(item);
                                    model.setCount(cartCounts.getOrDefault(item.getId(), 0));
                                    return model;
                                })
                );
    }

    public Mono<BigDecimal> getTotalPrice(List<Item> items) {
        return Mono.fromSupplier(() ->
                items.stream()
                        .map(item -> {
                            BigDecimal price = item.getPrice() != null ? item.getPrice() : BigDecimal.ZERO;
                            int count = item.getCount() != null ? item.getCount() : 0;
                            return price.multiply(BigDecimal.valueOf(count));
                        })
                        .reduce(BigDecimal.ZERO, BigDecimal::add)
        );
    }
}
