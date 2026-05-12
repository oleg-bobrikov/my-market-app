package ru.yandex.practicum.shop.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("order_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItemEntity {
    @Id
    private Long id;

    @Column("order_id")
    private Long orderId;

    @Column("item_id")
    private Long itemId;

    @Column("count")
    private Integer count;
}
