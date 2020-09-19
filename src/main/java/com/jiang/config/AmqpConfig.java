package com.jiang.config;

import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author shijiang.luo
 * @description
 * @date 2020-09-19 20:15
 */
@Configuration
public class AmqpConfig {

    @Bean
    public MessageConverter messageConverter(){

        return new Jackson2JsonMessageConverter();
    }

}