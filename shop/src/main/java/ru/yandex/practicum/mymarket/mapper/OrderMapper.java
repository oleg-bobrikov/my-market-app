package ru.yandex.practicum.mymarket.mapper;

import org.mapstruct.Mapper;
import ru.yandex.practicum.mymarket.entity.OrderEntity;
import ru.yandex.practicum.mymarket.entity.OrderItemEntity;
import ru.yandex.practicum.mymarket.model.Order;
import ru.yandex.practicum.mymarket.model.OrderItem;

@Mapper(componentModel = "spring")
public interface OrderMapper {
    Order toModel(OrderEntity entity);
    OrderEntity toEntity(Order model);
    
    OrderItem toModel(OrderItemEntity entity);
    OrderItemEntity toEntity(OrderItem model);
}
