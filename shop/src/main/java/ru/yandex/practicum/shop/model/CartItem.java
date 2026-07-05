package ru.yandex.practicum.shop.model;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CartItem {
    private Long id;
    private Long userId;
    private Long itemId;
    private Integer count;
}
