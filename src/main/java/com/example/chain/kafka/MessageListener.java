package com.example.chain.kafka;

import com.example.chain.handler.Handler;
import com.example.chain.model.Message;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class MessageListener {
    private final Handler validationHandler;

    public MessageListener(Handler validationHandler) {
        this.validationHandler = validationHandler;
    }

    @KafkaListener(topics = "messages", groupId = "message-group")
    public void listen(Message message) {
        validationHandler.handle(message);
    }
} 