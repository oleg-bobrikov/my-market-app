package ru.yandex.practicum.shop.model;

import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order {
    private Long id;
    private UUID sessionId;
    private BigDecimal total;
}
