package ru.yandex.practicum.shop.integration;

import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientManager;
import org.springframework.cache.CacheManager;
import ru.yandex.practicum.shop.repository.CartRepository;
import ru.yandex.practicum.shop.repository.OrderRepository;
import ru.yandex.practicum.shop.ShopApplication;
import ru.yandex.practicum.shop.configuration.EmbeddedRedisConfiguration;
import ru.yandex.practicum.shop.security.CustomUserDetails;

import java.util.List;
import java.util.ArrayList;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.springSecurity;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ContextConfiguration(classes = {ShopApplication.class, EmbeddedRedisConfiguration.class})
@AutoConfigureWebTestClient(timeout = "30000")
@ActiveProfiles("test")
public abstract class BaseIntegrationTest {

    @MockitoBean
    private ReactiveClientRegistrationRepository clientRegistrationRepository;

    @MockitoBean
    private ReactiveOAuth2AuthorizedClientService authorizedClientService;

    @MockitoBean
    private ReactiveOAuth2AuthorizedClientManager authorizedClientManager;

    @Autowired
    protected WebTestClient webTestClient;

    @Autowired
    private ApplicationContext context;

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private OrderRepository orderRepository;

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
    protected List<String> extractTitles(String html) {
        List<String> titles = new ArrayList<>();
        Pattern pattern = Pattern.compile("<h5[^>]*class=\"card-title\"[^>]*>([^<]+)</h5>");
        Matcher matcher = pattern.matcher(html);
        while (matcher.find()) {
            String title = matcher.group(1).trim();
            if (!title.isEmpty()) {
                titles.add(title);
            }
        }
        return titles;
    }
}
