package ru.yandex.practicum.mymarket.mapper;

import org.mapstruct.Mapper;
import ru.yandex.practicum.mymarket.entity.CartItemEntity;
import ru.yandex.practicum.mymarket.model.CartItem;

@Mapper(componentModel = "spring")
public interface CartItemMapper {
    CartItem toModel(CartItemEntity entity);
    CartItemEntity toEntity(CartItem model);
}
