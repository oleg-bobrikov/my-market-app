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
                .amount(new BigDecimal("100.00"))
                .isNew(true)
                .build();

        accountRepository.save(account)
                .then(accountRepository.updateBalance(accountId, new BigDecimal("30.00")))
                .as(StepVerifier::create)
                .expectNext(1) // 1 row updated
                .verifyComplete();

        accountRepository.findById(accountId)
                .as(StepVerifier::create)
                .assertNext(updated -> {
                    assertEquals(new BigDecimal("70.00"), updated.getAmount().setScale(2));
                })
                .verifyComplete();
    }

    @Test
    void updateBalance_WhenFundsInsufficient_DoesNotUpdate() {
        UUID accountId = UUID.randomUUID();
        AccountEntity account = AccountEntity.builder()
                .id(accountId)
                .amount(new BigDecimal("20.00"))
                .isNew(true)
                .build();

        accountRepository.save(account)
                .then(accountRepository.updateBalance(accountId, new BigDecimal("30.00")))
                .as(StepVerifier::create)
                .expectNext(0) // 0 rows updated
                .verifyComplete();

        accountRepository.findById(accountId)
                .as(StepVerifier::create)
                .assertNext(result -> {
                    assertEquals(new BigDecimal("20.00"), result.getAmount().setScale(2));
                })
                .verifyComplete();
    }
}
