package ru.yandex.practicum.mymarket.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.yandex.practicum.mymarket.dto.ItemDto;
import ru.yandex.practicum.mymarket.model.Item;

@Mapper(componentModel = "spring")
public interface ItemMapper {
    @Mapping(target = "count", constant = "0")
    ItemDto toDto(Item item);
}