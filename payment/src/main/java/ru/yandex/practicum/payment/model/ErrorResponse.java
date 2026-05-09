package ru.yandex.practicum.payment.model;

public record ErrorResponse(
        PaymentStatus status,
        String message
) {
}