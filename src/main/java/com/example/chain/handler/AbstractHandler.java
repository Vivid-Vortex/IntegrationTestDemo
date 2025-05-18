package com.example.chain.handler;

import com.example.chain.model.Message;

public abstract class AbstractHandler implements Handler {
    private Handler nextHandler;

    @Override
    public void setNext(Handler handler) {
        this.nextHandler = handler;
    }

    @Override
    public void handle(Message message) {
        if (nextHandler != null) {
            nextHandler.handle(message);
        }
    }
} 