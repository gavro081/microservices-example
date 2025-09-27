package com.github.gavro081.common.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {
    public static final String EXCHANGE_NAME = "order_events_exchange";
    public static final String ORDERS_QUEUE = "orders_queue";
    public static final String PRODUCTS_QUEUE = "products_queue";

//    Exchange: The central "post office" where you send all order-related events.
//    Queue: A specific "mailbox" for a service.
//    Binding: A rule that connects an exchange to a queue.

    @Bean
    TopicExchange exchange(){
        return new TopicExchange(EXCHANGE_NAME);
    }

    @Bean
    Queue ordersQueue(){
        return new Queue(ORDERS_QUEUE, true);
    }

    @Bean
    Queue productsQueue(){
        return new Queue(PRODUCTS_QUEUE, true);
    }

    @Bean
    Binding binding(Queue productsQueue, TopicExchange exchange) {
        // send any message whose topic starts with order. to the products_queue
        return BindingBuilder.bind(productsQueue).to(exchange).with("order.#");
    }
}
