package com.example.chain;

import com.example.chain.model.Message;
import com.example.chain.repository.MessageRepository;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.util.Properties;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@EmbeddedKafka(
        brokerProperties = {"listeners=PLAINTEXT://localhost:9092", "port=9092"}
)
// Use Below kafkaProperties Method instead of @TestPropertySource annotation as used in ITIngestAndReadFromEmbededKafka
class IntegrationTestE2EListenerToDB {
    private static final Logger logger = LoggerFactory.getLogger(IntegrationTestE2EListenerToDB.class);
    @Autowired
    private KafkaTemplate<String, Message> kafkaTemplate;

    @Autowired
    private MessageRepository messageRepository; // Inject the repository here

    private KafkaConsumer<String, String> consumer;

    /**
     * DynamicPropertySource: Dynamically registers properties for the Spring context during test execution.
     * getKafkaConfigsProps: The method is invoked to fetch Kafka configuration properties.
     * DynamicPropertyRegistry: Maps the properties dynamically to Spring's property keys.
     */
    @DynamicPropertySource
    static void kafkaProperties(DynamicPropertyRegistry registry) {
        Properties kafkaProps = getKafkaConfigsProps();
        registry.add("spring.kafka.producer.bootstrap-servers", () -> kafkaProps.getProperty("bootstrap.servers"));
        registry.add("embedded.kafka.topics", () -> kafkaProps.getProperty("embedded.kafka.topics"));
        registry.add("embedded.kafka.partitions", () -> kafkaProps.getProperty("embedded.kafka.partitions"));
        registry.add("spring.kafka.producer.key-serializer", () -> kafkaProps.getProperty("key.serializer"));
        registry.add("spring.kafka.producer.value-serializer", () -> kafkaProps.getProperty("value.serializer"));
        registry.add("spring.kafka.consumer.group-id", () -> kafkaProps.getProperty("group.id"));
        registry.add("spring.kafka.consumer.auto-offset-reset", () -> kafkaProps.getProperty("auto.offset.reset"));
        registry.add("spring.kafka.consumer.key-deserializer", () -> kafkaProps.getProperty("key.deserializer"));
        registry.add("spring.kafka.consumer.value-deserializer", () -> kafkaProps.getProperty("value.deserializer"));
    }

    @Test
    void testListenerToDbE2EFlow() throws Exception {
        // Step 1: Prepare the Sample message to send. you can read json file to to the sample payload.
        String testMessageContent = "Hello, Kafka!";

        // Step 2: Use the json data, prepare the message payload
        Message testMessage = preparePayloadBody(testMessageContent);
        logger.info("Sending message: {}", testMessageContent);

        // Step 1: Send message to Kafka
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

    private Message preparePayloadBody(String content) {
        Message message = new Message();
        message.setContent(content);
        return message;
    }

    private static Properties getKafkaConfigsProps() {
        Properties consumerProps = new Properties();
        consumerProps.put("bootstrap.servers", "localhost:9092");
        consumerProps.put("group.id", "test-group");
        consumerProps.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        consumerProps.put("value.serializer", "org.springframework.kafka.support.serializer.JsonSerializer");
        consumerProps.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        consumerProps.put("value.deserializer", "org.springframework.kafka.support.serializer.JsonSerializer");
        consumerProps.put("auto.offset.reset", "earliest");
        consumerProps.put("embedded.kafka.topics", "messages");
        consumerProps.put("embedded.kafka.partitions", "1");
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