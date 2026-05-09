package ru.yandex.practicum.payment.mymarket.repository;

import org.springframework.boot.test.autoconfigure.data.r2dbc.DataR2dbcTest;
import org.springframework.test.context.ActiveProfiles;

@DataR2dbcTest
@ActiveProfiles("test")
public abstract class BaseDataR2dbcTest {
}
