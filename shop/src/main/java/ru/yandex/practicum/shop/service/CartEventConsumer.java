package ru.yandex.practicum.shop.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.redis.connection.stream.*;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;
import ru.yandex.practicum.shop.dto.CartEventDto;
import ru.yandex.practicum.shop.entity.CartItemEntity;
import ru.yandex.practicum.shop.repository.CartRepository;

import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class CartEventConsumer {
    private static final String STREAM = "cart-events";

    private final ReactiveRedisTemplate<String, String> redisTemplate;
    private final CartRepository cartRepository;

    @EventListener(ApplicationReadyEvent.class)
    public void consume() {
        redisTemplate.<String, CartEventDto>opsForStream()
                .read(
                        CartEventDto.class,
                        StreamOffset.create(STREAM, ReadOffset.lastConsumed())
                )
                .flatMap(this::processEvent)
                .retryWhen(
                        Retry.backoff(Long.MAX_VALUE, Duration.ofSeconds(1))
                )
                .subscribe();
    }

    private Mono<Void> processEvent(ObjectRecord<String, CartEventDto> record) {
        CartEventDto event = record.getValue();

        return switch (event.action()) {
            case PLUS -> plus(event);
            case MINUS -> minus(event);
            case DELETE -> delete(event);
        };
    }

    private Mono<Void> plus(CartEventDto event) {
        return cartRepository
                .findBySessionIdAndItemId(
                        event.sessionId(),
                        event.itemId()
                )
                .flatMap(entity -> {
                    if (isOldVersion(event, entity)) {
                        return Mono.just(entity);
                    }
                    entity.setCount(entity.getCount() + 1);

                    return cartRepository.save(entity);
                })
                .switchIfEmpty(
                        cartRepository.save(
                                CartItemEntity.builder()
                                        .sessionId(event.sessionId())
                                        .itemId(event.itemId())
                                        .count(1)
                                        .build()
                        )
                )
                .retryWhen(
                        Retry.backoff(100, Duration.ofMillis(200))
                                .filter(e ->
                                        e instanceof OptimisticLockingFailureException
                                )
                )
                .then();
    }

    private Mono<Void> minus(CartEventDto event) {
        return cartRepository
                .findBySessionIdAndItemId(
                        event.sessionId(),
                        event.itemId()
                )
                .flatMap(entity -> {
                    if (isOldVersion(event, entity)) {
                        return Mono.just(entity);
                    }

                    if (entity.getCount() <= 1) {
                        return cartRepository.delete(entity).then(Mono.empty());
                    }

                    entity.setCount(entity.getCount() - 1);

                    return cartRepository.save(entity);
                })
                .retryWhen(
                        Retry.backoff(100, Duration.ofMillis(200))
                                .filter(e ->
                                        e instanceof OptimisticLockingFailureException
                                )
                )
                .then();
    }

    private Mono<Void> delete(CartEventDto event) {
        return cartRepository
                .findBySessionIdAndItemId(
                        event.sessionId(),
                        event.itemId()
                )
                .flatMap(entity -> {
                    if (isOldVersion(event, entity)) {
                        return Mono.empty();
                    }
                    return cartRepository.delete(entity);
                })
                .retryWhen(
                        Retry.backoff(100, Duration.ofMillis(200))
                                .filter(e ->
                                        e instanceof OptimisticLockingFailureException
                                )
                )
                .then();
    }

    private boolean isOldVersion(CartEventDto event, CartItemEntity entity) {
        if (event.version() != null
                && entity.getVersion() != null
                && event.version() <= entity.getVersion()) {
            log.debug("Skipping event with old version. Event: {}, Entity: {}", event.version(), entity.getVersion());
            return true;
        }
        return false;
    }
}
