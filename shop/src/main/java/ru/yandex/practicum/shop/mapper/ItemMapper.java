package ru.yandex.practicum.shop.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.springframework.beans.factory.annotation.Autowired;
import ru.yandex.practicum.shop.dto.ItemDto;
import ru.yandex.practicum.shop.entity.ItemEntity;
import ru.yandex.practicum.shop.model.Item;
import ru.yandex.practicum.shop.service.ImageService;

@Mapper(componentModel = "spring")
public abstract class ItemMapper {
    protected ImageService imageService;

    @Autowired
    public final void setImageService(ImageService imageService) {
        this.imageService = imageService;
    }

    @Mapping(target = "imgPath", qualifiedByName = "mapImage")
    public abstract ItemDto toDto(Item item);

    public abstract Item toModel(ItemEntity entity);

    @Named("mapImage")
    protected String mapImage(String path) {
        return imageService.getImageUrl(path);
    }
}