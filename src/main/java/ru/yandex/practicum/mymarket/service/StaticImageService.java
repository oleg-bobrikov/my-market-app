package ru.yandex.practicum.mymarket.service;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

@Service
public class StaticImageService implements ImageService {

    private static final String IMAGES_PATH = "static/images/";

    @Override
    public Optional<byte[]> getImage(String imageName) {
        try {
            ClassPathResource resource = new ClassPathResource(IMAGES_PATH + imageName);
            if (!resource.exists()) {
                return Optional.empty();
            }
            try (InputStream is = resource.getInputStream()) {
                return Optional.of(StreamUtils.copyToByteArray(is));
            }
        } catch (IOException e) {
            return Optional.empty();
        }
    }

    @Override
    public String getImageUrl(String imgPath) {
        if (imgPath == null) {
            return null;
        }
        // Если путь начинается с images/, заменяем на /api/images/
        if (imgPath.startsWith("images/")) {
            return "/api/images/" + imgPath.substring("images/".length());
        }
        return imgPath;
    }
}
