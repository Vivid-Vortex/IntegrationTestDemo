package com.example.chain.handler;

import com.example.chain.model.Message;
import org.springframework.data.jpa.repository.JpaRepository;
// import org.springframework.stereotype.Component;

// @Component
public class DatabaseSaveHandler extends AbstractHandler {
    private final JpaRepository<Message, Long> messageRepository;

    public DatabaseSaveHandler(JpaRepository<Message, Long> messageRepository) {
        this.messageRepository = messageRepository;
    }

    @Override
    public void handle(Message message) {
        if ("VALID".equals(message.getStatus())) {
            messageRepository.save(message);
            message.setStatus("SAVED");
        }
        super.handle(message);
    }
} 