package ru.yandex.practicum.payment.model;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

public record PaymentRequest(
        String orderId,

        @Schema(type = "string")
        BigDecimal amount) {
}
