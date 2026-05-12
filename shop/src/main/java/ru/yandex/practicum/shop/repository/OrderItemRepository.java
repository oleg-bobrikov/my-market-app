package ru.yandex.practicum.shop.repository;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import ru.yandex.practicum.shop.entity.OrderItemEntity;

@Repository
public interface OrderItemRepository extends ReactiveCrudRepository<OrderItemEntity, Long> {
    Flux<OrderItemEntity> findByOrderId(Long orderId);
}
