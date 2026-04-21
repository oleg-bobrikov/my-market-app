package ru.yandex.practicum.mymarket.repository;

import com.github.f4b6a3.uuid.UuidCreator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ru.yandex.practicum.mymarket.BaseDataJpaTest;
import ru.yandex.practicum.mymarket.model.Order;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class OrderRepositoryTest extends BaseDataJpaTest {

    @Autowired
    private OrderRepository orderRepository;

    @Test
    void save_shouldPersistOrder() {
        Order order = Order.builder()
                .sessionId(UuidCreator.getTimeOrderedEpoch())
                .total(1000L)
                .build();

        Order savedOrder = orderRepository.save(order);

        assertThat(savedOrder.getId()).isNotNull();
        assertThat(orderRepository.findById(savedOrder.getId())).isPresent();
    }
}
