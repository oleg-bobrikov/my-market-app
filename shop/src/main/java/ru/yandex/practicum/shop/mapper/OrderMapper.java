package ru.yandex.practicum.shop.mapper;

import org.mapstruct.Mapper;
import ru.yandex.practicum.shop.entity.OrderEntity;
import ru.yandex.practicum.shop.entity.OrderItemEntity;
import ru.yandex.practicum.shop.model.Order;
import ru.yandex.practicum.shop.model.OrderItem;

@Mapper(componentModel = "spring")
public interface OrderMapper {
    Order toModel(OrderEntity entity);
    OrderEntity toEntity(Order model);
    
    OrderItem toModel(OrderItemEntity entity);
    OrderItemEntity toEntity(OrderItem model);
}
