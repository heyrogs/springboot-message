package com.jiang;

import com.jiang.entity.Person;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;

@Slf4j
@SpringBootTest
class SpringbootMessageApplicationTests {

    private static final String EXCHANGE_DIRECT = "exchange.direct";
    private static final String EXCHANGE_FANOUT = "exchange.fanout";
    private static final String EXCHANGE_TOPIC = "exchange.topic";

    @Resource
    RabbitTemplate rabbitTemplate;
    @Resource
    AmqpAdmin amqpAdmin;

    @Test
    void contextLoads() {
    }

    /**
     *
     *  单播（点对点）
     *
     */
    @Test
    void directSendMsg(){
        // Message 需要自己构造一个；定义消息体内容和消息头
        // rabbitTemplate.send(exchage, routeKey, message);
        Person person = new Person(1, "test", 20);
        // object默认当成消息体，只需要传入要发送的对象， 自动序列化发送给 rabbitmq；
        // 序列化默认使用的是 JAVA的序列化机制
        rabbitTemplate.convertAndSend(EXCHANGE_DIRECT, "atme.news", person);
        log.info("send success...");
    }

    /**
     *
     * 接收数据
     *
     */
    @Test
    void directReceiveMsg(){
        Object obj = rabbitTemplate.receiveAndConvert("test.news");
        log.info("type: {}", obj.getClass());
        log.info("content: {}", obj);
    }

    /**
     *
     *  广播
     *
     */
    @Test
    void fanout(){
        rabbitTemplate.convertAndSend(EXCHANGE_FANOUT, "", new Person(2, "fanout", 25));
        log.info("fanout send success.");
    }

    @Test
    void createExchange(){
        amqpAdmin.declareExchange(new DirectExchange("amqpadmin.exchange"));
        log.info("exchange创建完成");
    }

    @Test
    void createQueue(){
        amqpAdmin.declareQueue(new Queue("amqpadmin.queue", true));
        log.info("queue创建完成");
    }

    @Test
    void binding(){
        amqpAdmin.declareBinding(
                new Binding("amqpadmin.queue", Binding.DestinationType.QUEUE,
                        "amqpadmin.exchange", "amqp.haha", null));
        log.info("binding成功");
    }


}
