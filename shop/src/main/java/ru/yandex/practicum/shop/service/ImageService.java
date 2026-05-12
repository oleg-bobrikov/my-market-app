package ru.yandex.practicum.shop.service;

import reactor.core.publisher.Mono;

public interface ImageService {
    /**
     * Возвращает данные изображения по его имени.
     *
     * @param imageName имя файла изображения (например, "1.jpg")
     * @return массив байтов изображения, если оно найдено
     */
    Mono<byte[]> getImage(String imageName);

    /**
     * Формирует URL для получения изображения.
     *
     * @param imgPath исходный путь изображения
     * @return URL изображения для отображения на странице
     */
    String getImageUrl(String imgPath);
}
