package ru.yandex.practicum.payment.mymarket.repository;

import org.springframework.boot.test.autoconfigure.data.r2dbc.DataR2dbcTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import ru.yandex.practicum.shop.ShopApplication;

@DataR2dbcTest
@ContextConfiguration(classes = ShopApplication.class)
@ActiveProfiles("test")
public abstract class BaseDataR2dbcTest {
}
