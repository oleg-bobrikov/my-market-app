package ru.yandex.practicum.shop.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import ru.yandex.practicum.shop.entity.ItemEntity;

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

}