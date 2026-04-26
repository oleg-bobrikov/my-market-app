package ru.yandex.practicum.mymarket.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.result.view.Rendering;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.mymarket.service.OrderService;

import java.util.Map;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class OrderController {
    private final OrderService orderService;

    @PostMapping("/buy")
    public Mono<Rendering> buy(@CookieValue(value = "SESSION_ID", required = false) String sessionId) {
        if (sessionId == null) {
            return Mono.just(Rendering.redirectTo("/items").build());
        }
        UUID sessionUuid = UUID.fromString(sessionId);

        return orderService.createOrder(sessionUuid)
                .map(order -> Rendering.redirectTo("/orders/" + order.getId()).build());
    }

    @GetMapping("/orders/{id}")
    public Mono<Rendering> getOrder(
            @PathVariable Long id,
            @RequestParam(defaultValue = "false") boolean newOrder,
            @CookieValue(value = "SESSION_ID", required = false) String sessionId
    ) {
        if (sessionId == null) {
            return Mono.just(Rendering.redirectTo("/items").build());
        }
        UUID sessionUuid = UUID.fromString(sessionId);

        return orderService.getOrderByIdAndSessionId(id, sessionUuid)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Order not found")))
                .zipWhen(order -> orderService.getOrderItemsDto(id).collectList())
                .map(tuple -> {
                    var order = tuple.getT1();
                    var items = tuple.getT2();

                    return Rendering.view("order")
                            .modelAttribute("order", Map.of(
                                    "id", order.getId(),
                                    "items", items,
                                    "totalSum", order.getTotal()
                            ))
                            .modelAttribute("newOrder", newOrder)
                            .build();
                });
    }

    @GetMapping("/orders")
    public Mono<Rendering> findBySessionId(
            @CookieValue(value = "SESSION_ID", required = false) String sessionId
    ) {
        if (sessionId == null) {
            return Mono.just(Rendering.redirectTo("/items").build());
        }
        UUID sessionUuid = UUID.fromString(sessionId);

        return orderService.findBySessionId(sessionUuid)
                .flatMap(order ->
                        orderService.getOrderItemsDto(order.getId())
                                .collectList()
                                .map(items -> Map.of(
                                        "id", order.getId(),
                                        "items", items,
                                        "totalSum", order.getTotal()
                                ))
                )
                .collectList()
                .map(orders ->
                        Rendering.view("orders")
                                .modelAttribute("orders", orders)
                                .build()
                );
    }
}
