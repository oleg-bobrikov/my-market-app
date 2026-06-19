package ru.yandex.practicum.shop.service;

import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class InMemoryReactiveUserDetailService implements ReactiveUserDetailsService {

    private final Map<String, UserDetails> users = new ConcurrentHashMap<>();

    public InMemoryReactiveUserDetailService() {
    }

    public Mono<UserDetails> addUser(UserDetails user) {
        users.put(user.getUsername(), user);
        return Mono.just(users.get(user.getUsername()));
    }

    @Override
    public Mono<UserDetails> findByUsername(String username) {
        return Mono.justOrEmpty(users.get(username.toLowerCase()));
    }
}