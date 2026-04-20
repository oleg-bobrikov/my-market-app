package ru.yandex.practicum.mymarket.controller;

import com.github.f4b6a3.uuid.UuidCreator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.mymarket.dto.ItemDto;
import ru.yandex.practicum.mymarket.model.CartAction;
import ru.yandex.practicum.mymarket.service.CartService;

import java.util.List;
import java.util.UUID;

@Controller
@RequestMapping("/cart/items")
public class CartController {

    private final CartService cartService;

    @Autowired
    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping
    public String getCartItems(@CookieValue(value = "SESSION_ID", required = false) String sessionId,
                               Model model) {
        if (sessionId == null) {
            return "redirect:/items";
        }

        UUID sessionUuid = UuidCreator.fromString(sessionId);
        List<ItemDto> cartItems = cartService.getCartItems(sessionUuid);
        model.addAttribute("items", cartItems);
        model.addAttribute("total", cartService.getTotalPrice(cartItems));

        return "cart";
    }

    @PostMapping
    public String updateCartItem(@CookieValue(value = "SESSION_ID", required = false) String sessionId,
                                 @RequestParam Long id,
                                 @RequestParam CartAction action) {
        if (sessionId != null) {
            cartService.updateCartItem(UuidCreator.fromString(sessionId), id, action);
        }
        return "redirect:/cart/items";
    }
}
