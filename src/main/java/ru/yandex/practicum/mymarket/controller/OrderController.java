package ru.yandex.practicum.mymarket.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.result.view.Rendering;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.mymarket.mapper.ItemMapper;
import ru.yandex.practicum.mymarket.service.OrderService;

import java.util.Map;
import java.util.UUID;

import static ru.yandex.practicum.mymarket.filter.SessionWebFilter.SESSION_ATTRIBUTE;

@Controller
@RequiredArgsConstructor
public class OrderController {
    private final OrderService orderService;
    private final ItemMapper itemMapper;

    @PostMapping("/buy")
    public Mono<Rendering> buy(ServerWebExchange exchange) {
        UUID sessionUuid = exchange.getAttribute(SESSION_ATTRIBUTE);

        return orderService.createOrder(sessionUuid)
                .map(order -> Rendering.redirectTo("/orders/" + order.getId()).build())
                .onErrorResume(IllegalStateException.class, e -> Mono.just(Rendering.redirectTo("/items").build()));
    }

    @GetMapping("/orders/{id}")
    public Mono<Rendering> getOrder(
            @PathVariable Long id,
            @RequestParam(defaultValue = "false") boolean newOrder,
            ServerWebExchange exchange
    ) {
        UUID sessionUuid = exchange.getAttribute(SESSION_ATTRIBUTE);

        return orderService.getOrderByIdAndSessionId(id, sessionUuid)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Order not found")))
                .zipWhen(order -> orderService.getOrderItems(id).map(itemMapper::toDto).collectList())
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
            ServerWebExchange exchange
    ) {
        UUID sessionUuid = exchange.getAttribute(SESSION_ATTRIBUTE);

        return orderService.findBySessionId(sessionUuid)
                .flatMap(order ->
                        orderService.getOrderItems(order.getId())
                                .map(itemMapper::toDto)
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
