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
import ru.yandex.practicum.mymarket.mapper.ItemMapper;
import ru.yandex.practicum.mymarket.model.CartAction;
import ru.yandex.practicum.mymarket.model.PagingInfo;
import ru.yandex.practicum.mymarket.model.SortType;
import ru.yandex.practicum.mymarket.service.ItemService;
import ru.yandex.practicum.mymarket.service.CartService;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

@Slf4j
@Controller
@RequestMapping({"/items", "/"})
public class ItemController extends BaseController {

    private final ItemService itemService;
    private final CartService cartService;
    private final ItemMapper itemMapper;

    @Autowired
    public ItemController(ItemService itemService, CartService cartService, ItemMapper itemMapper) {
        this.itemService = itemService;
        this.cartService = cartService;
        this.itemMapper = itemMapper;
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

        UUID sessionUuid = resolveSessionId(sessionId, exchange);

        Sort sortOrder = switch (sort) {
            case ALPHA -> Sort.by("title").ascending();
            case PRICE -> Sort.by("price").ascending();
            default -> Sort.unsorted();
        };

        Pageable pageable = PageRequest.of(pageNumber - 1, pageSize, sortOrder);

        return itemService.getItems(search, sessionUuid, pageable)
                .map(itemMapper::toDto)
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
            @RequestParam(required = false) Long id,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String search,
            @RequestParam(required = false, defaultValue = "NO") String sort,
            @RequestParam(required = false, defaultValue = "5") Integer pageSize,
            @RequestParam(required = false, defaultValue = "1") Integer pageNumber,
            @CookieValue(name = "SESSION_ID", required = false) String sessionId,
            ServerWebExchange exchange
    ) {
        return exchange.getFormData().flatMap(formData -> {
            var queryParams = exchange.getRequest().getQueryParams();
            String idStr = getParam(formData, queryParams, "id");
            Long finalId = idStr != null ? Long.valueOf(idStr) : id;

            String finalAction = action != null ? action : getParam(formData, queryParams, "action");
            String finalSearch = search != null ? search : getParam(formData, queryParams, "search");

            log.debug("updateItemCount: id={}, action={}, search={}, sort={}, pageSize={}, pageNumber={}",
                    finalId, finalAction, finalSearch, sort, pageSize, pageNumber);

            if (finalId == null || finalAction == null) {
                log.warn("Missing required parameters: id={}, action={}", finalId, finalAction);
                return Mono.just("redirect:/items");
            }

            CartAction cartAction;
            try {
                cartAction = CartAction.valueOf(finalAction);
            } catch (IllegalArgumentException e) {
                log.warn("Invalid action: {}", finalAction);
                return Mono.just("redirect:/items");
            }

            UUID sessionUuid = resolveSessionId(sessionId, exchange);

            String redirectUrl = String.format(
                    "redirect:/items?search=%s&sort=%s&pageSize=%d&pageNumber=%d#item-%d",
                    finalSearch != null ? finalSearch : "",
                    sort,
                    pageSize,
                    pageNumber,
                    finalId
            );

            return cartService.updateCartItem(sessionUuid, finalId, cartAction)
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
                .map(itemMapper::toDto)
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
            @RequestParam(required = false) String action,
            @CookieValue(name = "SESSION_ID", required = false) String sessionId,
            ServerWebExchange exchange
    ) {
        log.debug("updateItemCountOnPage: id={}, action={}", id, action);

        if (action == null) {
            log.warn("Missing required parameter: action for item id={}", id);
            return Mono.just("redirect:/items/" + id);
        }

        CartAction cartAction = CartAction.valueOf(action);
        UUID sessionUuid = resolveSessionId(sessionId, exchange);

        return cartService.updateCartItem(sessionUuid, id, cartAction)
                .thenReturn("redirect:/items/" + id);
    }

}
