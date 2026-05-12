package ru.yandex.practicum.payment.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.r2dbc.DataR2dbcTest;
import org.springframework.test.context.ActiveProfiles;
import reactor.test.StepVerifier;
import ru.yandex.practicum.payment.entity.AccountEntity;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DataR2dbcTest
@ActiveProfiles("test")
public class AccountRepositoryIntegrationTest {

    @Autowired
    private AccountRepository accountRepository;

    @Test
    void updateBalance_WhenFundsSufficient_UpdatesAmount() {
        UUID accountId = UUID.randomUUID();
        AccountEntity account = AccountEntity.builder()
                .id(accountId)
                .amount(BigDecimal.valueOf(100))
                .isNew(true)
                .build();

        accountRepository.save(account)
                .then(accountRepository.updateBalance(accountId, BigDecimal.valueOf(30)))
                .as(StepVerifier::create)
                .expectNext(1) // 1 row updated
                .verifyComplete();

        accountRepository.findById(accountId)
                .as(StepVerifier::create)
                .assertNext(updated -> assertEquals(
                        0,
                        BigDecimal.valueOf(70).compareTo(updated.getAmount())))
                .verifyComplete();
    }

    @Test
    void updateBalance_WhenFundsInsufficient_DoesNotUpdate() {
        UUID accountId = UUID.randomUUID();
        AccountEntity account = AccountEntity.builder()
                .id(accountId)
                .amount(BigDecimal.valueOf(20.0))
                .isNew(true)
                .build();

        accountRepository.save(account)
                .then(accountRepository.updateBalance(accountId, BigDecimal.valueOf(30.0)))
                .as(StepVerifier::create)
                .expectNext(0) // 0 rows updated
                .verifyComplete();

        accountRepository.findById(accountId)
                .as(StepVerifier::create)
                .assertNext(result -> assertEquals(
                        0,
                        BigDecimal.valueOf(20).compareTo(result.getAmount())))
                .verifyComplete();
    }
}
