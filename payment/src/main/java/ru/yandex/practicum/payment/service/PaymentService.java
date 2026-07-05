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


@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {
    private final AccountRepository accountRepository;
    private final static BigDecimal DEFAULT_BALANCE = BigDecimal.valueOf(30_000);

    @Transactional
    public Mono<PaymentResponse> payOrder(PaymentRequest paymentRequest) {
        Long clientId = paymentRequest.getClientId();
        String orderId = paymentRequest.getOrderId();
        BigDecimal amountToPay = new BigDecimal(paymentRequest.getAmount());

        log.info("Запрос на оплату: clientId={}, orderId={}, amount={}", clientId, orderId, amountToPay);

        return getOrCreateAccount(clientId)
                .flatMap(account -> accountRepository.updateBalance(clientId, amountToPay)
                        .flatMap(rowsUpdated -> {
                            if (rowsUpdated > 0) {
                                return accountRepository.findById(clientId)
                                        .map(updatedAccount -> {
                                            log.info("Оплата успешно выполнена: accountId={}, orderId={}, newBalance={}",
                                                    clientId, orderId, updatedAccount.getAmount());
                                            PaymentResponse response = new PaymentResponse();
                                            response.setStatus(PaymentStatus.SUCCESS);
                                            response.setOrderId(orderId);
                                            response.setRemainingBalance(updatedAccount.getAmount().toString());
                                            return response;
                                        });
                            } else {
                                log.warn("Недостаточно средств для оплаты или аккаунт не найден: accountId={}, orderId={}, required={}",
                                        clientId, orderId, amountToPay);
                                return Mono.error(new InsufficientFundsException("Недостаточно средств на счете"));
                            }
                        }))
                .doOnError(e -> {
                    if (!(e instanceof InsufficientFundsException)) {
                        log.error("Ошибка при выполнении оплаты: accountId={}, orderId={}, error={}",
                                clientId, orderId, e.getMessage());
                    }
                });
    }

    public Mono<Balance> getBalance(Long accountId) {
        log.info("Запрос баланса для accountId={}", accountId);
        return getOrCreateAccount(accountId)
                .map(account -> {
                    Balance balance = new Balance();
                    balance.setClientId(account.getId());
                    balance.setBalance(account.getAmount().toString());
                    return balance;
                })
                .doOnNext(balance -> log.info("Баланс accountId={}: {}", accountId, balance.getBalance()));
    }

    private Mono<AccountEntity> getOrCreateAccount(Long accountId) {
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
