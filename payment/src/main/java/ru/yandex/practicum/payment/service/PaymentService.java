package ru.yandex.practicum.payment.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
public class PaymentService {
    private final AccountRepository accountRepository;
    private final static BigDecimal DEFAULT_BALANCE = BigDecimal.valueOf(30_000);
    private final Map<UUID, Mono<AccountEntity>> accountCache = new ConcurrentHashMap<>();

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public Mono<PaymentResponse> payOrder(UUID accountId, PaymentRequest paymentRequest) {
        BigDecimal amountToPay = paymentRequest.amount();
        log.info("Запрос на оплату: accountId={}, orderId={}, amount={}", accountId, paymentRequest.orderId(), amountToPay);
        return getOrCreateAccount(accountId)
                .flatMap(account -> {
                    if (account.getAmount().compareTo(amountToPay) >= 0) {
                        account.setAmount(account.getAmount().subtract(amountToPay));
                        return accountRepository.save(account)
                                .map(savedAccount -> {
                                    log.info("Оплата успешно выполнена: accountId={}, orderId={}, newBalance={}",
                                            accountId, paymentRequest.orderId(), savedAccount.getAmount());
                                    return new PaymentResponse(
                                            PaymentStatus.SUCCESS,
                                            paymentRequest.orderId(),
                                            savedAccount.getAmount()
                                    );
                                });
                    } else {
                        log.warn("Недостаточно средств для оплаты: accountId={}, orderId={}, required={}, available={}",
                                accountId, paymentRequest.orderId(), amountToPay, account.getAmount());
                        return Mono.error(new RuntimeException("Недостаточно средств на счете"));
                    }
                })
                .doOnError(e -> {
                    if (!(e instanceof RuntimeException && e.getMessage().equals("Недостаточно средств на счете"))) {
                        log.error("Ошибка при выполнении оплаты: accountId={}, orderId={}, error={}",
                                accountId, paymentRequest.orderId(), e.getMessage());
                    }
                });
    }

    public Mono<Balance> getBalance(UUID sessionId) {
        log.info("Запрос баланса для sessionId={}", sessionId);
        return accountRepository.findById(sessionId)
                .map(account -> new Balance(account.getId(), account.getAmount()))
                .switchIfEmpty(Mono.just(new Balance(sessionId, DEFAULT_BALANCE)))
                .doOnNext(balance -> log.info("Баланс для sessionId={}: {}", sessionId, balance.balance()));
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
