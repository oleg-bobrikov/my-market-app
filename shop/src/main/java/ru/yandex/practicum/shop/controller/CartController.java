package ru.yandex.practicum.shop.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.result.view.Rendering;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.shop.mapper.ItemMapper;
import ru.yandex.practicum.shop.model.CartAction;
import ru.yandex.practicum.shop.service.CartService;

import java.util.UUID;

import static ru.yandex.practicum.shop.filter.SessionWebFilter.SESSION_ATTRIBUTE;

@Slf4j
@Controller
@RequestMapping("/cart")
public class CartController extends BaseController{
    private final CartService cartService;
    private final ItemMapper itemMapper;

    @Autowired
    public CartController(CartService cartService, ItemMapper itemMapper) {
        this.cartService = cartService;
        this.itemMapper = itemMapper;
    }

    @GetMapping("/items")
    public Mono<Rendering> getCartItems(
            ServerWebExchange exchange) {

        UUID sessionUuid = exchange.getAttribute(SESSION_ATTRIBUTE);

        return cartService.getCartItems(sessionUuid)
                .collectList()
                .flatMap(items -> {
                    if (items.isEmpty()) {
                        return Mono.just(Rendering.redirectTo("/items").build());
                    }
                    return cartService.getTotalPrice(items)
                            .map(total -> {
                                var itemsDto = items.stream().map(itemMapper::toDto).toList();
                                return Rendering.view("cart")
                                        .modelAttribute("items", itemsDto)
                                        .modelAttribute("total", total)
                                        .build();
                            });
                })
                .switchIfEmpty(Mono.just(Rendering.redirectTo("/items").build()));
    }

    @PostMapping("/items")
    public Mono<String> updateCartItem(
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
            UUID sessionUuid = exchange.getAttribute(SESSION_ATTRIBUTE);

            return cartService.updateCartItem(sessionUuid, id, action)
                    .then(cartService.getCartItems(sessionUuid).collectList())
                    .map(cartItems -> cartItems.isEmpty() ? "redirect:/items" : "redirect:/cart/items");
        });
    }
}
