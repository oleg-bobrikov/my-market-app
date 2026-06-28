package ru.yandex.practicum.shop.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authentication.RedirectServerAuthenticationSuccessHandler;
import org.springframework.security.web.server.authentication.logout.RedirectServerLogoutSuccessHandler;
import org.springframework.security.web.server.authentication.logout.SecurityContextServerLogoutHandler;
import org.springframework.security.web.server.authentication.logout.WebSessionServerLogoutHandler;
import org.springframework.security.web.server.authentication.logout.DelegatingServerLogoutHandler;
import org.springframework.security.web.server.csrf.WebSessionServerCsrfTokenRepository;

import java.net.URI;

import org.thymeleaf.extras.springsecurity6.dialect.SpringSecurityDialect;
import reactor.core.publisher.Mono;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {
    @Bean
    public SpringSecurityDialect springSecurityDialect() {
        return new SpringSecurityDialect();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }


    @Bean
    public WebSessionServerCsrfTokenRepository csrfTokenRepository() {
        return new WebSessionServerCsrfTokenRepository();
    }

    // Настраиваем поведение при выходе
    @Bean
    public RedirectServerLogoutSuccessHandler redirectServerLogoutSuccessHandler() {
        RedirectServerLogoutSuccessHandler logoutSuccessHandler = new RedirectServerLogoutSuccessHandler();
        // При выходе перенаправляем на страницу логина с параметром logout
        logoutSuccessHandler.setLogoutSuccessUrl(URI.create("/"));
        return logoutSuccessHandler;
    }

    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(
            ServerHttpSecurity http,
            RedirectServerLogoutSuccessHandler redirectServerLogoutSuccessHandler,
            WebSessionServerCsrfTokenRepository csrfTokenRepository) {
        http
                .csrf(csrf -> csrf.csrfTokenRepository(csrfTokenRepository))
                // Явно разрешаем доступ к /login и / для всех
                .authorizeExchange(exchanges -> exchanges
                        .pathMatchers(
                                "/items/**",
                                "/api/images/**",
                                "/login/**",
                                "/logout/**",
                                "/register/**",
                                "/favicon.ico",
                                "/",
                                "/cart/**",
                                "/buy/**",
                                "/orders/**"
                        ).permitAll()
                        .anyExchange().authenticated()
                )
                // Настраиваем форму логина
                .formLogin(form -> form
                        // URL страницы логина
                        .loginPage("/login")
                        .authenticationSuccessHandler((exchange, auth) -> exchange.getExchange().getSession()
                                .doOnNext(session -> {
                                    session.getAttributes().remove("login_error");
                                    session.getAttributes().remove("registration_error");
                                })
                                .then(new RedirectServerAuthenticationSuccessHandler("/")
                                        .onAuthenticationSuccess(exchange, auth)
                                ))
                        .authenticationFailureHandler((exchange, ex) ->
                                exchange.getExchange().getSession()
                                        .doOnNext(session ->
                                                session.getAttributes().put("login_error", "Неверное имя пользователя или пароль."))
                                        .then(Mono.fromRunnable(() -> {
                                            exchange.getExchange().getResponse().setStatusCode(HttpStatus.FOUND);
                                            exchange.getExchange().getResponse().getHeaders()
                                                    .setLocation(URI.create("/login"));
                                        })))
                )
                // Настраиваем обработку при выходе
                .logout(logout -> logout
                        // URL страницы выхода
                        .logoutUrl("/logout")
                        .logoutSuccessHandler(redirectServerLogoutSuccessHandler)
                        .logoutHandler(new DelegatingServerLogoutHandler(
                                new SecurityContextServerLogoutHandler(),
                                new WebSessionServerLogoutHandler()
                        ))
                );

        return http.build();
    }
}
