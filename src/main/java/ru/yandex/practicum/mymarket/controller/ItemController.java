package ru.yandex.practicum.mymarket.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.result.view.Rendering;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.mymarket.dto.ItemDto;
import ru.yandex.practicum.mymarket.model.CartAction;
import ru.yandex.practicum.mymarket.model.PagingInfo;
import ru.yandex.practicum.mymarket.model.SortType;
import ru.yandex.practicum.mymarket.service.ItemService;
import ru.yandex.practicum.mymarket.service.CartService;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.IntStream;

@Slf4j
@Controller
@RequestMapping({"/items", "/"})
public class ItemController extends BaseController{

    private final ItemService itemService;
    private final CartService cartService;

    @Autowired
    public ItemController(ItemService itemService, CartService cartService) {
        this.itemService = itemService;
        this.cartService = cartService;
    }



    @GetMapping
    public Mono<Rendering> getItems(
            @RequestParam(required = false, defaultValue = "") String search,
            @RequestParam(required = false, defaultValue = "NO") SortType sort,
            @RequestParam(required = false, defaultValue = "5") int pageSize,
            @RequestParam(required = false, defaultValue = "1") int pageNumber,
            @CookieValue(value = "SESSION_ID", required = false) String sessionId,
            ServerWebExchange exchange
    ) {

        // 1. Cookie
        UUID sessionUuid = resolveSessionId(sessionId, exchange);

        // 2. Pageable
        Sort sortOrder = switch (sort) {
            case ALPHA -> Sort.by("title").ascending();
            case PRICE -> Sort.by("price").ascending();
            default -> Sort.unsorted();
        };

        Pageable pageable = PageRequest.of(pageNumber - 1, pageSize, sortOrder);

        // 3. Реактивная цепочка
        return itemService.getItems(search, sessionUuid, pageable)
                .collectList()
                .map(content -> {
                    int chunkSize = 5;
                    List<List<ItemDto>> items = IntStream
                            .range(0, (content.size() + chunkSize - 1) / chunkSize)
                            .mapToObj(i -> {
                                int start = i * chunkSize;
                                int end = Math.min(start + chunkSize, content.size());
                                List<ItemDto> chunk = new ArrayList<>(content.subList(start, end));

                                while (chunk.size() < chunkSize) {
                                    chunk.add(new ItemDto(-1L, "", "", "", BigDecimal.ZERO, 0));
                                }

                                return chunk;
                            })
                            .toList();

                    return Rendering.view("items")
                            .modelAttribute("items", items)
                            .modelAttribute("search", search)
                            .modelAttribute("sort", sort)
                            .modelAttribute("paging",
                                    new PagingInfo(
                                            pageSize,
                                            pageNumber,
                                            pageNumber > 1,
                                            content.size() == pageSize // примитивная hasNext
                                    )
                            )
                            .build();
                });
    }

    @PostMapping
    public Mono<String> updateItemCount(
            ServerWebExchange exchange
    ) {
        return exchange.getFormData().flatMap(formData -> {
            var queryParams = exchange.getRequest().getQueryParams();

            String idStr = getParam(formData, queryParams, "id");
            String actionStr = getParam(formData, queryParams, "action");
            String search = getParam(formData, queryParams, "search");
            String sort = getParam(formData, queryParams, "sort");
            String pageSizeStr = getParam(formData, queryParams, "pageSize");
            String pageNumberStr = getParam(formData, queryParams, "pageNumber");

            log.debug("updateItemCount: id={}, action={}, search={}, sort={}, pageSize={}, pageNumber={}",
                    idStr, actionStr, search, sort, pageSizeStr, pageNumberStr);

            if (idStr == null || actionStr == null) {
                log.warn("Missing required parameters: id={}, action={}", idStr, actionStr);
                return Mono.just("redirect:/items");
            }

            Long id = Long.valueOf(idStr);
            CartAction action = CartAction.valueOf(actionStr);
            int pageSize = pageSizeStr != null ? Integer.parseInt(pageSizeStr) : 5;
            int pageNumber = pageNumberStr != null ? Integer.parseInt(pageNumberStr) : 1;
            
            String sessionId = exchange.getRequest().getCookies().getFirst("SESSION_ID") != null ? 
                    Objects.requireNonNull(exchange.getRequest().getCookies().getFirst("SESSION_ID")).getValue() : null;
            UUID sessionUuid = resolveSessionId(sessionId, exchange);

            String redirectUrl = String.format(
                    "redirect:/items?search=%s&sort=%s&pageSize=%d&pageNumber=%d#item-%d",
                    search != null ? search : "", 
                    sort != null ? sort : "NO", 
                    pageSize, 
                    pageNumber, 
                    id
            );

            return cartService.updateCartItem(sessionUuid, id, action)
                    .thenReturn(redirectUrl);
        });
    }

    @GetMapping("/{id:[0-9]+}")
    public Mono<Rendering> getItem(
            @PathVariable Long id,
            @CookieValue(value = "SESSION_ID", required = false) String sessionId
    ) {
        if (sessionId == null) {
            return Mono.just(
                    Rendering.redirectTo("/items").build()
            );
        }
        UUID sessionUuid = UUID.fromString(sessionId);

        return itemService.findByItemIdAndSessionId(id, sessionUuid)
                .defaultIfEmpty(emptyItem())
                .map(item -> Rendering.view("item")
                        .modelAttribute("item", item)
                        .build()
                );
    }

    private ItemDto emptyItem() {
        return new ItemDto(-1L, "", "", "", BigDecimal.ZERO, 0);
    }

    @PostMapping("/{id:[0-9]+}")
    public Mono<String> updateItemCountOnPage(
            @PathVariable Long id,
            ServerWebExchange exchange
    ) {
        return exchange.getFormData().flatMap(formData -> {
            var queryParams = exchange.getRequest().getQueryParams();
            String actionStr = getParam(formData, queryParams, "action");

            log.debug("updateItemCountOnPage: id={}, action={}", id, actionStr);
            if (actionStr == null) {
                log.warn("Missing required parameter: action for item id={}", id);
                return Mono.just("redirect:/items/" + id);
            }

            CartAction action = CartAction.valueOf(actionStr);
            String sessionId = exchange.getRequest().getCookies().getFirst("SESSION_ID") != null ? 
                    Objects.requireNonNull(exchange.getRequest().getCookies().getFirst("SESSION_ID")).getValue() : null;
            UUID sessionUuid = resolveSessionId(sessionId, exchange);

            return cartService.updateCartItem(sessionUuid, id, action)
                    .thenReturn("redirect:/items/" + id);
        });
    }

}
