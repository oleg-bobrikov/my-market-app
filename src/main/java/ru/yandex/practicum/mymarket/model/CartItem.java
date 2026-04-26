package ru.yandex.practicum.mymarket.model;

import lombok.*;

import java.time.LocalDateTime;
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
    private LocalDateTime createdAt;
}
