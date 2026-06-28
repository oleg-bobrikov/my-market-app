package ru.yandex.practicum.shop.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.shop.service.UserService;


@Controller
public class LoginController {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final String DEFAULT_ROLE = "USER";

    @Autowired
    public LoginController(UserService userService, PasswordEncoder passwordEncoder) {
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("/login")
    public Mono<String> login(ServerWebExchange exchange) {
        return exchange.getSession()
                .doOnNext(session ->
                        session.getAttributes().keySet().removeIf(key ->
                                key.endsWith("_error") && !key.equals("login_error")
                                        || key.equals("registration_success")
                        )
                )
                .thenReturn("login");
    }

    @GetMapping("/register")
    public Mono<String> getRegister(ServerWebExchange exchange) {
        return exchange.getSession()
                .doOnNext(session ->
                        session.getAttributes().keySet().removeIf(key ->
                                key.endsWith("_error") && !key.equals("registration_error")
                        )
                )
                .thenReturn("registration");
    }

    @PostMapping("/register")
    public Mono<String> register(ServerWebExchange exchange) {


        return exchange.getFormData()
                .flatMap(formData -> {
                    String username = formData.getFirst("username");
                    String password = formData.getFirst("password");

                    if (username == null || password == null) {
                        return exchange.getSession()
                                .doOnNext(session -> session.getAttributes().put("registration_error", "Не заполнено имя пользователя или пароль."))
                                .thenReturn("redirect:/register");
                    }

                    UserDetails user = User.withUsername(username.toLowerCase())
                            .password(passwordEncoder.encode(password))
                            .roles(DEFAULT_ROLE)
                            .build();

                    return userService.findByUsername(user.getUsername())
                            .flatMap(existingUser -> exchange.getSession()
                                    .doOnNext(session ->
                                            session.getAttributes().put("registration_error", "Ошибка регистрации. Попробуйте другое имя."))
                                    .thenReturn("redirect:/register"))
                            .switchIfEmpty(
                                    userService.register(user)
                                            .then(exchange.getSession()
                                                    .doOnNext(session ->
                                                            session.getAttributes().put(
                                                                    "registration_success",
                                                                    "Регистрация успешно пройдена."
                                                            )
                                                    ))
                                            .thenReturn("redirect:/register")
                            );
                });
    }
}
