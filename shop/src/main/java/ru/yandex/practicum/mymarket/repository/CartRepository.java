package ru.yandex.practicum.mymarket.repository;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.mymarket.entity.CartItemEntity;

import java.util.UUID;

@Repository
public interface CartRepository extends ReactiveCrudRepository<CartItemEntity, Long> {
    Mono<CartItemEntity> findBySessionIdAndItemId(UUID sessionId, Long itemId);

    Flux<CartItemEntity> findBySessionId(UUID sessionId);

    @Query("""
            DELETE FROM carts
            WHERE session_id = :sessionId
            """)
    Mono<Void> deleteBySessionId(UUID sessionId);
}
