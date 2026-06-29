package ru.yandex.practicum.payment.repository;

import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.payment.entity.AccountEntity;

import java.math.BigDecimal;


public interface AccountRepository extends ReactiveCrudRepository<AccountEntity, Long> {
    @Modifying
    @Query("UPDATE accounts SET amount = amount - :amount WHERE id = :id AND amount >= :amount")
    Mono<Integer> updateBalance(Long id, BigDecimal amount);
}
