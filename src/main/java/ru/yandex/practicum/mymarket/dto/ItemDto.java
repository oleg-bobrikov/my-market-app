package ru.yandex.practicum.mymarket.dto;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ItemDto {
    private Long id;
    private String title;
    private String description;
    private String imgPath;
    private BigDecimal price;
    private Integer count;
}
