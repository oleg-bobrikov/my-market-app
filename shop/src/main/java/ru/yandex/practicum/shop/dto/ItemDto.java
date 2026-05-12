package ru.yandex.practicum.shop.dto;

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
    
    public Integer getCount() {
        return count == null ? 0 : count;
    }

    public BigDecimal getPrice() {
        return price == null ? BigDecimal.ZERO : price;
    }
}
