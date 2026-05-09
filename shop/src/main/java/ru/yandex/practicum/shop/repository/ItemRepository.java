package ru.yandex.practicum.shop.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.shop.entity.ItemEntity;

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
            ORDER BY
                CASE
                    WHEN :#{#pageable.sort.toString().contains('title: ASC')} THEN title
                    WHEN :#{#pageable.sort.toString().contains('price: ASC')} THEN CAST(price AS VARCHAR)
                    ELSE CAST(id AS VARCHAR)
                END
            LIMIT :#{#pageable.pageSize} OFFSET :#{#pageable.offset}
            """)
    Flux<ItemEntity> findAll(Pageable pageable);

    @Query("""
             SELECT
                 id,
                 title,
                 description,
                 img_path,
                 price
             FROM items
             WHERE title ILIKE :pattern OR description ILIKE :pattern
             ORDER BY
                CASE
                    WHEN :#{#pageable.sort.toString().contains('title: ASC')} THEN title
                    WHEN :#{#pageable.sort.toString().contains('price: ASC')} THEN price
                    ELSE id
                END
            LIMIT :#{#pageable.pageSize} OFFSET :#{#pageable.offset}
            """)
    Flux<ItemEntity> searchByTitleOrDescription(String pattern, Pageable pageable);

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
    Mono<ItemEntity> findByItemIdAndSessionId(
            @Param("itemId") long itemId, 
            @Param("sessionId") UUID sessionId);
}