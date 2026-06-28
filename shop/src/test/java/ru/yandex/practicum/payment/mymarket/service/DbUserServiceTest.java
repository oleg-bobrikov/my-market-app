package ru.yandex.practicum.payment.mymarket.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import ru.yandex.practicum.shop.entity.UserEntity;
import ru.yandex.practicum.shop.repository.UserRepository;
import ru.yandex.practicum.shop.service.DbUserService;

import java.util.Set;
import java.util.Arrays;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DbUserServiceTest {

    @Mock
    private UserRepository userRepository;

    private DbUserService userService;

    @BeforeEach
    void setUp() {
        userService = new DbUserService(userRepository);
    }

    @Test
    void register_WhenValidUser_SavesUserWithRolesString() {
        UserDetails userDetails = User.withUsername("testadmin")
                .password("password")
                .roles("USER", "ADMIN")
                .build();

        UserEntity savedEntity = UserEntity.builder()
                .id(1L)
                .login("testadmin")
                .password("password")
                .roles("USER,ADMIN")
                .build();

        when(userRepository.findByLogin("testadmin")).thenReturn(Mono.empty());
        when(userRepository.save(any(UserEntity.class))).thenReturn(Mono.just(savedEntity));

        userService.register(userDetails)
                .as(StepVerifier::create)
                .expectNextMatches(user -> {
                    assertEquals("testadmin", user.getUsername());
                    return true;
                })
                .verifyComplete();

        verify(userRepository).save(argThat(entity -> {
            assertEquals("testadmin", entity.getLogin());
            assertEquals("password", entity.getPassword());
            assertEquals(
                    Set.of("USER", "ADMIN"),
                    Arrays.stream(entity.getRoles().split(",")).collect(Collectors.toSet())
            );
            return true;
        }));
    }

    @Test
    void findByUsername_WhenUserExists_ReturnsUserDetailsWithParsedRoles() {
        UserEntity entity = UserEntity.builder()
                .id(2L)
                .login("testuser")
                .password("password")
                .roles("USER, ADMIN")
                .build();

        when(userRepository.findByLogin("testuser")).thenReturn(Mono.just(entity));

        userService.findByUsername("testuser")
                .as(StepVerifier::create)
                .expectNextMatches(userDetails -> {
                    Set<String> authorities = userDetails.getAuthorities().stream()
                            .map(authority -> authority.getAuthority())
                            .collect(Collectors.toSet());

                    assertEquals("testuser", userDetails.getUsername());
                    assertEquals("password", userDetails.getPassword());
                    assertEquals(Set.of("ROLE_USER", "ROLE_ADMIN"), authorities);
                    return true;
                })
                .verifyComplete();

        verify(userRepository).findByLogin("testuser");
    }
}