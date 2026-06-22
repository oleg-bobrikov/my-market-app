package ru.yandex.practicum.shop.service;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.shop.entity.UserEntity;
import ru.yandex.practicum.shop.repository.UserRepository;

@Service
public class DbUserService implements UserService{
    private final UserRepository userRepository;

    public DbUserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public Mono<UserDetails> register(UserDetails userDetails) {
        return userRepository.findByLogin(userDetails.getUsername())
                .flatMap(existingUser ->
                        Mono.error(new RuntimeException("Пользователь уже существует"))
                )
                .switchIfEmpty(
                        userRepository.save(
                                UserEntity.builder()
                                        .login(userDetails.getUsername())
                                        .password(userDetails.getPassword())
                                        .build()
                        )
                )
                .map(savedUser -> userDetails);
    }

    @Override
    public Mono<UserDetails> findByUsername(String username) {
        return userRepository.findByLogin(username)
                .map(user -> User.withUsername(user.getLogin())
                        .password(user.getPassword())
                        .roles("USER")
                        .build());
    }
}
