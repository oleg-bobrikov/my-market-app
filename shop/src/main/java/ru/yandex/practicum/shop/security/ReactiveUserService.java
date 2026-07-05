package ru.yandex.practicum.shop.security;

import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.core.userdetails.UserDetails;
import reactor.core.publisher.Mono;

public interface ReactiveUserService extends ReactiveUserDetailsService {
    Mono<UserDetails> register(UserDetails userDetails);
}
