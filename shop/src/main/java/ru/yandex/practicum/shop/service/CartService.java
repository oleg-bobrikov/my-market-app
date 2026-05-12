package ru.yandex.practicum.shop.service;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.shop.dto.ItemDto;
import ru.yandex.practicum.shop.entity.CartItemEntity;
import ru.yandex.practicum.shop.model.CartAction;
import ru.yandex.practicum.shop.model.Item;
import ru.yandex.practicum.shop.repository.CartRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class CartService {
    private final CartRepository cartRepository;

    public CartService(CartRepository cartRepository) {
        this.cartRepository = cartRepository;
    }

    @CacheEvict(value = "carts", key = "#sessionId")
    public Mono<Void> updateCartItem(UUID sessionId, Long itemId, CartAction action) {
        return cartRepository.findBySessionIdAndItemId(sessionId, itemId)
                .flatMap(entity -> {
                    switch (action) {
                        case PLUS -> entity.setCount(entity.getCount() + 1);
                        case MINUS -> {
                            if (entity.getCount() <= 1) {
                                return cartRepository.delete(entity).then(Mono.empty());
                            }
                            entity.setCount(entity.getCount() - 1);
                        }
                        case DELETE -> {
                            return cartRepository.delete(entity).then(Mono.empty());
                        }
                    }
                    return cartRepository.save(entity);
                })
                .switchIfEmpty(Mono.defer(() -> {
                    if (action == CartAction.PLUS) {
                        return cartRepository.save(
                                CartItemEntity.builder()
                                        .sessionId(sessionId)
                                        .itemId(itemId)
                                        .count(1)
                                        .build()
                        );
                    }
                    return Mono.empty();
                }))
                .then();
    }

    @Cacheable(value = "carts", key = "#sessionId")
    public Mono<Map<Long, Integer>> getCartCounts(UUID sessionId) {
        if (sessionId == null) {
            return Mono.empty();
        }
        return cartRepository.findBySessionId(sessionId)
                .collect(Collectors.toMap(
                        CartItemEntity::getItemId,
                        CartItemEntity::getCount
                ));
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

    @CacheEvict(value = "carts", key = "#sessionId")
    public Mono<Void> clearCart(UUID sessionId) {
        return cartRepository.deleteBySessionId(sessionId);
    }
}
