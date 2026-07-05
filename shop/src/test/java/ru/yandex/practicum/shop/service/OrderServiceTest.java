package ru.yandex.practicum.shop.service;

import org.mockito.ArgumentMatchers;
import org.springframework.transaction.reactive.TransactionalOperator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;
import org.reactivestreams.Publisher;
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

import java.math.BigDecimal;

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

    @Mock
    private CartService cartService;

    @Mock
    private ItemService itemService;

    @Mock
    private TransactionalOperator transactionalOperator;

    @InjectMocks
    private OrderService orderService;

    @SuppressWarnings("unchecked")
    private void mockTransactional() {
        when(transactionalOperator.transactional(any(Mono.class)))
                .thenAnswer((Answer<Mono<?>>) invocation -> invocation.getArgument(0));
    }

    @Test
    void createOrder_WhenSuccessful_CreatesOrder() {
        Long userId = 1L;
        Item item = new Item();
        item.setId(1L);
        item.setPrice(new BigDecimal("100.00"));
        item.setCount(2);

        OrderEntity entity = OrderEntity.builder().userId(userId).total(new BigDecimal("200.00")).build();
        Order savedModel = Order.builder().id(10L).userId(userId).total(new BigDecimal("200.00")).build();
        OrderEntity savedEntity = OrderEntity.builder().id(10L).userId(userId).total(new BigDecimal("200.00")).build();

        when(itemService.getCartItems(userId)).thenReturn(Flux.just(item));
        when(cartService.getTotalPrice(anyList())).thenReturn(Mono.just(new BigDecimal("200.00")));
        when(orderMapper.toEntity(any(Order.class))).thenReturn(entity);
        when(orderRepository.save(any(OrderEntity.class))).thenReturn(Mono.just(savedEntity));
        when(orderMapper.toModel(savedEntity)).thenReturn(savedModel);
        when(orderMapper.toEntity(any(OrderItem.class))).thenReturn(new OrderItemEntity());
        when(orderItemRepository.saveAll(ArgumentMatchers.<Publisher<OrderItemEntity>>any())).thenReturn(Flux.empty());
        when(cartRepository.deleteByUserId(userId)).thenReturn(Mono.empty());
        when(cartService.clearCart(userId)).thenReturn(Mono.empty());

        orderService.createOrder(userId)
                .as(StepVerifier::create)
                .expectNextMatches(order -> order.getId().equals(10L) && order.getTotal().compareTo(new BigDecimal("200.00")) == 0)
                .verifyComplete();

        verify(orderRepository).save(any(OrderEntity.class));
        verify(orderItemRepository).saveAll(ArgumentMatchers.<Publisher<OrderItemEntity>>any());
        verify(cartRepository).deleteByUserId(userId);
        verify(cartService).clearCart(userId);
    }

    @Test
    void createOrder_WhenCartIsEmpty_ThrowsException() {
        Long userId = 1L;
        when(itemService.getCartItems(userId)).thenReturn(Flux.empty());

        orderService.createOrder(userId)
                .as(StepVerifier::create)
                .expectError(IllegalStateException.class)
                .verify();

        verify(orderRepository, never()).save(any());
    }

    @Test
    void getOrderItems_WhenItemsExist_PopulatesCount() {
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
    void buy_WhenPaymentSuccessful_CreatesOrder() {
        mockTransactional();
        Long userId = 1L;
        BigDecimal total = new BigDecimal("200.00");
        BigDecimal balance = new BigDecimal("300.00");
        Order savedModel = Order.builder().id(10L).userId(userId).total(total).build();
        OrderEntity savedEntity = OrderEntity.builder().id(10L).userId(userId).total(total).build();

        when(itemService.getCartItems(userId)).thenReturn(Flux.just(new Item()));
        when(cartService.getTotalPrice(anyList())).thenReturn(Mono.just(total));
        when(paymentClient.getBalance(userId)).thenReturn(Mono.just(balance));
        when(paymentClient.pay(any(PaymentRequest.class))).thenReturn(Mono.empty());
        when(orderMapper.toEntity(any(Order.class))).thenReturn(new OrderEntity());
        when(orderRepository.save(any(OrderEntity.class))).thenReturn(Mono.just(savedEntity));
        when(orderMapper.toModel(savedEntity)).thenReturn(savedModel);
        when(orderMapper.toEntity(any(OrderItem.class))).thenReturn(new OrderItemEntity());
        when(orderItemRepository.saveAll(ArgumentMatchers.<Publisher<OrderItemEntity>>any())).thenReturn(Flux.empty());
        when(cartRepository.deleteByUserId(userId)).thenReturn(Mono.empty());
        when(cartService.clearCart(userId)).thenReturn(Mono.empty());

        orderService.buy(userId)
                .as(StepVerifier::create)
                .expectNextMatches(order -> order.getId().equals(10L))
                .verifyComplete();
    }

    @Test
    void buy_WhenBalanceInsufficient_ThrowsException() {
        mockTransactional();
        Long userId = 1L;
        BigDecimal total = new BigDecimal("200.00");
        BigDecimal balance = new BigDecimal("150.00");

        when(itemService.getCartItems(userId)).thenReturn(Flux.just(new Item()));
        when(cartService.getTotalPrice(anyList())).thenReturn(Mono.just(total));
        when(paymentClient.getBalance(userId)).thenReturn(Mono.just(balance));

        orderService.buy(userId)
                .as(StepVerifier::create)
                .expectError(InsufficientFundsException.class)
                .verify();
    }

    @Test
    void buy_WhenPaymentServiceFails_ThrowsException() {
        mockTransactional();
        BigDecimal total = new BigDecimal("200.00");
        Long userId = 1L;

        when(itemService.getCartItems(userId)).thenReturn(Flux.just(new Item()));
        when(cartService.getTotalPrice(anyList())).thenReturn(Mono.just(total));
        when(paymentClient.getBalance(userId)).thenReturn(Mono.error(new RuntimeException("Conn error")));

        orderService.buy(userId)
                .as(StepVerifier::create)
                .expectError(PaymentServiceException.class)
                .verify();
    }

    @Test
    void getOrderByIdAndSessionId_WhenOrderExists_ReturnsOrder() {
        Long orderId = 1L;
        Long userId = 1L;
        OrderEntity entity = OrderEntity.builder().id(orderId).userId(userId).build();
        Order model = Order.builder().id(orderId).userId(userId).build();

        when(orderRepository.findByIdAndUserId(orderId, userId)).thenReturn(Mono.just(entity));
        when(orderMapper.toModel(entity)).thenReturn(model);

        orderService.getOrderByIdAndSessionId(orderId, userId)
                .as(StepVerifier::create)
                .expectNext(model)
                .verifyComplete();

        verify(orderRepository).findByIdAndUserId(orderId, userId);
    }
}
