package ru.yandex.practicum.payment.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.payment.entity.AccountEntity;
import ru.yandex.practicum.payment.model.*;
import ru.yandex.practicum.payment.repository.AccountRepository;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class PaymentService {
    private final AccountRepository accountRepository;
    private final static BigDecimal DEFAULT_BALANCE = BigDecimal.valueOf(30_000);
    private final Map<UUID, Mono<AccountEntity>> accountCache = new ConcurrentHashMap<>();

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public Mono<PaymentResponse> payOrder(UUID accountId, PaymentRequest paymentRequest) {
        BigDecimal amountToPay = paymentRequest.amount();
        return getOrCreateAccount(accountId)
                .flatMap(account -> {
                    if (account.getAmount().compareTo(amountToPay) >= 0) {
                        account.setAmount(account.getAmount().subtract(amountToPay));
                        return accountRepository.save(account)
                                .map(savedAccount -> new PaymentResponse(
                                        PaymentStatus.SUCCESS,
                                        paymentRequest.orderId(),
                                        savedAccount.getAmount()
                                ));
                    } else {
                        return Mono.error(new RuntimeException("Недостаточно средств на счете"));
                    }
                });
    }

    public Mono<Balance> getBalance(UUID sessionId) {
        return accountRepository.findById(sessionId)
                .map(account -> new Balance(account.getId(), account.getAmount()))
                .switchIfEmpty(Mono.just(new Balance(sessionId, DEFAULT_BALANCE)));
    }

    private Mono<AccountEntity> getOrCreateAccount(UUID accountId) {
        return accountRepository.findById(accountId)
                .switchIfEmpty(Mono.defer(() -> accountCache.computeIfAbsent(accountId, id ->
                        accountRepository.save(new AccountEntity(id, DEFAULT_BALANCE))
                                .cache()
                                .doFinally(signalType -> accountCache.remove(id))
                )));
    }
}
