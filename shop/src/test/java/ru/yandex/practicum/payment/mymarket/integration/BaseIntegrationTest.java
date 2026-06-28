package ru.yandex.practicum.payment.mymarket.integration;

import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.junit.jupiter.api.BeforeEach;
import ru.yandex.practicum.shop.ShopApplication;
import ru.yandex.practicum.payment.mymarket.configuration.EmbeddedRedisConfiguration;

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

    @BeforeEach
    void setup() {
        this.webTestClient = WebTestClient.bindToApplicationContext(context)
                .apply(springSecurity())
                .configureClient()
                .build();
    }
}
