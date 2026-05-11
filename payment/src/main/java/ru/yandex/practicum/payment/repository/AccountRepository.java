package ru.yandex.practicum.payment.repository;

import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.payment.entity.AccountEntity;

import java.math.BigDecimal;
import java.util.UUID;

public interface AccountRepository extends ReactiveCrudRepository<AccountEntity, UUID> {
    @Modifying
    @Query("UPDATE accounts SET amount = amount - :amount WHERE account_id = :id AND amount >= :amount")
    Mono<Integer> updateBalance(UUID id, BigDecimal amount);
}
