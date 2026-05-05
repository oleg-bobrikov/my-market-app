package ru.yandex.practicum.mymarket.model;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItem {
    private Long id;
    private Long orderId;
    private Long itemId;
    private Integer count;
}
