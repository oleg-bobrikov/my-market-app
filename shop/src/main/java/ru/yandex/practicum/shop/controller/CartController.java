package ru.yandex.practicum.shop.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.result.view.Rendering;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.shop.client.PaymentClient;
import ru.yandex.practicum.shop.mapper.ItemMapper;
import ru.yandex.practicum.shop.model.CartAction;
import ru.yandex.practicum.shop.security.CustomUserDetails;
import ru.yandex.practicum.shop.service.CartService;
import ru.yandex.practicum.shop.service.ItemService;


@Slf4j
@Controller
@RequestMapping("/cart")
public class CartController extends BaseController {
    private final CartService cartService;
    private final ItemService itemService;
    private final ItemMapper itemMapper;
    private final PaymentClient paymentClient;

    @Autowired
    public CartController(CartService cartService, ItemService itemService, ItemMapper itemMapper, PaymentClient paymentClient) {
        this.cartService = cartService;
        this.itemService = itemService;
        this.itemMapper = itemMapper;
        this.paymentClient = paymentClient;
    }

    @GetMapping("/items")
    @PreAuthorize("hasRole('USER')")
    public Mono<Rendering> getCartItems(
            @AuthenticationPrincipal CustomUserDetails user,
            ServerWebExchange exchange) {

        Long userId = user.getUserId();
        if (userId == null) {
            log.error("User is not authenticated");
            return Mono.just(Rendering.redirectTo("/items").build());
        }

        return itemService.getCartItems(userId)
                .collectList()
                .flatMap(items -> {
                    if (items.isEmpty()) {
                        return Mono.just(Rendering.redirectTo("/items").build());
                    }
                    return cartService.getTotalPrice(items)
                            .flatMap(total -> paymentClient.getBalance(userId)
                                    .map(balance -> {
                                        var itemsDto = items.stream().map(itemMapper::toDto).toList();
                                        var rendering = Rendering.view("cart")
                                                .modelAttribute("items", itemsDto)
                                                .modelAttribute("total", total);

                                        if (balance.compareTo(total) < 0) {
                                            rendering.modelAttribute("paymentError", "на балансе недостаточно средств");
                                        }
                                        return rendering.build();
                                    })
                                    .onErrorResume(e -> {
                                        log.error("Payment service error for user_id {}: {} (Type: {})",
                                                userId, e.getMessage(), e.getClass().getSimpleName());
                                        var itemsDto = items.stream().map(itemMapper::toDto).toList();
                                        return Mono.just(Rendering.view("cart")
                                                .modelAttribute("items", itemsDto)
                                                .modelAttribute("total", total)
                                                .modelAttribute("paymentError", "сервис платежей недоступен")
                                                .build());
                                    })
                            );
                })
                .switchIfEmpty(Mono.just(Rendering.redirectTo("/items").build()));
    }

    @PostMapping("/items")
    @PreAuthorize("hasRole('USER')")
    public Mono<String> updateCartItem(
            @AuthenticationPrincipal CustomUserDetails user,
            ServerWebExchange exchange
    ) {
        return exchange.getFormData().flatMap(formData -> {
            var queryParams = exchange.getRequest().getQueryParams();
            String idStr = getParam(formData, queryParams, "id");
            String actionStr = getParam(formData, queryParams, "action");

            if (idStr == null || actionStr == null) {
                log.warn("Missing required parameters: id={}, action={}", idStr, actionStr);
                return Mono.just("redirect:/items");
            }

            Long id = Long.valueOf(idStr);
            CartAction action = CartAction.valueOf(actionStr);
            Long userId = user.getUserId();
            if (userId == null) {
                log.error("User is not authenticated");
                return Mono.just("redirect:/items");
            }

            return cartService.updateCartItem(userId, id, action)
                    .then(itemService.getCartItems(userId).collectList())
                    .map(cartItems -> cartItems.isEmpty() ? "redirect:/items" : "redirect:/cart/items");
        });
    }
}
