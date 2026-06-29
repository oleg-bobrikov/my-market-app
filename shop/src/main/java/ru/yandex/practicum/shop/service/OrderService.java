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


@Service
@RequiredArgsConstructor
public class OrderService {
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final CartRepository cartRepository;
    private final ItemRepository itemRepository;
    private final CartService cartService;
    private final ItemService itemService;
    private final ItemMapper itemMapper;
    private final OrderMapper orderMapper;
    private final PaymentClient paymentClient;
    private final TransactionalOperator transactionalOperator;

    public Mono<BigDecimal> getBalance(Long userId) {
        return paymentClient.getBalance(userId);
    }

    public Mono<BigDecimal> calculateTotal(Long userId) {
        return itemService.getCartItems(userId)
                .collectList()
                .flatMap(cartService::getTotalPrice);
    }

    public Mono<Order> buy(Long userId) {
        if (userId == null) {
            return Mono.error(new IllegalStateException("User ID is null"));
        }

        return calculateTotal(userId)
                .flatMap(total -> getBalance(userId)
                        .zipWith(Mono.just(total))
                )
                .flatMap(tuple -> {
                    BigDecimal balance = tuple.getT1();
                    BigDecimal total = tuple.getT2();

                    if (balance.compareTo(total) < 0) {
                        return Mono.error(new InsufficientFundsException("Недостаточно средств на счете"));
                    }

                    return createOrder(userId)
                            .flatMap(order -> paymentClient.pay(new PaymentRequest(userId, order.getId(), order.getTotal()))
                                    .thenReturn(order));
                })
                .onErrorMap(e -> {
                    if (e instanceof InsufficientFundsException || e instanceof IllegalStateException) {
                        return e;
                    }
                    return new PaymentServiceException(e.getMessage());
                })
                .as(transactionalOperator::transactional);
    }

    public Mono<Order> createOrder(Long userId) {
        return itemService.getCartItems(userId)
                .collectList()
                .flatMap(items -> {
                    if (items.isEmpty()) {
                        return Mono.error(new IllegalStateException("Корзина пуста"));
                    }

                    return cartService.getTotalPrice(items)
                            .flatMap(total -> {
                                Order order = Order.builder()
                                        .userId(userId)
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
                                                    .then(cartRepository.deleteByUserId(userId))
                                                    .then(cartService.clearCart(userId))
                                                    .thenReturn(savedOrder);
                                        });
                            });
                });
    }

    public Mono<Order> getOrderByIdAndSessionId(Long id, Long userId) {
        return orderRepository.findByIdAndUserId(id, userId).map(orderMapper::toModel);
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

    public Flux<Order> findByUserId(Long userId) {
        return orderRepository.findByUserId(userId).map(orderMapper::toModel);
    }
}