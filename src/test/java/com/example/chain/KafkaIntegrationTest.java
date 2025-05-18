package com.example.chain;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
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
@EmbeddedKafka(partitions = 1, topics = {"test-topic"}, brokerProperties = {"listeners=PLAINTEXT://localhost:9092", "port=9092"})
@TestPropertySource(properties = {
    "spring.kafka.producer.bootstrap-servers=${spring.embedded.kafka.brokers}",
    "spring.kafka.consumer.bootstrap-servers=${spring.embedded.kafka.brokers}",
    "spring.kafka.consumer.group-id=test-group",
    "spring.kafka.consumer.auto-offset-reset=earliest",
    "spring.kafka.producer.key-serializer=org.apache.kafka.common.serialization.StringSerializer",
    "spring.kafka.producer.value-serializer=org.apache.kafka.common.serialization.StringSerializer",
    "spring.kafka.consumer.key-deserializer=org.apache.kafka.common.serialization.StringDeserializer",
    "spring.kafka.consumer.value-deserializer=org.apache.kafka.common.serialization.StringDeserializer"
})
class KafkaIntegrationTest {
    private static final Logger logger = LoggerFactory.getLogger(KafkaIntegrationTest.class);

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    private KafkaConsumer<String, String> consumer;

    @Test
    void testKafkaMessageFlow() throws Exception {
        // Step 1: Send message to Kafka
        String testMessage = "Hello, Kafka!";
        logger.info("Sending message: {}", testMessage);

        final SendResult<String, String> result = sendMessage(testMessage);
        logger.info("Message sent successfully to partition: {}", result.getRecordMetadata().partition());

        // Step 2: Consume message from Kafka
        final Properties consumerProps = getKafkaConfigsProps();

        // Use REAEDME.MD -> ConsumerRecords - key and value to know more about
        final ConsumerRecords<String, String> records = consumerMessages(consumerProps);
        assertFalse(records.isEmpty(), "No messages were consumed");

        String receivedMessage = records.iterator().next().value();
        logger.info("Received message: {}", receivedMessage);

        // Step 3: Assert the result
        assertEquals(testMessage, receivedMessage, "The received message should match the sent message");
    }

    private ConsumerRecords<String, String> consumerMessages(Properties consumerProps) {
        consumer = new KafkaConsumer<>(consumerProps);
        consumer.subscribe(List.of("test-topic"));
        return consumer.poll(Duration.ofSeconds(10));
    }

    private SendResult<String, String> sendMessage(String testMessage) throws InterruptedException, ExecutionException, TimeoutException {
        CompletableFuture<SendResult<String, String>> future = kafkaTemplate.send("test-topic", testMessage);
        return future.get(10, java.util.concurrent.TimeUnit.SECONDS);
    }

    private static Properties getKafkaConfigsProps() {
        logger.info("Attempting to consume message...");
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
        if (consumer != null) {
            consumer.close();
            logger.info("KafkaConsumer closed successfully.");
        }
    }
} 