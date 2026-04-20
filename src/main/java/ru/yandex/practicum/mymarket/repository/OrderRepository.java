package ru.yandex.practicum.mymarket.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.mymarket.model.Order;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
}
