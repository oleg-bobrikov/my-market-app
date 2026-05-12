package ru.yandex.practicum.shop.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebInputException;

@Slf4j
@ControllerAdvice
public class GlobalErrorHandler {
    @ExceptionHandler(ServerWebInputException.class)
    public ResponseEntity<String> handleServerWebInputException(ServerWebInputException ex) {
        log.error("Bad Request: {}", ex.getMessage(), ex);
        return ResponseEntity
                .badRequest()
                .body(ex.getMessage());
    }

    @ExceptionHandler(WebExchangeBindException.class)
    public ResponseEntity<String> handleWebExchangeBindException(WebExchangeBindException ex) {
        log.error("Validation error: {}", ex.getMessage(), ex);

        String details = ex.getBindingResult()
                .getAllErrors()
                .stream()
                .map(err -> err.getObjectName() + " - " + err.getDefaultMessage())
                .reduce((a, b) -> a + "; " + b)
                .orElse("Validation error");

        return ResponseEntity
                .badRequest()
                .body(details);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<String> handleResponseStatusException(ResponseStatusException ex) {
        log.error("Response status error: {}", ex.getMessage());
        return ResponseEntity
                .status(ex.getStatusCode())
                .body(ex.getReason());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleException(Exception ex) {
        log.error("Unexpected error: {}", ex.getMessage(), ex);
        return ResponseEntity
                .status(500)
                .body("Internal server error");
    }
}