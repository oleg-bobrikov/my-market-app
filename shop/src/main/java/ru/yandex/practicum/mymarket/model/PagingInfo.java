package ru.yandex.practicum.mymarket.model;

public record PagingInfo(int pageSize, int pageNumber, boolean hasPrevious, boolean hasNext) {
}
