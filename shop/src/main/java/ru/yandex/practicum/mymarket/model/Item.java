package ru.yandex.practicum.mymarket.model;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Item {
    private Long id;
    private String title;
    private String description;
    private String imgPath;
    private BigDecimal price;
    private Integer count;
}
