package ru.yandex.practicum.shop.dto;

import ru.yandex.practicum.shop.model.CartAction;

import java.time.Instant;
import java.util.UUID;

public record CartEventDto(UUID sessionId, Long itemId, CartAction action, Long version, Instant timestamp) {
}
