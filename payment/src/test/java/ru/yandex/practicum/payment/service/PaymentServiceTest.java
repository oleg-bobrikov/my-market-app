package ru.yandex.practicum.payment.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import ru.yandex.practicum.payment.entity.AccountEntity;
import ru.yandex.practicum.payment.model.Balance;
import ru.yandex.practicum.payment.model.PaymentRequest;
import ru.yandex.practicum.payment.model.PaymentResponse;
import ru.yandex.practicum.payment.model.PaymentStatus;
import ru.yandex.practicum.payment.repository.AccountRepository;

import java.math.BigDecimal;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @InjectMocks
    private PaymentService paymentService;

    private UUID sessionId;

    @BeforeEach
    void setUp() {
        sessionId = UUID.randomUUID();
    }

    @Test
    void payOrder_WhenAccountDoesNotExist_CreatesAccountWithDefaultBalance() {
        BigDecimal amountToPay = new BigDecimal("1000");
        PaymentRequest paymentRequest = new PaymentRequest("order-1", amountToPay);
        BigDecimal defaultBalance = new BigDecimal(30_000);

        when(accountRepository.findById(sessionId)).thenReturn(Mono.empty());
        // Ожидаем, что будет создан новый аккаунт с DEFAULT_BALANCE - amountToPay
        when(accountRepository.save(any(AccountEntity.class))).thenAnswer(invocation -> {
            AccountEntity savedAccount = invocation.getArgument(0);
            return Mono.just(savedAccount);
        });

        Mono<PaymentResponse> result = paymentService.payOrder(sessionId, paymentRequest);

        StepVerifier.create(result)
                .expectNextMatches(response ->
                        response.status() == PaymentStatus.SUCCESS &&
                        response.remainingBalance().compareTo(defaultBalance.subtract(amountToPay)) == 0
                )
                .verifyComplete();

        verify(accountRepository, times(2)).save(any(AccountEntity.class));
    }

    @Test
    void getBalance_WhenAccountExists_ReturnsBalanceFromAccount() {
        BigDecimal amount = new BigDecimal("100.00");
        AccountEntity account = new AccountEntity(sessionId, amount);
        when(accountRepository.findById(sessionId)).thenReturn(Mono.just(account));

        Mono<Balance> result = paymentService.getBalance(sessionId);

        StepVerifier.create(result)
                .expectNext(new Balance(sessionId, amount))
                .verifyComplete();
    }

    @Test
    void payOrder_ConcurrentCalls_CreatesAccountOnlyOnce() {
        PaymentRequest paymentRequest = new PaymentRequest("order-1", new BigDecimal("1000"));
        BigDecimal defaultBalance = new BigDecimal(30_000);

        // Имитируем задержку при первом сохранении
        when(accountRepository.findById(sessionId)).thenReturn(Mono.empty());
        when(accountRepository.save(any(AccountEntity.class))).thenAnswer(invocation -> {
            AccountEntity savedAccount = invocation.getArgument(0);
            return Mono.just(savedAccount).delayElement(java.time.Duration.ofMillis(200));
        });

        // Запускаем два параллельных запроса
        Mono<PaymentResponse> call1 = paymentService.payOrder(sessionId, paymentRequest);
        Mono<PaymentResponse> call2 = paymentService.payOrder(sessionId, paymentRequest);

        StepVerifier.create(Mono.zip(call1, call2))
                .expectNextMatches(tuple ->
                        tuple.getT1().status() == PaymentStatus.SUCCESS &&
                        tuple.getT2().status() == PaymentStatus.SUCCESS
                )
                .verifyComplete();

        // Проверяем, что save (создание) — только один раз (плюс 2 раза для списания средств в каждом вызове)
        verify(accountRepository, times(3)).save(any(AccountEntity.class));
    }
}
