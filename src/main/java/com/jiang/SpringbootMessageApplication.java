package com.jiang;

import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@EnableRabbit
@SpringBootApplication
public class SpringbootMessageApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpringbootMessageApplication.class, args);
    }

}
