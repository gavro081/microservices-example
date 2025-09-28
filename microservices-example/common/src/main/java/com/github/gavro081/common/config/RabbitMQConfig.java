package com.github.gavro081.common.config;

import com.github.gavro081.common.events.*;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.DefaultClassMapper;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class RabbitMQConfig {
    public static final String EXCHANGE_NAME = "order_events_exchange";
    public static final String ORDERS_QUEUE = "orders_queue";
    public static final String PRODUCTS_QUEUE = "products_queue";
    public static final String USERS_QUEUE = "users_queue";

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
    Queue usersQueue(){
        return new Queue(USERS_QUEUE, true);
    }

    @Bean
    Binding productsBinding(Queue productsQueue, TopicExchange exchange) {
//         product service needs to know when an order is created so it can process it
        return BindingBuilder.bind(productsQueue).to(exchange).with("order.created");
    }

    @Bean
    Binding usersBinding(Queue usersQueue, TopicExchange exchange) {
//         user service needs to know when inventory is reserved so it could deduct balance
        return BindingBuilder.bind(usersQueue).to(exchange).with("inventory.reserved");
    }

//    orders service needs to know about all inventory and balance outcomes to finalize the order
    @Bean
    Binding ordersInventoryBinding(Queue ordersQueue, TopicExchange exchange) {
        return BindingBuilder.bind(ordersQueue).to(exchange).with("inventory.#");
    }
    @Bean
    Binding ordersBalanceBinding(Queue ordersQueue, TopicExchange exchange) {
        return BindingBuilder.bind(ordersQueue).to(exchange).with("balance.#");
    }

    @Bean
    Binding productsCompensationBinding(Queue productsQueue, TopicExchange exchange) {
//     products-service needs to know about payment failure to compensate
        return BindingBuilder.bind(productsQueue).to(exchange).with("balance.failed");
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter();
        DefaultClassMapper classMapper = new DefaultClassMapper();

//        cleaner, but doesn't work :)
//        classMapper.setTrustedPackages("com.github.gavro081.common.events.*");
//        converter.setClassMapper(classMapper);

        Map<String, Class<?>> idClassMapping = new HashMap<>();
        idClassMapping.put("com.github.gavro081.common.events.OrderCreatedEvent", OrderCreatedEvent.class);
        idClassMapping.put("com.github.gavro081.common.events.InventoryReservedEvent", InventoryReservedEvent.class);
        idClassMapping.put("com.github.gavro081.common.events.InventoryReservationFailedEvent", InventoryReservationFailedEvent.class);
        idClassMapping.put("com.github.gavro081.common.events.BalanceDebitedEvent", BalanceDebitedEvent.class);
        idClassMapping.put("com.github.gavro081.common.events.BalanceDebitFailedEvent", BalanceDebitFailedEvent.class);

        classMapper.setIdClassMapping(idClassMapping);
        converter.setClassMapper(classMapper);
        return converter;
    }
}
