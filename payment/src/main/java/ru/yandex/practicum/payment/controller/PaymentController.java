package ru.yandex.practicum.payment.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.payment.model.*;
import ru.yandex.practicum.payment.service.PaymentService;

import java.util.UUID;

@RestController
@RequestMapping("/payments/api")
@Tag(name = "Payment Service API", description = "API для управления платежами")
@RequiredArgsConstructor
public class PaymentController {
    private final PaymentService paymentService;
    @PostMapping
    @Operation(
            summary = "Выполнить оплату заказа",
            description = "Списание суммы заказа с баланса покупателя",
            security = @SecurityRequirement(name = "SessionAuth"),
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Платеж успешно выполнен",
                            content = @Content(schema = @Schema(implementation = PaymentResponse.class))
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Недостаточно средств",
                            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
                    )
            }
    )
    public Mono<ResponseEntity<Object>> payOrder(
            @RequestBody PaymentRequest paymentRequest,
            @RequestHeader("session_id") String sessionId
    ) {
        return paymentService.payOrder(UUID.fromString(sessionId), paymentRequest)
                .map(response -> ResponseEntity.ok((Object) response))
                .onErrorResume(e -> Mono.just(ResponseEntity.badRequest()
                        .body(new ErrorResponse(PaymentStatus.ERROR, e.getMessage()))));
    }

    @GetMapping("/balance")
    @Operation(
            summary = "Получить информацию о балансе счета",
            security = @SecurityRequirement(name = "SessionAuth"),
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Успешный ответ",
                            content = @Content(schema = @Schema(implementation = Balance.class))
                    )
            }
    )
    public Mono<ResponseEntity<Balance>> getBalance(
            @RequestHeader("session_id") String sessionId
    ) {
        return paymentService.getBalance(UUID.fromString(sessionId))
                .map(ResponseEntity::ok);
    }
}
