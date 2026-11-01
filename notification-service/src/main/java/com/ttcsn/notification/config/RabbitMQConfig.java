/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.ttcsn.notification.config;

import com.ttcsn.common.event.Topics;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.fasterxml.jackson.databind.ObjectMapper;
/**
 *
 * @author quanghieu
 */
@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE            = "mb.topic.exchange";
    public static final String NOTIFICATION_QUEUE  = "q.payment-completed.notification";
    public static final String ROUTING_KEY         = Topics.PAYMENT_COMPLETED;

    @Bean
    public TopicExchange mbTopicExchange() {
        return new TopicExchange(EXCHANGE, true, false);
    }

    @Bean
    public Queue notificationPaymentQueue() {
        return new Queue(NOTIFICATION_QUEUE, true);
    }

    @Bean
    public Binding notificationPaymentBinding(Queue notificationPaymentQueue, TopicExchange mbTopicExchange) {
        return BindingBuilder.bind(notificationPaymentQueue).to(mbTopicExchange).with(ROUTING_KEY);
    }

    @Bean
    public MessageConverter messageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }
}