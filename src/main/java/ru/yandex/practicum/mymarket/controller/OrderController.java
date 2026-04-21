package ru.yandex.practicum.mymarket.controller;

import com.github.f4b6a3.uuid.UuidCreator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import ru.yandex.practicum.mymarket.model.Order;
import ru.yandex.practicum.mymarket.service.OrderService;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
public class OrderController {
    private final OrderService orderService;

    @PostMapping("/buy")
    public String buy(@CookieValue(value = "SESSION_ID", required = false) String sessionId,
                      RedirectAttributes redirectAttributes) {
        if (sessionId == null) {
            return "redirect:/items";
        }
        UUID sessionUuid = UuidCreator.fromString(sessionId);
        Long orderId = orderService.createOrder(sessionUuid);
        redirectAttributes.addAttribute("newOrder", true);
        return "redirect:/orders/" + orderId;
    }

    @GetMapping("/orders/{id}")
    public String getOrder(@PathVariable Long id,
                           @RequestParam(defaultValue = "false") boolean newOrder,
                           Model model) {
        Order order = orderService.getOrderById(id)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));

        model.addAttribute("order", Map.of(
                "id", order.getId(),
                "items", orderService.getOrderItemsDto(order),
                "totalSum", order.getTotal()
        ));
        model.addAttribute("newOrder", newOrder);

        return "order";
    }

    @GetMapping("/orders")
    public String getAllOrders(Model model) {
        List<Order> orders = orderService.getAllOrders();
        model.addAttribute("orders", orders.stream()
                .map(order -> Map.of(
                        "id", order.getId(),
                        "items", orderService.getOrderItemsDto(order),
                        "totalSum", order.getTotal()
                ))
                .collect(Collectors.toList()));
        return "orders";
    }
}
