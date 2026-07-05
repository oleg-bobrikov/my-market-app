package ru.yandex.practicum.shop.controller;

import io.r2dbc.spi.ConnectionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import ru.yandex.practicum.shop.config.SecurityConfig;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientManager;
import ru.yandex.practicum.shop.ShopApplication;
import org.springframework.transaction.ReactiveTransactionManager;
import org.springframework.transaction.reactive.TransactionalOperator;
import ru.yandex.practicum.shop.client.PaymentClient;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import ru.yandex.practicum.shop.mapper.CartItemMapper;
import ru.yandex.practicum.shop.mapper.ItemMapper;
import ru.yandex.practicum.shop.mapper.OrderMapper;
import ru.yandex.practicum.shop.repository.CartRepository;
import ru.yandex.practicum.shop.repository.ItemRepository;
import ru.yandex.practicum.shop.repository.OrderItemRepository;
import ru.yandex.practicum.shop.repository.OrderRepository;
import ru.yandex.practicum.shop.security.ReactiveUserService;
import ru.yandex.practicum.shop.security.CustomUserDetails;
import ru.yandex.practicum.shop.service.*;

import java.util.List;

@WebFluxTest(controllers = {CartController.class, ItemController.class, OrderController.class, ImageController.class, GlobalErrorHandler.class, LoginController.class}, properties = {"spring.main.allow-bean-definition-overriding=true", "app.cookie.max-age=7d"})
@ContextConfiguration(classes = {ShopApplication.class})
@Import(SecurityConfig.class)
@ActiveProfiles("test")
public class BaseWebFluxTest {

    @MockitoBean
    private ReactiveClientRegistrationRepository clientRegistrationRepository;

    @MockitoBean
    private ReactiveOAuth2AuthorizedClientService authorizedClientService;

    @MockitoBean
    private ReactiveOAuth2AuthorizedClientManager authorizedClientManager;

    @MockitoBean
    protected CartRepository cartRepository;

    @MockitoBean
    protected ItemRepository itemRepository;

    @MockitoBean
    protected OrderRepository orderRepository;

    @MockitoBean
    protected OrderItemRepository orderItemRepository;

    @MockitoBean
    protected ConnectionFactory connectionFactory;

    @MockitoBean
    protected ReactiveTransactionManager transactionManager;

    @MockitoBean
    protected TransactionalOperator transactionalOperator;

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
    protected OrderMapper orderMapper;

    @MockitoBean
    protected ReactiveUserService reactiveUserService;

    @MockitoBean
    protected CartItemMapper cartItemMapper;

    @MockitoBean
    private PasswordEncoder passwordEncoder;

    protected Authentication auth(long userId) {
        CustomUserDetails user = CustomUserDetails.builder()
                .userId(userId)
                .username("user")
                .password("password")
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_USER")))
                .build();

        return new UsernamePasswordAuthenticationToken(
                user,
                user.getPassword(),
                user.getAuthorities()
        );
    }
}
