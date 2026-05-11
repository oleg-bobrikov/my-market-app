package ru.yandex.practicum.payment.mymarket.configuration;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import redis.embedded.RedisServer;

import java.io.IOException;

@TestConfiguration
public class EmbeddedRedisConfiguration {

    @Bean(destroyMethod = "stop")
    public RedisServer redisServer() throws IOException {
        var redisServer = new RedisServer();
        redisServer.start();
        return redisServer;
    }
}