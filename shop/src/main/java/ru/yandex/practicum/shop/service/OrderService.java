package ru.yandex.practicum.shop.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.shop.mapper.ItemMapper;
import ru.yandex.practicum.shop.mapper.OrderMapper;
import ru.yandex.practicum.shop.model.Item;
import ru.yandex.practicum.shop.model.Order;
import ru.yandex.practicum.shop.model.OrderItem;
import ru.yandex.practicum.shop.repository.CartRepository;
import ru.yandex.practicum.shop.repository.ItemRepository;
import ru.yandex.practicum.shop.repository.OrderItemRepository;
import ru.yandex.practicum.shop.repository.OrderRepository;

import java.math.BigDecimal;
import java.util.UUID;


@Service
@RequiredArgsConstructor
public class OrderService {
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final CartRepository cartRepository;
    private final ItemRepository itemRepository;
    private final ItemMapper itemMapper;
    private final OrderMapper orderMapper;

    @Transactional
    public Mono<Order> createOrder(UUID sessionId) {
        return itemRepository.findBySessionId(sessionId)
                .collectList()
                .flatMap(items -> {
                    if (items.isEmpty()) {
                        return Mono.error(new IllegalStateException("Cart is empty"));
                    }

                    BigDecimal total = items.stream()
                            .map(item -> {
                                Integer count = item.getCount();
                                if (count == null) count = 0; // Guard against null count
                                return item.getPrice().multiply(BigDecimal.valueOf(count));
                            })
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    Order order = Order.builder()
                            .sessionId(sessionId)
                            .total(total)
                            .build();

                    return orderRepository.save(orderMapper.toEntity(order))
                            .map(orderMapper::toModel)
                            .flatMap(savedOrder -> {
                                var orderItems = items.stream()
                                        .map(item -> {
                                            OrderItem orderItem = new OrderItem();
                                            orderItem.setOrderId(savedOrder.getId());
                                            orderItem.setItemId(item.getId());
                                            Integer count = item.getCount();
                                            orderItem.setCount(count != null ? count : 0);
                                            return orderMapper.toEntity(orderItem);
                                        }).toList();

                                return orderItemRepository.saveAll(Flux.fromIterable(orderItems))
                                        .then(cartRepository.deleteBySessionId(sessionId))
                                        .thenReturn(savedOrder);
                            });
                });
    }

    public Mono<Order> getOrderByIdAndSessionId(Long id, UUID sessionId) {
        return orderRepository.findByIdAndSessionId(id, sessionId).map(orderMapper::toModel);
    }

    public Flux<Item> getOrderItems(Long orderId) {
        return orderItemRepository.findByOrderId(orderId)
                .flatMap(orderItem ->
                        itemRepository.findById(orderItem.getItemId())
                                .map(item -> {
                                    Item model = itemMapper.toModel(item);
                                    model.setCount(orderItem.getCount());
                                    return model;
                                })
                );
    }

    public Flux<Order> findBySessionId(UUID sessionId) {
        return orderRepository.findBySessionId(sessionId).map(orderMapper::toModel);
    }
}