package com.jiang.service;

import com.jiang.entity.Person;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

/**
 * @author shijiang.luo
 * @description
 * @date 2020-09-19 21:10
 */
@Service
public class PersonService {

    @RabbitListener(queues = {"atme.news"})
    public void receive(Person person){
        System.out.println("接收到消息: " + person);
    }

    @RabbitListener(queues = "test.news")
    public void receive(Message message) {
        System.out.println("header: " + message.getMessageProperties());
        System.out.println("body: " + message.getBody());
    }
}