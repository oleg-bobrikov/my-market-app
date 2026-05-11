package ru.yandex.practicum.payment.model;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.UUID;

public record Balance(
        UUID clientId,
        @Schema(type = "string")
        BigDecimal balance
) {
}
