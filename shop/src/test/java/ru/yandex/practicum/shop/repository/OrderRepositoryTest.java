package ru.yandex.practicum.shop.repository;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.test.StepVerifier;
import ru.yandex.practicum.shop.entity.OrderEntity;

import java.math.BigDecimal;

class OrderRepositoryTest extends BaseDataR2dbcTest {

    @Autowired
    private OrderRepository orderRepository;

    @Test
    void save_WhenOrderProvided_PersistsOrder() {
        long userId = 1L;
        OrderEntity order = OrderEntity.builder()
                .userId(userId)
                .total(BigDecimal.valueOf(1000))
                .build();

        orderRepository.save(order)
                .as(StepVerifier::create)
                .expectNextMatches(savedOrder -> savedOrder.getId() != null &&
                        savedOrder.getUserId().equals(userId) &&
                        savedOrder.getTotal().compareTo(BigDecimal.valueOf(1000)) == 0)
                .verifyComplete();
    }

    @Test
    void findByIdAndUserId_WhenOrderExists_ReturnsOrder() {
        long userId = 1L;
        OrderEntity order = OrderEntity.builder()
                .userId(userId)
                .total(BigDecimal.valueOf(1000))
                .build();

        OrderEntity saved = orderRepository.save(order).block();

        Assertions.assertNotNull(saved);
        orderRepository.findByIdAndUserId(saved.getId(), userId)
                .as(StepVerifier::create)
                .expectNextMatches(found -> found.getId().equals(saved.getId()) && found.getUserId().equals(userId))
                .verifyComplete();
    }

    @Test
    void findByIdAndUserId_WhenSessionMismatch_ReturnsEmpty() {
        long userId = 1L;
        long otherUserId = 2L;
        OrderEntity order = OrderEntity.builder()
                .userId(userId)
                .total(BigDecimal.valueOf(1000))
                .build();

        OrderEntity saved = orderRepository.save(order).block();

        Assertions.assertNotNull(saved);
        orderRepository.findByIdAndUserId(saved.getId(), otherUserId)
                .as(StepVerifier::create)
                .verifyComplete();
    }
}
