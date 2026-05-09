package ru.yandex.practicum.payment.mymarket.integration;

import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.beans.factory.annotation.Autowired;
import ru.yandex.practicum.shop.ShopApplication;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration(classes = ShopApplication.class)
@AutoConfigureWebTestClient(timeout = "30000")
@ActiveProfiles("test")
public abstract class BaseIntegrationTest {
    @Autowired
    protected WebTestClient webTestClient;
}
