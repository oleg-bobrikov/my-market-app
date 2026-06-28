package ru.yandex.practicum.shop.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.result.view.Rendering;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.shop.mapper.ItemMapper;
import ru.yandex.practicum.shop.exception.InsufficientFundsException;
import ru.yandex.practicum.shop.exception.PaymentServiceException;
import ru.yandex.practicum.shop.security.CustomUserDetails;
import ru.yandex.practicum.shop.service.CartService;
import ru.yandex.practicum.shop.service.ItemService;
import ru.yandex.practicum.shop.service.OrderService;

import java.util.Map;


@Slf4j
@Controller
@RequiredArgsConstructor
@PreAuthorize("hasRole('USER')")
public class OrderController {
    private final OrderService orderService;
    private final ItemMapper itemMapper;
    private final CartService cartService;
    private final ItemService itemService;

    @PostMapping("/buy")
    public Mono<Rendering> buy(
            ServerWebExchange exchange,
            @AuthenticationPrincipal CustomUserDetails user) {
        Long userId = user.getUserId();
        return orderService.buy(userId)
                .map(order -> Rendering.redirectTo("/orders/" + order.getId()).build())
                .onErrorResume(InsufficientFundsException.class,
                        e -> renderCartWithError(userId, e.getMessage()))
                .onErrorResume(PaymentServiceException.class, e -> {
                    log.error("Payment service error: {}", e.getMessage());
                    return renderCartWithError(userId, "сервис платежей недоступен");
                })
                .onErrorResume(IllegalStateException.class,
                        e -> Mono.just(Rendering.redirectTo("/items").build())
                );
    }

    private Mono<Rendering> renderCartWithError(Long userId, String error) {
        return itemService.getCartItems(userId)
                .collectList()
                .flatMap(items -> cartService.getTotalPrice(items)
                        .map(total -> {
                            var itemsDto = items.stream().map(itemMapper::toDto).toList();
                            return Rendering.view("cart")
                                    .modelAttribute("items", itemsDto)
                                    .modelAttribute("total", total)
                                    .modelAttribute("paymentError", error)
                                    .build();
                        }));
    }

    @GetMapping("/orders/{id}")
    public Mono<Rendering> getOrder(
            @PathVariable Long id,
            @RequestParam(defaultValue = "false") boolean newOrder,
            @AuthenticationPrincipal CustomUserDetails user,
            ServerWebExchange exchange
    ) {

        return orderService.getOrderByIdAndSessionId(id, user.getUserId())
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
            @AuthenticationPrincipal CustomUserDetails user,
            ServerWebExchange exchange
    ) {
        return orderService.findByUserId(user.getUserId())
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
