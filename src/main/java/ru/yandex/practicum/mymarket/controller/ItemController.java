package ru.yandex.practicum.mymarket.controller;

import com.github.f4b6a3.uuid.UuidCreator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import ru.yandex.practicum.mymarket.model.CartAction;
import ru.yandex.practicum.mymarket.model.Item;
import ru.yandex.practicum.mymarket.model.PagingInfo;
import ru.yandex.practicum.mymarket.model.SortType;
import ru.yandex.practicum.mymarket.service.ItemService;
import ru.yandex.practicum.mymarket.service.CartService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

@Controller
@RequestMapping({"/items", "/"})
public class ItemController {

    private final ItemService itemService;
    private final CartService cartService;

    @Autowired
    public ItemController(ItemService itemService, CartService cartService) {
        this.itemService = itemService;
        this.cartService = cartService;
    }

    @GetMapping
    public String getItems(Model model,
                           @RequestParam(required = false, defaultValue = "") String search,
                           @RequestParam(required = false, defaultValue = "NO") SortType sort,
                           @RequestParam(required = false, defaultValue = "5") int pageSize,
                           @RequestParam(required = false, defaultValue = "1") int pageNumber,
                           @CookieValue(value = "SESSION_ID", required = false) String sessionId,
                           HttpServletResponse response) {

        if (sessionId == null) {
            sessionId = UuidCreator.getTimeOrderedEpoch().toString();
            Cookie cookie = new Cookie("SESSION_ID", sessionId);
            cookie.setPath("/");
            cookie.setHttpOnly(true);
            cookie.setMaxAge(7 * 24 * 60 * 60); // 1 неделя
            response.addCookie(cookie);
        }

        Sort sortOrder = switch (sort) {
            case SortType.ALPHA -> Sort.by("title").ascending();
            case SortType.PRICE -> Sort.by("price").ascending();
            default -> Sort.unsorted();
        };
        Pageable pageable = PageRequest.of(pageNumber - 1, pageSize, sortOrder);
        Page<Item> page = itemService.getItems(search, sessionId, pageable);
        List<Item> content = page.getContent();

        int chunkSize = 5;
        List<List<Item>> items = IntStream.range(0, (content.size() + chunkSize - 1) / chunkSize)
                .mapToObj(i -> {
                    int start = i * chunkSize;
                    int end = Math.min(start + chunkSize, content.size());

                    List<Item> chunk = new ArrayList<>(content.subList(start, end));

                    // 🔹 добиваем до chunkSize элементов
                    while (chunk.size() < chunkSize) {
                        chunk.add(new Item(-1L, "", "", "", BigDecimal.ZERO, 0));
                    }

                    return chunk;
                })
                .toList();

        model.addAttribute("items", items);
        model.addAttribute("search", search);
        model.addAttribute("sort", sort);
        model.addAttribute("paging", new PagingInfo(pageable.getPageSize(), pageable.getPageNumber() + 1, page.hasPrevious(), page.hasNext()));

        return "items";
    }

    @PostMapping
    public String updateItemCount(@RequestParam Long id,
                                  @RequestParam CartAction action,
                                  @RequestParam(required = false, defaultValue = "") String search,
                                  @RequestParam(required = false, defaultValue = "NO") SortType sort,
                                  @RequestParam(required = false, defaultValue = "5") int pageSize,
                                  @RequestParam(required = false, defaultValue = "1") int pageNumber,
                                  @CookieValue(value = "SESSION_ID", required = false) String sessionId,
                                  RedirectAttributes redirectAttributes) {
        if (sessionId != null) {
            cartService.updateCartItem(UuidCreator.fromString(sessionId), id, action);
        }

        redirectAttributes.addAttribute("search", search);
        redirectAttributes.addAttribute("sort", sort);
        redirectAttributes.addAttribute("pageSize", pageSize);
        redirectAttributes.addAttribute("pageNumber", pageNumber);
        
        return "redirect:/items#item-" + id;
    }

    @GetMapping("/{id}")
    public String getItem(@PathVariable Long id,
                          @CookieValue(value = "SESSION_ID", required = false) String sessionId,
                          Model model) {
        Item item = itemService.getItemById(id, sessionId)
                .orElseGet(() -> new Item(-1L, "", "", "", BigDecimal.ZERO, 0));
        model.addAttribute("item", item);
        return "item";
    }

    @PostMapping("/{id}")
    public String updateItemCountOnPage(@PathVariable Long id,
                                        @RequestParam CartAction action,
                                        @CookieValue(value = "SESSION_ID", required = false) String sessionId) {
        if (sessionId != null) {
            cartService.updateCartItem(UuidCreator.fromString(sessionId), id, action);
        }
        return "redirect:/items/" + id;
    }

}
