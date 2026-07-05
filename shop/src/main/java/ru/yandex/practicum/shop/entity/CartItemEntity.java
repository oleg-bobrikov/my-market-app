package ru.yandex.practicum.shop.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table(name = "carts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CartItemEntity {
    @Id
    private Long id;

    @Column("user_id")
    private Long userId;

    @Column("item_id")
    private Long itemId;

    @Column("count")
    private Integer count;

    @Version
    private Long version;
}
