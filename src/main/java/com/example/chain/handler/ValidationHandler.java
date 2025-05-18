package com.example.chain.handler;

import com.example.chain.model.Message;
// import org.springframework.stereotype.Component;

// @Component
public class ValidationHandler extends AbstractHandler {
    @Override
    public void handle(Message message) {
        if (message.getContent() == null || message.getContent().trim().isEmpty()) {
            message.setStatus("INVALID");
            return;
        }
        message.setStatus("VALID");
        super.handle(message);
    }
} 