package ru.yandex.practicum.payment.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.payment.exception.InsufficientFundsException;
import ru.yandex.practicum.payment.model.ErrorResponse;
import ru.yandex.practicum.payment.model.PaymentStatus;

@RestControllerAdvice
public class PaymentExceptionHandler {

    @ExceptionHandler(InsufficientFundsException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleInsufficientFundsException(InsufficientFundsException ex) {
        ErrorResponse errorResponse = new ErrorResponse();
        errorResponse.setStatus(PaymentStatus.ERROR);
        errorResponse.setMessage(ex.getMessage());
        return Mono.just(ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(errorResponse));
    }

    @ExceptionHandler(Exception.class)
    public Mono<ResponseEntity<ErrorResponse>> handleGenericException(Exception ex) {
        ErrorResponse errorResponse = new ErrorResponse();
        errorResponse.setStatus(PaymentStatus.ERROR);
        errorResponse.setMessage("Внутренняя ошибка сервера");
        return Mono.just(ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(errorResponse));
    }
}
