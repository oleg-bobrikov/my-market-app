package ru.yandex.practicum.shop.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.shop.entity.UserEntity;
import ru.yandex.practicum.shop.repository.UserRepository;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class ReactiveUserServiceImpl implements ReactiveUserService {
    private final UserRepository userRepository;

    public ReactiveUserServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public Mono<UserDetails> register(UserDetails userDetails) {
        String authorities = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(","));

        return userRepository.findByLogin(userDetails.getUsername())
                .flatMap(existingUser ->
                        Mono.error(new RuntimeException("Пользователь уже существует"))
                )
                .then(userRepository.save(
                        UserEntity.builder()
                                .login(userDetails.getUsername())
                                .password(userDetails.getPassword())
                                .authorities(authorities)
                                .build()
                ))
                .map(savedUser -> CustomUserDetails.builder()
                        .userId(savedUser.getId())
                        .username(savedUser.getLogin())
                        .password(savedUser.getPassword())
                        .authorities(getAuthorities(savedUser.getAuthorities()))
                        .build());
    }

    @Override
    public Mono<UserDetails> findByUsername(String username) {
        return userRepository.findByLogin(username)
                .map(user -> CustomUserDetails.builder()
                        .userId(user.getId())
                        .username(user.getLogin())
                        .password(user.getPassword())
                        .authorities(getAuthorities(user.getAuthorities()))
                        .build());
    }

    private Set<GrantedAuthority> getAuthorities(String authorities) {
        return Arrays.stream(Optional.ofNullable(authorities).orElse("")
                        .split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toSet());
    }
}