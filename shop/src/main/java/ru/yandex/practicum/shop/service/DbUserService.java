package ru.yandex.practicum.shop.service;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.shop.entity.UserEntity;
import ru.yandex.practicum.shop.repository.UserRepository;

import java.util.Arrays;
import java.util.stream.Collectors;

@Service
public class DbUserService implements UserService{
    private static final String DEFAULT_ROLE = "USER";

    private final UserRepository userRepository;

    public DbUserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public Mono<UserDetails> register(UserDetails userDetails) {
        String authorities = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .map(DbUserService::removeRolePrefix)
                .collect(Collectors.joining(","));
        String roles = authorities.isBlank() ? DEFAULT_ROLE : authorities;

        return userRepository.findByLogin(userDetails.getUsername())
                .flatMap(existingUser ->
                        Mono.error(new RuntimeException("Пользователь уже существует"))
                )
                .switchIfEmpty(Mono.defer(() ->
                        userRepository.save(
                                UserEntity.builder()
                                        .login(userDetails.getUsername())
                                        .password(userDetails.getPassword())
                                        .roles(roles)
                                        .build()
                        )
                ))
                .map(savedUser -> userDetails);
    }

    @Override
    public Mono<UserDetails> findByUsername(String username) {
        return userRepository.findByLogin(username)
                .map(user -> User.withUsername(user.getLogin())
                        .password(user.getPassword())
                        .roles(parseRoles(user.getRoles()))
                        .build());
    }

    private static String[] parseRoles(String roles) {
        if (roles == null || roles.isBlank()) {
            return new String[]{DEFAULT_ROLE};
        }
        String[] parsedRoles = Arrays.stream(roles.split(","))
                .map(String::trim)
                .filter(role -> !role.isBlank())
                .map(DbUserService::removeRolePrefix)
                .toArray(String[]::new);
        return parsedRoles.length > 0 ? parsedRoles : new String[]{DEFAULT_ROLE};
    }

    private static String removeRolePrefix(String role) {
        return role.startsWith("ROLE_") ? role.substring(5) : role;
    }
}
