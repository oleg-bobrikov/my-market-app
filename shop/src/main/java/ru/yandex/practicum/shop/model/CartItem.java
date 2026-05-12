package ru.yandex.practicum.shop.model;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CartItem {
    private Long id;
    private UUID sessionId;
    private Long itemId;
    private Integer count;
}
