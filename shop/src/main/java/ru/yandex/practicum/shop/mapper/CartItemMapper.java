package ru.yandex.practicum.shop.mapper;

import org.mapstruct.Mapper;
import ru.yandex.practicum.shop.entity.CartItemEntity;
import ru.yandex.practicum.shop.model.CartItem;

@Mapper(componentModel = "spring")
public interface CartItemMapper {
    CartItem toModel(CartItemEntity entity);
    CartItemEntity toEntity(CartItem model);
}
