package ru.yandex.practicum.shop.exception;

public class PaymentServiceException extends RuntimeException {
    public PaymentServiceException(String message) {
        super(message);
    }
}
