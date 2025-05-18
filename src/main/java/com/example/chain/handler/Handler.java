package com.example.chain.handler;

import com.example.chain.model.Message;

public interface Handler {
    void setNext(Handler handler);
    void handle(Message message);
} 