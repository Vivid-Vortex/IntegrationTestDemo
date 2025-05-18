package com.example.chain.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import lombok.Data;

@Entity
@Data
public class Message {
    @Id
    @GeneratedValue
    private Long id;
    private String content;
    private String status;
} 