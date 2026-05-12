package ru.yandex.practicum.payment.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.payment.entity.AccountEntity;
import ru.yandex.practicum.payment.exception.InsufficientFundsException;
import ru.yandex.practicum.payment.model.*;
import ru.yandex.practicum.payment.repository.AccountRepository;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {
    private final AccountRepository accountRepository;
    private final static BigDecimal DEFAULT_BALANCE = BigDecimal.valueOf(30_000);

    @Transactional
    public Mono<PaymentResponse> payOrder(UUID accountId, PaymentRequest paymentRequest) {
        if (paymentRequest.getAmount() == null) {
            return Mono.error(new IllegalArgumentException("Amount must not be null"));
        }
        BigDecimal amountToPay = new BigDecimal(paymentRequest.getAmount());
        log.info("Запрос на оплату: accountId={}, orderId={}, amount={}", accountId, paymentRequest.getOrderId(), amountToPay);
        
        return getOrCreateAccount(accountId)
                .flatMap(account -> accountRepository.updateBalance(accountId, amountToPay)
                        .flatMap(rowsUpdated -> {
                            if (rowsUpdated > 0) {
                                return accountRepository.findById(accountId)
                                        .map(updatedAccount -> {
                                            log.info("Оплата успешно выполнена: accountId={}, orderId={}, newBalance={}",
                                                    accountId, paymentRequest.getOrderId(), updatedAccount.getAmount());
                                            PaymentResponse response = new PaymentResponse();
                                            response.setStatus(PaymentStatus.SUCCESS);
                                            response.setOrderId(paymentRequest.getOrderId());
                                            response.setRemainingBalance(updatedAccount.getAmount().toString());
                                            return response;
                                        });
                            } else {
                                log.warn("Недостаточно средств для оплаты или аккаунт не найден: accountId={}, orderId={}, required={}",
                                        accountId, paymentRequest.getOrderId(), amountToPay);
                                return Mono.error(new InsufficientFundsException("Недостаточно средств на счете"));
                            }
                        }))
                .doOnError(e -> {
                    if (!(e instanceof InsufficientFundsException)) {
                        log.error("Ошибка при выполнении оплаты: accountId={}, orderId={}, error={}",
                                accountId, paymentRequest.getOrderId(), e.getMessage());
                    }
                });
    }

    public Mono<Balance> getBalance(UUID sessionId) {
        log.info("Запрос баланса для sessionId={}", sessionId);
        return getOrCreateAccount(sessionId)
                .map(account -> {
                    Balance balance = new Balance();
                    balance.setClientId(account.getId());
                    balance.setBalance(account.getAmount().toString());
                    return balance;
                })
                .doOnNext(balance -> log.info("Баланс для sessionId={}: {}", sessionId, balance.getBalance()));
    }

    private Mono<AccountEntity> getOrCreateAccount(UUID accountId) {
        return accountRepository.findById(accountId)
                .switchIfEmpty(Mono.defer(() -> {
                    log.info("Создание нового аккаунта: accountId={}", accountId);
                    AccountEntity newAccount = AccountEntity.builder()
                            .id(accountId)
                            .amount(DEFAULT_BALANCE)
                            .isNew(true)
                            .build();
                    return accountRepository.save(newAccount)
                            .onErrorResume(e -> {
                                log.debug("Ошибка при сохранении нового аккаунта (возможно, уже создан): {}", e.getMessage());
                                return accountRepository.findById(accountId);
                            })
                            .map(account -> {
                                account.setNew(false);
                                return account;
                            });
                }));
    }
}
