package ru.yandex.practicum.mymarket.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import ru.yandex.practicum.mymarket.filter.SessionWebFilter;
import ru.yandex.practicum.mymarket.mapper.ItemMapper;
import ru.yandex.practicum.mymarket.service.CartService;
import ru.yandex.practicum.mymarket.service.ImageService;
import ru.yandex.practicum.mymarket.service.ItemService;
import ru.yandex.practicum.mymarket.service.OrderService;

@WebFluxTest({CartController.class, ItemController.class, OrderController.class, ImageController.class, GlobalErrorHandler.class, SessionWebFilter.class})
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
}
