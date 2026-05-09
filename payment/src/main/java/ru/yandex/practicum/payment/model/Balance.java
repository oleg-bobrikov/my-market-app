package ru.yandex.practicum.payment.model;

import java.math.BigDecimal;
import java.util.UUID;

public record Balance(
        UUID clientId,
        BigDecimal balance
) {
}
