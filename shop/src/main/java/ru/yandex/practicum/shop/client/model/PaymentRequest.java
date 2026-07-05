package ru.yandex.practicum.shop.client.model;

import java.math.BigDecimal;

public record PaymentRequest(
        Long userId,
        Long orderId,
        BigDecimal amount
) {
}
