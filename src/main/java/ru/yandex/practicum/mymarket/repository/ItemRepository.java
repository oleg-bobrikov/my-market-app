package ru.yandex.practicum.mymarket.repository;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.mymarket.entity.ItemEntity;
import ru.yandex.practicum.mymarket.model.Item;

import java.util.UUID;

@Repository
public interface ItemRepository extends ReactiveCrudRepository<ItemEntity, Long> {
    @Query("""
            SELECT
                id,
                title,
                description,
                img_path,
                price
            FROM items
            ORDER BY id
            LIMIT :limit OFFSET :offset
            """)
    Flux<ItemEntity> findAll(int limit, long offset);

    @Query("""
            SELECT
                id,
                title,
                description,
                img_path,
                price
            FROM items
            ORDER BY title
            LIMIT :limit OFFSET :offset
            """)
    Flux<ItemEntity> findAllOrderedByTitle(int limit, long offset);

    @Query("""
            SELECT
                id,
                title,
                description,
                img_path,
                price
            FROM items
            ORDER BY price
            LIMIT :limit OFFSET :offset
            """)
    Flux<ItemEntity> findAllOrderedByPrice(int limit, long offset);

    @Query("""
             SELECT
                 id,
                 title,
                 description,
                 img_path,
                 price
             FROM items
             WHERE title ILIKE :pattern OR description ILIKE :pattern
             LIMIT :limit OFFSET :offset
            """)
    Flux<ItemEntity> searchByTitleOrDescription(String pattern, int limit, long offset);

    @Query("""
            SELECT
                items.id,
                items.title,
                items.description,
                items.img_path,
                items.price,
                carts.count
            FROM items
            INNER JOIN carts
                ON carts.item_id = items.id
            WHERE carts.session_id = :sessionId
            ORDER BY carts.created_at
            """)
    Flux<ItemEntity> findBySessionId(UUID sessionId);

    @Query("""
            SELECT
                items.id,
                items.title,
                items.description,
                items.img_path,
                items.price,
                carts.count
            FROM items
            LEFT JOIN carts
                ON carts.item_id = items.id AND carts.session_id = :sessionId
            WHERE items.id = :itemId
            LIMIT 1
            """)
    Mono<ItemEntity> findByItemIdAndSessionId(@org.springframework.data.repository.query.Param("itemId") long itemId, @org.springframework.data.repository.query.Param("sessionId") java.util.UUID sessionId);
}