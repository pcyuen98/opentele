package com.opentele.stacktrace.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.opentele.stacktrace.model.StackTraceData;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
public class RedisConfig {

    @Bean
    @ConditionalOnMissingBean(name = "stackTraceDataRedisTemplate")
    public RedisTemplate<String, StackTraceData> stackTraceDataRedisTemplate(RedisConnectionFactory connectionFactory) {
        log.debug("Initializing RedisTemplate for StackTraceData");
        RedisTemplate<String, StackTraceData> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        
        template.afterPropertiesSet();
        return template;
    }
}
