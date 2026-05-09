package ru.yandex.practicum.payment.model;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

public record PaymentResponse(
        PaymentStatus status,
        String orderId,

        @Schema(type = "string")
        BigDecimal remainingBalance
) {
}
