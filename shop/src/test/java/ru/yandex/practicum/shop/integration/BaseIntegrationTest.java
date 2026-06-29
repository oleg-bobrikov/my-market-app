package ru.yandex.practicum.shop.integration;

import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.junit.jupiter.api.BeforeEach;
import ru.yandex.practicum.shop.ShopApplication;
import ru.yandex.practicum.shop.configuration.EmbeddedRedisConfiguration;
import ru.yandex.practicum.shop.security.CustomUserDetails;

import java.util.List;

import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.springSecurity;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ContextConfiguration(classes = {ShopApplication.class, EmbeddedRedisConfiguration.class})
@AutoConfigureWebTestClient(timeout = "30000")
@ActiveProfiles("test")
public abstract class BaseIntegrationTest {
    @Autowired
    protected WebTestClient webTestClient;

    @Autowired
    private ApplicationContext context;

    @Autowired
    private org.springframework.cache.CacheManager cacheManager;

    @Autowired
    private ru.yandex.practicum.shop.repository.CartRepository cartRepository;

    @Autowired
    private ru.yandex.practicum.shop.repository.OrderRepository orderRepository;

    @BeforeEach
    void setup() {
        if (cacheManager != null) {
            cacheManager.getCacheNames().forEach(name -> {
                var cache = cacheManager.getCache(name);
                if (cache != null) {
                    cache.clear();
                }
            });
        }
        cartRepository.deleteAll().block();
        orderRepository.deleteAll().block();

        this.webTestClient = WebTestClient.bindToApplicationContext(context)
                .apply(springSecurity())
                .configureClient()
                .build();
    }

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
