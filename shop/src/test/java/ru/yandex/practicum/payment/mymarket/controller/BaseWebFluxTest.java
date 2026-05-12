package ru.yandex.practicum.payment.mymarket.controller;

import org.springframework.context.annotation.Import;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import ru.yandex.practicum.shop.client.PaymentClient;
import ru.yandex.practicum.shop.controller.*;
import ru.yandex.practicum.shop.filter.SessionWebFilter;
import ru.yandex.practicum.shop.mapper.ItemMapper;
import ru.yandex.practicum.shop.service.CartService;
import ru.yandex.practicum.shop.service.ImageService;
import ru.yandex.practicum.shop.service.ItemService;
import ru.yandex.practicum.shop.service.OrderService;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import ru.yandex.practicum.shop.mapper.CartItemMapper;
import ru.yandex.practicum.shop.mapper.OrderMapper;

@WebFluxTest(controllers = {CartController.class, ItemController.class, OrderController.class, ImageController.class, GlobalErrorHandler.class}, properties = {"spring.main.allow-bean-definition-overriding=true", "app.cookie.max-age=7d"})
@ContextConfiguration(classes = {ru.yandex.practicum.shop.ShopApplication.class})
@Import(SessionWebFilter.class)
@ActiveProfiles("test")
public class BaseWebFluxTest {

    @MockitoBean
    protected ru.yandex.practicum.shop.repository.CartRepository cartRepository;

    @MockitoBean
    protected ru.yandex.practicum.shop.repository.ItemRepository itemRepository;

    @MockitoBean
    protected ru.yandex.practicum.shop.repository.OrderRepository orderRepository;

    @MockitoBean
    protected ru.yandex.practicum.shop.repository.OrderItemRepository orderItemRepository;

    @MockitoBean
    protected io.r2dbc.spi.ConnectionFactory connectionFactory;

    @MockitoBean
    protected org.springframework.transaction.ReactiveTransactionManager transactionManager;

    @MockitoBean
    protected org.springframework.transaction.reactive.TransactionalOperator transactionalOperator;

    @Autowired
    protected WebTestClient webTestClient;

    @MockitoBean
    protected CartService cartService;

    @MockitoBean
    protected ItemService itemService;

    @MockitoBean
    protected ImageService imageService;

    @MockitoBean
    protected OrderService orderService;

    @MockitoBean
    protected ItemMapper itemMapper;

    @MockitoBean
    protected PaymentClient paymentClient;

    @MockitoBean
    protected ReactiveRedisTemplate<String, String> redisTemplate;

    @MockitoBean
    protected CartItemMapper cartItemMapper;

    @MockitoBean
    protected OrderMapper orderMapper;
}
