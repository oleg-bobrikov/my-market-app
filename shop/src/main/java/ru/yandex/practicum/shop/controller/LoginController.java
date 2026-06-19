package ru.yandex.practicum.shop.controller;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.web.server.context.ServerSecurityContextRepository;
import org.springframework.security.web.server.context.WebSessionServerSecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.shop.service.InMemoryReactiveUserDetailService;


@Controller
public class LoginController {

    private final InMemoryReactiveUserDetailService userDetailsService;
    private final PasswordEncoder passwordEncoder;
    private final ServerSecurityContextRepository securityContextRepository = new WebSessionServerSecurityContextRepository();

    public LoginController(InMemoryReactiveUserDetailService userDetailsService, PasswordEncoder passwordEncoder) {
        this.userDetailsService = userDetailsService;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("/login")
    public Mono<String> login() {
        return Mono.just("login");
    }

    @PostMapping("/register")
    public Mono<String> register(ServerWebExchange exchange) {
        return exchange.getFormData()
                .flatMap(formData -> {
                    String username = formData.getFirst("username");
                    String password = formData.getFirst("password");

                    if (username == null || password == null) {
                        return exchange.getSession()
                                .doOnNext(session ->
                                        session.getAttributes().put("flash_error", "reg_failed"))
                                .thenReturn("redirect:/login");
                    }

                    UserDetails user = User.withUsername(username.toLowerCase())
                            .password(passwordEncoder.encode(password))
                            .roles("USER")
                            .build();

                    return userDetailsService.findByUsername(user.getUsername())
                            .flatMap(existingUser -> exchange.getSession()
                                    .doOnNext(session ->
                                            session.getAttributes().put("flash_error", "reg_failed"))
                                    .thenReturn("redirect:/login"))
                            .switchIfEmpty(
                                    userDetailsService.addUser(user)
                                            .flatMap(savedUser -> {
                                                UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                                                        savedUser,
                                                        null,
                                                        savedUser.getAuthorities()
                                                );
                                                SecurityContextImpl securityContext = new SecurityContextImpl(auth);
                                                return securityContextRepository.save(exchange, securityContext)
                                                        .contextWrite(ReactiveSecurityContextHolder.withSecurityContext(Mono.just(securityContext)))
                                                        .then(exchange.getSession())
                                                        .doOnNext(session ->
                                                                session.getAttributes()
                                                                        .put("flash_success", "registered"))
                                                        .thenReturn("redirect:/");
                                            })
                            );
                });
    }
}
