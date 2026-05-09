package ru.yandex.practicum.payment.mymarket.repository;

import com.github.f4b6a3.uuid.UuidCreator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.test.StepVerifier;
import ru.yandex.practicum.shop.entity.OrderEntity;
import ru.yandex.practicum.shop.repository.OrderRepository;

import java.math.BigDecimal;
import java.util.UUID;

class OrderRepositoryTest extends BaseDataR2dbcTest {

    @Autowired
    private OrderRepository orderRepository;

    @Test
    void save_shouldPersistOrder() {
        UUID sessionId = UuidCreator.getTimeOrderedEpoch();
        OrderEntity order = OrderEntity.builder()
                .sessionId(sessionId)
                .total(BigDecimal.valueOf(1000))
                .build();

        orderRepository.save(order)
                .as(StepVerifier::create)
                .expectNextMatches(savedOrder -> {
                    return savedOrder.getId() != null &&
                            savedOrder.getSessionId().equals(sessionId) &&
                            savedOrder.getTotal().compareTo(BigDecimal.valueOf(1000)) == 0;
                })
                .verifyComplete();
    }

    @Test
    void findByIdAndSessionId_shouldReturnOrder() {
        UUID sessionId = UuidCreator.getTimeOrderedEpoch();
        OrderEntity order = OrderEntity.builder()
                .sessionId(sessionId)
                .total(BigDecimal.valueOf(1000))
                .build();

        OrderEntity saved = orderRepository.save(order).block();

        orderRepository.findByIdAndSessionId(saved.getId(), sessionId)
                .as(StepVerifier::create)
                .expectNextMatches(found -> found.getId().equals(saved.getId()) && found.getSessionId().equals(sessionId))
                .verifyComplete();
    }

    @Test
    void findByIdAndSessionId_shouldReturnEmpty_whenSessionIdMismatch() {
        UUID sessionId = UuidCreator.getTimeOrderedEpoch();
        UUID otherSessionId = UuidCreator.getTimeOrderedEpoch();
        OrderEntity order = OrderEntity.builder()
                .sessionId(sessionId)
                .total(BigDecimal.valueOf(1000))
                .build();

        OrderEntity saved = orderRepository.save(order).block();

        orderRepository.findByIdAndSessionId(saved.getId(), otherSessionId)
                .as(StepVerifier::create)
                .verifyComplete();
    }
}
