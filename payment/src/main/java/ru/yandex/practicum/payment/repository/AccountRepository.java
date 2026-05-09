package ru.yandex.practicum.payment.repository;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import ru.yandex.practicum.payment.entity.AccountEntity;
import java.util.UUID;

public interface AccountRepository extends ReactiveCrudRepository<AccountEntity, UUID> {
}
