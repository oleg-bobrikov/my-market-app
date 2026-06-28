package ru.yandex.practicum.shop.repository;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.shop.entity.OrderEntity;


@Repository
public interface OrderRepository extends ReactiveCrudRepository<OrderEntity, Long> {
    @Query("""
            SELECT
                orders.id,
                orders.user_id,
                orders.total
            FROM orders
                 WHERE orders.user_id = :userId
            ORDER BY orders.id
            """)
    Flux<OrderEntity> findByUserId(Long userId);

    Mono<OrderEntity> findByIdAndUserId(Long id, Long userId);
}
