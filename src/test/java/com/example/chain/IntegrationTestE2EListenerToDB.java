package com.example.chain;

import com.example.chain.model.Message;
import com.example.chain.repository.MessageRepository;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.TestPropertySource;
import org.springframework.kafka.support.SendResult;

import java.time.Duration;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
//@EmbeddedKafka(partitions = 1, topics = {"test-topic"}, brokerProperties = {"listeners=PLAINTEXT://localhost:9092", "port=9092"})
@EmbeddedKafka(
        partitions = 1,
        topics = {"messages"},
        brokerProperties = {"listeners=PLAINTEXT://localhost:9092", "port=9092"}
)
@TestPropertySource(properties = {
        "spring.kafka.producer.bootstrap-servers=${spring.embedded.kafka.brokers}",
        "spring.kafka.consumer.bootstrap-servers=${spring.embedded.kafka.brokers}",
        "spring.kafka.consumer.group-id=test-group",
        "spring.kafka.consumer.auto-offset-reset=earliest",
        "spring.kafka.producer.key-serializer=org.apache.kafka.common.serialization.StringSerializer",
        "spring.kafka.producer.value-serializer= org.springframework.kafka.support.serializer.JsonSerializer",
        "spring.kafka.consumer.key-deserializer=org.apache.kafka.common.serialization.StringDeserializer",
        "spring.kafka.consumer.value-deserializer=org.springframework.kafka.support.serializer.JsonSerializer"
})
class IntegrationTestE2EListenerToDB {
    private static final Logger logger = LoggerFactory.getLogger(IntegrationTestE2EListenerToDB.class);
    @Autowired
    private KafkaTemplate<String, Message> kafkaTemplate;

    @Autowired
    private MessageRepository messageRepository; // Inject the repository here

    private KafkaConsumer<String, String> consumer;

    @Test
    void testKafkaMessageFlow2() throws Exception {
        // Step 1: Send message to Kafka
        String testMessageContent = "Hello, Kafka!";
        Message testMessage = new Message();
        testMessage.setContent(testMessageContent);
        logger.info("Sending message: {}", testMessageContent);

        CompletableFuture<SendResult<String, Message>> future = kafkaTemplate.send("messages", testMessage);
        SendResult<String, Message> result = future.get(10, java.util.concurrent.TimeUnit.SECONDS);
        logger.info("Message sent successfully to partition: {}", result.getRecordMetadata().partition());

        // Step 2: Wait for the message to be consumed and processed
        Thread.sleep(5000); // Allow some time for the listener to process the message

        // Step 3: Verify the message was inserted into the H2 database
        Message savedMessage = messageRepository.findByContent(testMessageContent);
        assertNotNull(savedMessage, "Message should be saved in the database");
        assertEquals(testMessageContent, savedMessage.getContent(), "The saved message content should match the sent message");
    }

    private ConsumerRecords<String, String> consumeMessages(Properties consumerProps) {
        consumer = new KafkaConsumer<>(consumerProps);
        consumer.subscribe(List.of("test-topic"));
        return consumer.poll(Duration.ofSeconds(10));
    }

    private SendResult<String, Message> sendMessage(Message testMessage) throws InterruptedException, ExecutionException, TimeoutException {
        CompletableFuture<SendResult<String, Message>> future = kafkaTemplate.send("test-topic", testMessage);
        return future.get(10, java.util.concurrent.TimeUnit.SECONDS);
    }

    private static Properties getKafkaConfigsProps() {
        Properties consumerProps = new Properties();
        consumerProps.put("bootstrap.servers", "localhost:9092");
        consumerProps.put("group.id", "test-group");
        consumerProps.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        consumerProps.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        consumerProps.put("auto.offset.reset", "earliest");
        return consumerProps;
    }

    @AfterEach
    public void tearDown() {
        logger.info("Attempting to close KafkaConsumer...");
        if (consumer != null) {
            consumer.close();
            logger.info("KafkaConsumer closed successfully.");
        }
    }
} 