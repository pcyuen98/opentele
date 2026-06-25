package com.opentele.stacktrace;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import redis.embedded.RedisServer;

import java.io.IOException;

@TestConfiguration
public class EmbeddedRedisConfiguration {

    private RedisServer redisServer;

    @Bean
    public RedisServer redisServer() throws IOException {
        this.redisServer = new RedisServer(6379);
        this.redisServer.start();
        return this.redisServer;
    }

    public void stopRedis() {
        if (this.redisServer != null) {
            this.redisServer.stop();
        }
    }
}
