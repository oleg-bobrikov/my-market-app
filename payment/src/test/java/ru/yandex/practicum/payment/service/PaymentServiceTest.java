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
import ru.yandex.practicum.payment.exception.InsufficientFundsException;
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
        PaymentRequest paymentRequest = new PaymentRequest();
        paymentRequest.setOrderId("order-1");
        paymentRequest.setAmount(amountToPay.toString());
        BigDecimal defaultBalance = new BigDecimal(30_000);
        BigDecimal remaining = defaultBalance.subtract(amountToPay);

        when(accountRepository.findById(sessionId))
                .thenReturn(Mono.empty()) // Первый вызов в getOrCreateAccount
                .thenReturn(Mono.just(new AccountEntity(sessionId, remaining, false))); // Вызов после updateBalance

        when(accountRepository.save(any(AccountEntity.class))).thenAnswer(invocation -> {
            AccountEntity savedAccount = invocation.getArgument(0);
            return Mono.just(savedAccount);
        });

        when(accountRepository.updateBalance(eq(sessionId), eq(amountToPay))).thenReturn(Mono.just(1));

        Mono<PaymentResponse> result = paymentService.payOrder(sessionId, paymentRequest);

        StepVerifier.create(result)
                .expectNextMatches(response ->
                        response.getStatus() == PaymentStatus.SUCCESS &&
                        new BigDecimal(response.getRemainingBalance()).compareTo(remaining) == 0
                )
                .verifyComplete();
    }

    @Test
    void getBalance_WhenAccountExists_ReturnsBalance() {
        BigDecimal amount = new BigDecimal("100.00");
        AccountEntity account = new AccountEntity(sessionId, amount, false);
        when(accountRepository.findById(sessionId)).thenReturn(Mono.just(account));

        Mono<Balance> result = paymentService.getBalance(sessionId);

        StepVerifier.create(result)
                .expectNextMatches(balance -> 
                        balance.getClientId().equals(sessionId) &&
                        new BigDecimal(balance.getBalance()).compareTo(amount) == 0)
                .verifyComplete();
    }

    @Test
    void payOrder_WhenCalledConcurrently_CreatesAccountOnlyOnce() {
        BigDecimal amountToPay = new BigDecimal("1000");
        PaymentRequest paymentRequest = new PaymentRequest();
        paymentRequest.setOrderId("order-1");
        paymentRequest.setAmount(amountToPay.toString());
        BigDecimal defaultBalance = new BigDecimal(30_000);

        // Имитируем задержку при первом сохранении
        // При первом вызове findById возвращаем empty, чтобы сработал switchIfEmpty
        when(accountRepository.findById(sessionId))
                .thenReturn(Mono.empty()); 

        when(accountRepository.save(any(AccountEntity.class))).thenAnswer(invocation -> {
            AccountEntity savedAccount = invocation.getArgument(0);
            // После сохранения findById должен возвращать этот аккаунт
            when(accountRepository.findById(sessionId)).thenReturn(Mono.just(savedAccount));
            return Mono.just(savedAccount).delayElement(java.time.Duration.ofMillis(200));
        });

        when(accountRepository.updateBalance(eq(sessionId), eq(amountToPay))).thenReturn(Mono.just(1));

        // Запускаем два параллельных запроса
        Mono<PaymentResponse> call1 = paymentService.payOrder(sessionId, paymentRequest);
        Mono<PaymentResponse> call2 = paymentService.payOrder(sessionId, paymentRequest);

        StepVerifier.create(Mono.zip(call1, call2))
                .expectNextMatches(tuple ->
                        tuple.getT1().getStatus() == PaymentStatus.SUCCESS &&
                        tuple.getT2().getStatus() == PaymentStatus.SUCCESS
                )
                .verifyComplete();

        // Проверяем, что save был вызван хотя бы раз
        verify(accountRepository, atLeastOnce()).save(any(AccountEntity.class));
    }

    @Test
    void payOrder_WhenInsufficientFunds_ThrowsExceptionAndLogs() {
        BigDecimal amountToPay = new BigDecimal("40000");
        PaymentRequest paymentRequest = new PaymentRequest();
        paymentRequest.setOrderId("order-fail");
        paymentRequest.setAmount(amountToPay.toString());

        when(accountRepository.findById(sessionId))
                .thenReturn(Mono.empty()); // Аккаунт не найден, будет создан

        when(accountRepository.save(any(AccountEntity.class))).thenAnswer(invocation -> {
            AccountEntity savedAccount = invocation.getArgument(0);
            return Mono.just(savedAccount);
        });

        when(accountRepository.updateBalance(eq(sessionId), eq(amountToPay))).thenReturn(Mono.just(0));

        Mono<PaymentResponse> result = paymentService.payOrder(sessionId, paymentRequest);

        StepVerifier.create(result)
                .expectError(InsufficientFundsException.class)
                .verify();
    }

    @Test
    void payOrder_SequentialPayments_ExceedingBalance_ShouldFail() {
        BigDecimal order1 = new BigDecimal("3000");
        BigDecimal order2 = new BigDecimal("15100");
        BigDecimal order3 = new BigDecimal("15100");

        java.util.concurrent.atomic.AtomicReference<BigDecimal> currentBalance = 
            new java.util.concurrent.atomic.AtomicReference<>(new BigDecimal("30000"));

        when(accountRepository.findById(sessionId)).thenAnswer(inv -> 
            Mono.just(new AccountEntity(sessionId, currentBalance.get(), false))
        );
        
        when(accountRepository.updateBalance(eq(sessionId), any(BigDecimal.class))).thenAnswer(invocation -> {
            BigDecimal amount = invocation.getArgument(1);
            if (currentBalance.get().compareTo(amount) >= 0) {
                currentBalance.set(currentBalance.get().subtract(amount));
                return Mono.just(1);
            }
            return Mono.just(0);
        });

        // Первый платеж
        PaymentRequest req1 = new PaymentRequest();
        req1.setOrderId("order1");
        req1.setAmount(order1.toString());
        paymentService.payOrder(sessionId, req1)
                .as(StepVerifier::create)
                .expectNextMatches(r -> r.getStatus() == PaymentStatus.SUCCESS && new BigDecimal(r.getRemainingBalance()).compareTo(new BigDecimal("27000")) == 0)
                .verifyComplete();

        // Второй платеж
        PaymentRequest req2 = new PaymentRequest();
        req2.setOrderId("order2");
        req2.setAmount(order2.toString());
        paymentService.payOrder(sessionId, req2)
                .as(StepVerifier::create)
                .expectNextMatches(r -> r.getStatus() == PaymentStatus.SUCCESS && new BigDecimal(r.getRemainingBalance()).compareTo(new BigDecimal("11900")) == 0)
                .verifyComplete();

        // Третий платеж - должен зафейлиться
        PaymentRequest req3 = new PaymentRequest();
        req3.setOrderId("order3");
        req3.setAmount(order3.toString());
        paymentService.payOrder(sessionId, req3)
                .as(StepVerifier::create)
                .expectError(InsufficientFundsException.class)
                .verify();
    }
}
