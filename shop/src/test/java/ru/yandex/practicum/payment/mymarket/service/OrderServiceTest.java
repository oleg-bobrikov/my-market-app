package ru.yandex.practicum.payment.mymarket.service;

import com.github.f4b6a3.uuid.UuidCreator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import ru.yandex.practicum.shop.client.PaymentClient;
import ru.yandex.practicum.shop.client.model.PaymentRequest;
import ru.yandex.practicum.shop.entity.ItemEntity;
import ru.yandex.practicum.shop.entity.OrderEntity;
import ru.yandex.practicum.shop.entity.OrderItemEntity;
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
import ru.yandex.practicum.shop.service.OrderService;

import java.math.BigDecimal;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private CartRepository cartRepository;

    @Mock
    private ItemRepository itemRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private ItemMapper itemMapper;

    @Mock
    private OrderMapper orderMapper;

    @Mock
    private PaymentClient paymentClient;

    @InjectMocks
    private OrderService orderService;

    @Test
    void createOrder_Success() {
        UUID sessionId = UuidCreator.getTimeOrderedEpoch();
        ItemEntity itemEntity = ItemEntity.builder()
                .id(1L)
                .price(new BigDecimal("100.00"))
                .count(2)
                .build();
        
        Order model = Order.builder().sessionId(sessionId).total(new BigDecimal("200.00")).build();
        OrderEntity entity = OrderEntity.builder().sessionId(sessionId).total(new BigDecimal("200.00")).build();
        Order savedModel = Order.builder().id(10L).sessionId(sessionId).total(new BigDecimal("200.00")).build();
        OrderEntity savedEntity = OrderEntity.builder().id(10L).sessionId(sessionId).total(new BigDecimal("200.00")).build();

        when(itemRepository.findBySessionId(sessionId)).thenReturn(Flux.just(itemEntity));
        when(orderMapper.toEntity(any(Order.class))).thenReturn(entity);
        when(orderRepository.save(any(OrderEntity.class))).thenReturn(Mono.just(savedEntity));
        when(orderMapper.toModel(savedEntity)).thenReturn(savedModel);
        when(orderMapper.toEntity(any(OrderItem.class))).thenReturn(new OrderItemEntity());
        when(orderItemRepository.saveAll(any(Flux.class))).thenReturn(Flux.empty());
        when(cartRepository.deleteBySessionId(sessionId)).thenReturn(Mono.empty());

        orderService.createOrder(sessionId)
                .as(StepVerifier::create)
                .expectNextMatches(order -> order.getId().equals(10L) && order.getTotal().compareTo(new BigDecimal("200.00")) == 0)
                .verifyComplete();

        verify(orderRepository).save(any(OrderEntity.class));
        verify(orderItemRepository).saveAll(any(Flux.class));
        verify(cartRepository).deleteBySessionId(sessionId);
    }

    @Test
    void createOrder_ThrowsException_WhenCartIsEmpty() {
        UUID sessionId = UuidCreator.getTimeOrderedEpoch();
        when(itemRepository.findBySessionId(sessionId)).thenReturn(Flux.empty());

        orderService.createOrder(sessionId)
                .as(StepVerifier::create)
                .expectError(IllegalStateException.class)
                .verify();

        verify(orderRepository, never()).save(any());
    }

    @Test
    void getOrderItems_ShouldPopulateCount() {
        Long orderId = 10L;
        Long itemId = 1L;
        int count = 5;

        OrderItemEntity orderItemEntity = OrderItemEntity.builder()
                .orderId(orderId)
                .itemId(itemId)
                .count(count)
                .build();

        ItemEntity itemEntity = ItemEntity.builder()
                .id(itemId)
                .price(new BigDecimal("10.00"))
                .build();

        Item itemModel = new Item();
        itemModel.setId(itemId);
        itemModel.setPrice(new BigDecimal("10.00"));

        when(orderItemRepository.findByOrderId(orderId)).thenReturn(Flux.just(orderItemEntity));
        when(itemRepository.findById(itemId)).thenReturn(Mono.just(itemEntity));
        when(itemMapper.toModel(itemEntity)).thenReturn(itemModel);

        orderService.getOrderItems(orderId)
                .as(StepVerifier::create)
                .expectNextMatches(model -> model.getId().equals(itemId) && model.getCount() == count)
                .verifyComplete();
    }

    @Test
    void buy_Success() {
        UUID sessionId = UuidCreator.getTimeOrderedEpoch();
        ItemEntity itemEntity = ItemEntity.builder()
                .id(1L)
                .price(new BigDecimal("100.00"))
                .count(2)
                .build();
        BigDecimal total = new BigDecimal("200.00");
        BigDecimal balance = new BigDecimal("300.00");
        Order savedModel = Order.builder().id(10L).sessionId(sessionId).total(total).build();
        OrderEntity savedEntity = OrderEntity.builder().id(10L).sessionId(sessionId).total(total).build();

        when(itemRepository.findBySessionId(sessionId)).thenReturn(Flux.just(itemEntity));
        when(paymentClient.getBalance(sessionId)).thenReturn(Mono.just(balance));
        when(paymentClient.pay(any(PaymentRequest.class), eq(sessionId))).thenReturn(Mono.empty());
        when(orderMapper.toEntity(any(Order.class))).thenReturn(new OrderEntity());
        when(orderRepository.save(any(OrderEntity.class))).thenReturn(Mono.just(savedEntity));
        when(orderMapper.toModel(savedEntity)).thenReturn(savedModel);
        when(orderMapper.toEntity(any(OrderItem.class))).thenReturn(new OrderItemEntity());
        when(orderItemRepository.saveAll(any(Flux.class))).thenReturn(Flux.empty());
        when(cartRepository.deleteBySessionId(sessionId)).thenReturn(Mono.empty());

        orderService.buy(sessionId)
                .as(StepVerifier::create)
                .expectNextMatches(order -> order.getId().equals(10L))
                .verifyComplete();
    }

    @Test
    void buy_InsufficientFunds() {
        UUID sessionId = UuidCreator.getTimeOrderedEpoch();
        ItemEntity itemEntity = ItemEntity.builder()
                .id(1L)
                .price(new BigDecimal("100.00"))
                .count(2)
                .build();
        BigDecimal balance = new BigDecimal("150.00");

        when(itemRepository.findBySessionId(sessionId)).thenReturn(Flux.just(itemEntity));
        when(paymentClient.getBalance(sessionId)).thenReturn(Mono.just(balance));

        orderService.buy(sessionId)
                .as(StepVerifier::create)
                .expectError(InsufficientFundsException.class)
                .verify();
    }

    @Test
    void buy_PaymentServiceError() {
        UUID sessionId = UuidCreator.getTimeOrderedEpoch();
        ItemEntity itemEntity = ItemEntity.builder()
                .id(1L)
                .price(new BigDecimal("100.00"))
                .count(2)
                .build();

        when(itemRepository.findBySessionId(sessionId)).thenReturn(Flux.just(itemEntity));
        when(paymentClient.getBalance(sessionId)).thenReturn(Mono.error(new RuntimeException("Conn error")));

        orderService.buy(sessionId)
                .as(StepVerifier::create)
                .expectError(PaymentServiceException.class)
                .verify();
    }

    @Test
    void getOrderByIdAndSessionId_Success() {
        Long orderId = 1L;
        UUID sessionId = UuidCreator.getTimeOrderedEpoch();
        OrderEntity entity = OrderEntity.builder().id(orderId).sessionId(sessionId).build();
        Order model = Order.builder().id(orderId).sessionId(sessionId).build();

        when(orderRepository.findByIdAndSessionId(orderId, sessionId)).thenReturn(Mono.just(entity));
        when(orderMapper.toModel(entity)).thenReturn(model);

        orderService.getOrderByIdAndSessionId(orderId, sessionId)
                .as(StepVerifier::create)
                .expectNext(model)
                .verifyComplete();

        verify(orderRepository).findByIdAndSessionId(orderId, sessionId);
    }
}
