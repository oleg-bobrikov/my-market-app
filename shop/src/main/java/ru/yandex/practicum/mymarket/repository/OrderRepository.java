package ru.yandex.practicum.mymarket.repository;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.mymarket.entity.OrderEntity;

import java.util.UUID;

@Repository
public interface OrderRepository extends ReactiveCrudRepository<OrderEntity, Long> {
    @Query("""
            SELECT
                orders.id,
                orders.session_id,
                orders.total
            FROM orders
                 WHERE orders.session_id = :sessionId
            ORDER BY orders.id
            """)
    Flux<OrderEntity> findBySessionId(UUID sessionId);

    Mono<OrderEntity> findByIdAndSessionId(Long id, UUID sessionId);
}
