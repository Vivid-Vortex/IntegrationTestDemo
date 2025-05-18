package com.example.chain.handler;

import com.example.chain.repository.MessageRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class HandlerConfig {
    @Bean
    public ValidationHandler validationHandler() {
        return new ValidationHandler();
    }

    @Bean
    public DatabaseSaveHandler databaseSaveHandler(MessageRepository messageRepository) {
        return new DatabaseSaveHandler(messageRepository);
    }

    @Bean
    public Handler handlerChain(ValidationHandler validationHandler, DatabaseSaveHandler databaseSaveHandler) {
        validationHandler.setNext(databaseSaveHandler);
        return validationHandler;
    }
} 