package ru.yandex.practicum.mymarket.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.springframework.beans.factory.annotation.Autowired;
import ru.yandex.practicum.mymarket.dto.ItemDto;
import ru.yandex.practicum.mymarket.model.Item;
import ru.yandex.practicum.mymarket.service.ImageService;

@Mapper(componentModel = "spring")
public abstract class ItemMapper {
    protected ImageService imageService;

    @Autowired
    public final void setImageService(ImageService imageService) {
        this.imageService = imageService;
    }

    @Mapping(target = "count", constant = "0")
    @Mapping(target = "imgPath", qualifiedByName = "mapImage")
    public abstract ItemDto toDto(Item item);

    @Named("mapImage")
    protected String mapImage(String path) {
        return imageService.getImageUrl(path);
    }
}