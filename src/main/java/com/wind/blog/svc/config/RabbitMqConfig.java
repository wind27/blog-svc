package com.wind.blog.svc.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.amqp.RabbitProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMqConfig
 *
 * @author qianchun 2018/8/13
 **/
@Configuration
@EnableRabbit
@EnableConfigurationProperties(RabbitProperties.class)
public class RabbitMqConfig {

    private static Logger logger = LoggerFactory.getLogger(RabbitMqConfig.class);

    public static final String QUEUE_BLOGLINKPARSE = "queue.blogLinkParse";

    public static final String EXCHANGE_DIRECT_BLOGLINKPARSE = "exchange.direct.blogLinkParse";

    public static final String ROUTING_BLOGLINKPARSE = "routing.blogLinkParse";

    @Autowired
    private RabbitProperties rabbitProperties;

    @Bean
    public ConnectionFactory connectionFactory() {
        CachingConnectionFactory connectionFactory = new CachingConnectionFactory();
        connectionFactory.setHost(rabbitProperties.getHost());
        connectionFactory.setPort(rabbitProperties.getPort());
        connectionFactory.setUsername(rabbitProperties.getUsername());
        connectionFactory.setPassword(rabbitProperties.getPassword());
        connectionFactory.setVirtualHost(rabbitProperties.getVirtualHost());
//        connectionFactory.setPublisherConfirms(true);//设置事件回调
        return connectionFactory;
    }

    @Bean
    public RabbitTemplate rabbitTemplate() {
        RabbitTemplate template = new RabbitTemplate(connectionFactory());
        template.setQueue(QUEUE_BLOGLINKPARSE);
        return template;
    }

    @Bean
    public Queue userQueue() {
        return new Queue(QUEUE_BLOGLINKPARSE, true);
    }

    @Bean
    DirectExchange directExchange() {
        return new DirectExchange(EXCHANGE_DIRECT_BLOGLINKPARSE);
    }

    @Bean
    public Binding binding() {
        return BindingBuilder.bind(userQueue()).to(directExchange()).with(ROUTING_BLOGLINKPARSE);
    }
}
