package ru.yandex.practicum.shop.configuration;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import redis.embedded.RedisServer;

import java.io.IOException;

@TestConfiguration
public class EmbeddedRedisConfiguration {

    @Bean(destroyMethod = "stop")
    public RedisServer redisServer() throws IOException {
        var redisServer = new RedisServer(6379);
        try {
            redisServer.start();
        } catch (Exception e) {
            // Если порт уже занят или другая ошибка, значит сервер уже запущен в другом контексте
            // или внешним процессом. Для тестов на одном порту это допустимо, 
            // если они используют общую инстанцию.
        }
        return redisServer;
    }
}