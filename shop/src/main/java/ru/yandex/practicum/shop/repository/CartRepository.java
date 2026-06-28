package ru.yandex.practicum.shop.repository;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.shop.entity.CartItemEntity;


@Repository
public interface CartRepository extends ReactiveCrudRepository<CartItemEntity, Long> {
    Mono<CartItemEntity> findByUserIdAndItemId(Long userId, Long itemId);

    Flux<CartItemEntity> findByUserId(Long userId);

    @Query("""
            DELETE FROM carts
            WHERE user_id = :userId
            """)
    Mono<Void> deleteByUserId(Long userId);
}
