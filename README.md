# springboot-message
Message 异步机制

## 一、应用场景

1. 异步消息处理；
2. 应用解耦；
3. 流量削峰；

## 二、 概述

消息的模式：
  1. 点对点式
  
    - 消息发送者发送消息， 消息代理将其放入一个队列中，消息接收者从队列中获取消息内容，消息读取后被移出队列
    - 消息只有唯一的发送者和接受者， 但并不是说只能有一个接收者
   
  2. 发布订阅式
  
    - 发送者（发布者）发送消息到主题， 多个接收者（订阅者）监听（订阅）这个主题， 那么就会在消息到达时同时受到消息


## 三、 RabbitMQ 简介

### RabbitMQ 简介

RabbitMQ 是一个由 erlang 开发的 AMQP（Advanved Message Queue Protocol）的开源实现。

### 核心实现

***Message***

消息， 消息是不具名的，它由消息头和消息体组成。 消息体是不透明的， 而消息头是由一系列的可选属性组成，这些属性包括 routing-key（路由键）、
priority（相对于其他消息的优先权）、delivery-mode（指出该消息可能需要持久性存储）等。

***Publisher***

消息的生产者，也是一个向交换机发布消息的客户端应用程序。

***Exchange***

交换器， 用来接收生产者发送的消息，并将这些消息路由给服务器中的队列。
Exchange有4种类型： direct（默认），fanout，topic 和 headers， 不同类型的 Exchange 转发消息策略有所区别。
  
***Queue***

消息队列， 用来保存消息直到发送给消费者。它是消息的容器也是消息的终点。一个消息可投入一个或多个队列。消息一直在队列里面，等待消费者连接到这个
队列取走消息。

***Binding***

绑定，用于消息队列和交换器之间的关联。一个绑定就是基于路由键将交换器和消息队列连接起来的路由规则， 所以可以将交换器理解成一个由绑定构成的路由
表。Exchange 和 Queue 的绑定可以是多对多的关系。

***Connection***

网络连接， 比如一个TCP连接。

***Channel***

信道， 多路复用连接中的一条独立的双向数据流通道。 信道是建立在真实的TCP连接内的虚拟连接，AMQP 命令都是通过信道发出去的。不管是发布消息、订阅
队列还是接收消息，这些动作都是通过信道完成。 因为对于操作系统来说建立和销毁 TCP 都是非常昂贵的开销，所以引入了信道的概念， 以复用一条TCP连接。


### RabbitMQ Exchange创建

创建三个 Exchange 作为测试：

* exchange.direct
  - 点对点
  - 单播
  
* exchange.fanout
  - 全部都发送
  - 广播
  - 这里不需要使用 routeKey 

* exchange.topic
  - 能根据 routeKey 匹配上的队列将会被发送

## 三、分析下 SpringBoot 自动配置了哪些？

1. 查看类 RabbitAutoConfiguration;

2. 自动配置了连接工厂
```java
@Bean
public CachingConnectionFactory rabbitConnectionFactory(RabbitProperties properties,
        ObjectProvider<ConnectionNameStrategy> connectionNameStrategy) throws Exception {
    PropertyMapper map = PropertyMapper.get();
    CachingConnectionFactory factory = new CachingConnectionFactory(
            getRabbitConnectionFactoryBean(properties).getObject());
    map.from(properties::determineAddresses).to(factory::setAddresses);
    map.from(properties::isPublisherReturns).to(factory::setPublisherReturns);
    map.from(properties::getPublisherConfirmType).whenNonNull().to(factory::setPublisherConfirmType);
    RabbitProperties.Cache.Channel channel = properties.getCache().getChannel();
    map.from(channel::getSize).whenNonNull().to(factory::setChannelCacheSize);
    map.from(channel::getCheckoutTimeout).whenNonNull().as(Duration::toMillis)
            .to(factory::setChannelCheckoutTimeout);
    RabbitProperties.Cache.Connection connection = properties.getCache().getConnection();
    map.from(connection::getMode).whenNonNull().to(factory::setCacheMode);
    map.from(connection::getSize).whenNonNull().to(factory::setConnectionCacheSize);
    map.from(connectionNameStrategy::getIfUnique).whenNonNull().to(factory::setConnectionNameStrategy);
    return factory;
}
```

RabbitProperties 封装了 RabbitMQ 的配置。 

3. 提供了 RabbitTemplate, 发送和接收消息的接口

```java
@Bean
@ConditionalOnSingleCandidate(ConnectionFactory.class)
@ConditionalOnMissingBean(RabbitOperations.class)
public RabbitTemplate rabbitTemplate(RabbitTemplateConfigurer configurer, ConnectionFactory connectionFactory) {
    RabbitTemplate template = new RabbitTemplate();
    configurer.configure(template, connectionFactory);
    return template;
}
```

4. 提供了系统管理组件: AmqpAdmin
```java
@Bean
@ConditionalOnSingleCandidate(ConnectionFactory.class)
@ConditionalOnProperty(prefix = "spring.rabbitmq", name = "dynamic", matchIfMissing = true)
@ConditionalOnMissingBean
public AmqpAdmin amqpAdmin(ConnectionFactory connectionFactory) {
    return new RabbitAdmin(connectionFactory);
}
```

## 四、自定义实现消息解析器

定义一个消息解析类
```java
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
```

如果config中有定义的解析器， 将会自动将对象注入到 RabbitTemplate 中去, 源码如下:

```java
@Bean
@ConditionalOnMissingBean
public RabbitTemplateConfigurer rabbitTemplateConfigurer(RabbitProperties properties,
        ObjectProvider<MessageConverter> messageConverter,
        ObjectProvider<RabbitRetryTemplateCustomizer> retryTemplateCustomizers) {
    RabbitTemplateConfigurer configurer = new RabbitTemplateConfigurer();
    configurer.setMessageConverter(messageConverter.getIfUnique());
    configurer
            .setRetryTemplateCustomizers(retryTemplateCustomizers.orderedStream().collect(Collectors.toList()));
    configurer.setRabbitProperties(properties);
    return configurer;
}
```

## 五、@RabbitListener 和 @EnableRabbit

@EnableRabbit 启用监听事件

@RabbitListener 实现对消息队列的监听

### 具体实现

1. 在启动类上添加注解 @EnableRabbit

2. 在服务层方法上使用注解 @RabbitListener 实现监听

具体代码实现如下:

使用 @EnableRabbit 
```java
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
```

使用 @RabbitListener

```java
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

    /**
    * 
    * 不需要获取消息头方式
    * 
    * @param person
    */
    @RabbitListener(queues = {"atme.news"})
    public void receive(Person person){
        System.out.println("接收到消息: " + person);
    }

    /**
    * 
    * 消息头信息获取方式
    * 
    * @param message
    */
    @RabbitListener(queues = "test.news")
    public void receive(Message message) {
        System.out.println("header: " + message.getMessageProperties());
        System.out.println("body: " + message.getBody());
    }
}
```