package com.ttcsn.booking.config;

import com.ttcsn.common.event.Topics;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * SOA Step 3 — RabbitMQ phía booking-service.
 * Exchange (topic) + Queue riêng của booking-service, bind routing-key "payment.completed".
 */
@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE       = "mb.topic.exchange";
    public static final String BOOKING_QUEUE  = "q.payment-completed.booking";
    public static final String ROUTING_KEY    = Topics.PAYMENT_COMPLETED; // payment.completed

    @Bean
    public TopicExchange mbTopicExchange() {
        return new TopicExchange(EXCHANGE, true, false);
    }

    @Bean
    public Queue bookingPaymentQueue() {
        return new Queue(BOOKING_QUEUE, true);
    }

    @Bean
    public Binding bookingPaymentBinding(Queue bookingPaymentQueue, TopicExchange mbTopicExchange) {
        return BindingBuilder.bind(bookingPaymentQueue).to(mbTopicExchange).with(ROUTING_KEY);
    }

    /** Converter JSON — inject ObjectMapper của Boot (đã có JavaTimeModule cho OffsetDateTime). */
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
