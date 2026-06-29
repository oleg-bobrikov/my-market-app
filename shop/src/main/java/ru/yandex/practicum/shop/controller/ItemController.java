package ru.yandex.practicum.shop.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.result.view.Rendering;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.shop.dto.ItemDto;
import ru.yandex.practicum.shop.mapper.ItemMapper;
import ru.yandex.practicum.shop.model.CartAction;
import ru.yandex.practicum.shop.model.PagingInfo;
import ru.yandex.practicum.shop.model.SortType;
import ru.yandex.practicum.shop.security.CustomUserDetails;
import ru.yandex.practicum.shop.service.ItemService;
import ru.yandex.practicum.shop.service.CartService;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

import static ru.yandex.practicum.shop.filter.SessionWebFilter.SESSION_ATTRIBUTE;

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
            @AuthenticationPrincipal CustomUserDetails user,
            ServerWebExchange exchange
    ) {

        Sort sortOrder = switch (sort) {
            case ALPHA -> Sort.by("title").ascending();
            case PRICE -> Sort.by("price").ascending();
            default -> Sort.unsorted();
        };

        Pageable pageable = PageRequest.of(pageNumber - 1, pageSize, sortOrder);
        // Anonymous users may browse the catalog (the "/" path is permitAll): there is
        // no cart association, so userId stays null and cart counts resolve to empty.
        Long userId = user != null ? user.getUserId() : null;

        return itemService.getItems(search, userId, pageable)
                .map(itemMapper::toDto)
                .collectList()
                .flatMap(content -> {
                    int chunkSize = pageSize;
                    List<List<ItemDto>> items = IntStream
                            .range(0, (content.size() + chunkSize - 1) / chunkSize)
                            .mapToObj(i -> {
                                int start = i * chunkSize;
                                int end = Math.min(start + chunkSize, content.size());
                                return (List<ItemDto>) new ArrayList<>(content.subList(start, end));
                            })
                            .toList();

                    return itemService.getItems(search, userId, PageRequest.of(pageNumber, pageSize, sortOrder))
                            .hasElements()
                            .map(hasNext -> Rendering.view("items")
                                    .modelAttribute("items", items)
                                    .modelAttribute("search", search)
                                    .modelAttribute("sort", sort)
                                    .modelAttribute("paging",
                                            new PagingInfo(
                                                    pageSize,
                                                    pageNumber,
                                                    pageNumber > 1,
                                                    hasNext
                                            )
                                    )
                                    .build());
                });
    }

    @PostMapping
    @PreAuthorize("hasRole('USER')")
    public Mono<String> updateItemCount(
            @RequestParam(required = false) Long id,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) Integer pageSize,
            @RequestParam(required = false) Integer pageNumber,
            @AuthenticationPrincipal CustomUserDetails user,
            ServerWebExchange exchange
    ) {
        return exchange.getFormData().flatMap(formData -> {
            var queryParams = exchange.getRequest().getQueryParams();
            String idStr = getParam(formData, queryParams, "id");
            Long finalId = idStr != null ? Long.valueOf(idStr) : id;

            String finalAction = action != null ? action : getParam(formData, queryParams, "action");
            String finalSearch = search != null ? search : getParam(formData, queryParams, "search");
            String finalSort = sort != null ? sort : getParam(formData, queryParams, "sort");

            String pageSizeStr = getParam(formData, queryParams, "pageSize");
            Integer finalPageSize = pageSizeStr != null ? Integer.valueOf(pageSizeStr) : pageSize;

            String pageNumberStr = getParam(formData, queryParams, "pageNumber");
            Integer finalPageNumber = pageNumberStr != null ? Integer.valueOf(pageNumberStr) : pageNumber;

            if (finalPageSize == null) finalPageSize = 5;
            if (finalPageNumber == null) finalPageNumber = 1;
            if (finalSort == null) finalSort = "NO";

            log.debug("updateItemCount: id={}, action={}, search={}, sort={}, pageSize={}, pageNumber={}",
                    finalId, finalAction, finalSearch, finalSort, finalPageSize, finalPageNumber);

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

            String redirectUrl = String.format(
                    "redirect:/items?search=%s&sort=%s&pageSize=%d&pageNumber=%d#item-%d",
                    finalSearch != null ? finalSearch : "",
                    finalSort,
                    finalPageSize,
                    finalPageNumber,
                    finalId
            );

            return cartService.updateCartItem(user.getUserId(), finalId, cartAction)
                    .thenReturn(redirectUrl);
        });
    }

    @GetMapping("/{id:[0-9]+}")
    public Mono<Rendering> getItem(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        return itemService.findByItemIdAndUserId(id, user.getUserId())
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
    @PreAuthorize("hasRole('USER')")
    public Mono<String> updateItemCountOnPage(
            @PathVariable Long id,
            @RequestParam(required = false) String action,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        log.debug("updateItemCountOnPage: id={}, action={}", id, action);

        if (action == null) {
            log.warn("Missing required parameter: action for item id={}", id);
            return Mono.just("redirect:/items/" + id);
        }

        CartAction cartAction = CartAction.valueOf(action);

        return cartService.updateCartItem(user.getUserId(), id, cartAction)
                .thenReturn("redirect:/items/" + id);
    }

}
