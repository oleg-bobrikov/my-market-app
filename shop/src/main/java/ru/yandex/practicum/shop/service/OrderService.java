package ru.yandex.practicum.shop.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.shop.client.PaymentClient;
import ru.yandex.practicum.shop.client.model.PaymentRequest;
import ru.yandex.practicum.shop.exception.InsufficientFundsException;
import ru.yandex.practicum.shop.exception.PaymentServiceException;
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
    private final CartService cartService;
    private final ItemMapper itemMapper;
    private final OrderMapper orderMapper;
    private final PaymentClient paymentClient;
    private final TransactionalOperator transactionalOperator;

    public Mono<BigDecimal> getBalance(UUID sessionId) {
        return paymentClient.getBalance(sessionId);
    }

    public Mono<BigDecimal> calculateTotal(UUID sessionId) {
        return cartService.getCartItems(sessionId)
                .collectList()
                .flatMap(cartService::getTotalPrice);
    }

    public Mono<Order> buy(UUID sessionId) {
        return calculateTotal(sessionId)
                .flatMap(total -> getBalance(sessionId)
                        .zipWith(Mono.just(total))
                )
                .onErrorMap(e -> {
                    if (e instanceof InsufficientFundsException) {
                        return e;
                    }
                    return new PaymentServiceException(e.getMessage());
                })
                .flatMap(tuple -> {
                    BigDecimal balance = tuple.getT1();
                    BigDecimal total = tuple.getT2();

                    if (balance.compareTo(total) < 0) {
                        return Mono.error(new InsufficientFundsException("Недостаточно средств на счете"));
                    }

                    return paymentClient.pay(new PaymentRequest(sessionId.toString(), total), sessionId)
                            .then(createOrder(sessionId));
                })
                .as(transactionalOperator::transactional);
    }

    public Mono<Order> createOrder(UUID sessionId) {
        return cartService.getCartItems(sessionId)
                .collectList()
                .flatMap(items -> {
                    if (items.isEmpty()) {
                        return Mono.error(new IllegalStateException("Корзина пуста"));
                    }

                    return cartService.getTotalPrice(items)
                            .flatMap(total -> {
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
                                                    .then(cartService.clearCart(sessionId))
                                                    .thenReturn(savedOrder);
                                        });
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