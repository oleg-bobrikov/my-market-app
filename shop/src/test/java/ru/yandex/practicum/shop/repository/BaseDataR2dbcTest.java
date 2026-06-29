package ru.yandex.practicum.shop.repository;

import org.springframework.boot.test.autoconfigure.data.r2dbc.DataR2dbcTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.context.annotation.Import;
import ru.yandex.practicum.shop.ShopApplication;
import ru.yandex.practicum.shop.config.SecurityConfig;
import ru.yandex.practicum.shop.security.ReactiveUserServiceImpl;

@DataR2dbcTest
@ContextConfiguration(classes = ShopApplication.class)
@ActiveProfiles("test")
public abstract class BaseDataR2dbcTest {
}
