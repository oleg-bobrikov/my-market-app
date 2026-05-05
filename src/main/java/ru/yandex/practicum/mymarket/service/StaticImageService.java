package ru.yandex.practicum.mymarket.service;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.FileNotFoundException;
import java.io.InputStream;

@Service
public class StaticImageService implements ImageService {

    private static final String IMAGES_PATH = "static/images/";

    @Override
    public Mono<byte[]> getImage(String imageName) {
        return Mono.fromCallable(() -> {
                    ClassPathResource resource = new ClassPathResource(IMAGES_PATH + imageName);

                    if (!resource.exists()) {
                        throw new FileNotFoundException(imageName);
                    }

                    try (InputStream is = resource.getInputStream()) {
                        return StreamUtils.copyToByteArray(is);
                    }
                })
                .subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    public String getImageUrl(String imgPath) {
        if (imgPath == null) {
            return null;
        }

        if (imgPath.startsWith("images/")) {
            return "api/images/" + imgPath.substring("images/".length());
        }

        return imgPath;
    }
}
