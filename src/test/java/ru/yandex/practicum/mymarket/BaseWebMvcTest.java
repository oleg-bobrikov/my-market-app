package ru.yandex.practicum.mymarket;

import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import ru.yandex.practicum.mymarket.controller.CartController;
import ru.yandex.practicum.mymarket.controller.ImageController;
import ru.yandex.practicum.mymarket.controller.ItemController;
import ru.yandex.practicum.mymarket.controller.OrderController;
import org.springframework.test.context.ActiveProfiles;
import ru.yandex.practicum.mymarket.service.CartService;
import ru.yandex.practicum.mymarket.service.ImageService;
import ru.yandex.practicum.mymarket.service.ItemService;
import ru.yandex.practicum.mymarket.service.OrderService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.beans.factory.annotation.Autowired;

@WebMvcTest({CartController.class, ItemController.class, OrderController.class, ImageController.class})
@ActiveProfiles("test")
public abstract class BaseWebMvcTest {
    @Autowired
    protected MockMvc mockMvc;

    @MockitoBean
    protected CartService cartService;

    @MockitoBean
    protected ItemService itemService;

    @MockitoBean
    protected ImageService imageService;

    @MockitoBean
    protected OrderService orderService;
}
