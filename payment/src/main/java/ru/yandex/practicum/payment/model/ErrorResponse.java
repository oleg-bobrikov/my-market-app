package ru.yandex.practicum.payment.model;

public record ErrorResponse(
        Enum<PaymentStatus> status,
        String message
) {
}