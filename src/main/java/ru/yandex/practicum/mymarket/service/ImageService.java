package ru.yandex.practicum.mymarket.service;

import java.util.Optional;

public interface ImageService {
    /**
     * Возвращает данные изображения по его имени.
     *
     * @param imageName имя файла изображения (например, "1.jpg")
     * @return массив байтов изображения, если оно найдено
     */
    Optional<byte[]> getImage(String imageName);

    /**
     * Формирует URL для получения изображения.
     *
     * @param imgPath исходный путь изображения
     * @return URL изображения для отображения на странице
     */
    String getImageUrl(String imgPath);
}
