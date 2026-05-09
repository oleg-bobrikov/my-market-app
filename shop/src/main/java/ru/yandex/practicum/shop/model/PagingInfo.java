package ru.yandex.practicum.shop.model;

public record PagingInfo(int pageSize, int pageNumber, boolean hasPrevious, boolean hasNext) {
}
