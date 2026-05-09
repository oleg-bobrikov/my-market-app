package ru.yandex.practicum.payment.mymarket.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import ru.yandex.practicum.shop.ShopApplication;
import ru.yandex.practicum.shop.client.PaymentClient;
import ru.yandex.practicum.shop.controller.*;
import ru.yandex.practicum.shop.filter.SessionWebFilter;
import ru.yandex.practicum.shop.mapper.ItemMapper;
import ru.yandex.practicum.shop.service.CartService;
import ru.yandex.practicum.shop.service.ImageService;
import ru.yandex.practicum.shop.service.ItemService;
import ru.yandex.practicum.shop.service.OrderService;

@WebFluxTest(controllers = {CartController.class, ItemController.class, OrderController.class, ImageController.class, GlobalErrorHandler.class, SessionWebFilter.class}, properties = {"spring.main.allow-bean-definition-overriding=true"})
@ContextConfiguration(classes = ShopApplication.class)
@ActiveProfiles("test")
public class BaseWebFluxTest {
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
}
