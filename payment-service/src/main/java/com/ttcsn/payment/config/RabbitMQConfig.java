package com.ttcsn.payment.config;

import com.ttcsn.common.event.Topics;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * SOA Step 3 — RabbitMQ phía payment-service (producer).
 * Khai báo TopicExchange; queue được các consumer (booking/notification) tự khai báo & bind.
 */
@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE = "mb.topic.exchange";

    @Bean
    public TopicExchange mbTopicExchange() {
        return new TopicExchange(EXCHANGE, true, false);
    }

    @Bean
    public MessageConverter messageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter messageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter);
        return template;
    }
}
