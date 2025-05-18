package com.example.chain;

import com.example.chain.model.Message;
import com.example.chain.repository.MessageRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.internals.RecordHeader;
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

import java.io.File;
import java.io.FileReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
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

    public static final String SRC_TEST_RESOURCES_MESSAGE_JSON = "src/test/resources/payloads/message.json";
    public static final String SRC_TEST_RESOURCES_HEADERS_JSON = "src/test/resources/headers/headers.json";

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
        // Setting below two properties is not required as we are setting these directly in buildProducerRecord -> ProducerRecord arguments
//        registry.add("embedded.kafka.topics", () -> kafkaProps.getProperty("embedded.kafka.topics"));
//        registry.add("embedded.kafka.partitions", () -> kafkaProps.getProperty("embedded.kafka.partitions"));
        registry.add("spring.kafka.producer.key-serializer", () -> kafkaProps.getProperty("key.serializer"));
        registry.add("spring.kafka.producer.value-serializer", () -> kafkaProps.getProperty("value.serializer"));
        registry.add("spring.kafka.consumer.group-id", () -> kafkaProps.getProperty("group.id"));
        registry.add("spring.kafka.consumer.auto-offset-reset", () -> kafkaProps.getProperty("auto.offset.reset"));
        registry.add("spring.kafka.consumer.key-deserializer", () -> kafkaProps.getProperty("key.deserializer"));
        registry.add("spring.kafka.consumer.value-deserializer", () -> kafkaProps.getProperty("value.deserializer"));
    }

    @Test
    void testListenerToDbE2EFlow() throws Exception {
        // Step 1: Prepare the Sample message to send. you can read json file to create the sample payload.
        String testMessageContent = "Hello, Kafka!";

        // Step 2: Using the above json data, prepare the message payload
        Message testMessage = preparePayloadBody(testMessageContent);
        logger.info("Sending message: {}", testMessageContent);

        // Step 3: Create headers for the message
        final List<Header> headers = getHeaders();

        // Step 4: Create a ProducerRecord with the Payload (Step 2) and Headers (Step 3)
        ProducerRecord<String, Message> messageProducerRecord = buildProducerRecord(testMessage, headers);

        // Step 5: Send message to Kafka
        CompletableFuture<SendResult<String, Message>> future = kafkaTemplate.send(messageProducerRecord);
        SendResult<String, Message> result = future.get(10, java.util.concurrent.TimeUnit.SECONDS);
        logger.info("Message sent successfully to partition: {}", result.getRecordMetadata().partition());

        // Step 6: Wait for the message to be consumed and processed. Allow some time for the listener to process the message.
        // Please note that you must adjust this as you might observe that sometime after trying two or three time this tests passes.
        Thread.sleep(6000);

        // Step 7: Fetch the message from the H2 database using the repository
        Message savedMessage = messageRepository.findByContent(testMessageContent);

        // Step 8: Verify or Assert the message was inserted into the H2 database
        assertNotNull(savedMessage, "Message should be saved in the database");
        assertEquals(testMessageContent, savedMessage.getContent(), "The saved message content should match the sent message");
    }

    @Test
    void testListenerToDbE2EFlowWithJsonFiles() throws Exception {
        // Step 1: Read the message payload from message.json
        Message testMessage = readMessageFromJson(SRC_TEST_RESOURCES_MESSAGE_JSON, Message.class);
        logger.info("Sending message from JSON: {}", testMessage.getContent());

        // Step 2: Read the headers from headers.json
        List<RecordHeader> headers = readHeadersFromJson(SRC_TEST_RESOURCES_HEADERS_JSON);

        // Step 3: Create a ProducerRecord with the payload and headers
        ProducerRecord<String, Message> messageProducerRecord = buildProducerRecord(testMessage, List.copyOf(headers));

        // Step 4: Send the message to Kafka
        CompletableFuture<SendResult<String, Message>> future = kafkaTemplate.send(messageProducerRecord);
        /* Here, the future.get() method is called on the CompletableFuture object (future) to block the current thread until the Kafka send operation completes or the specified timeout (10 seconds) elapses. The get() method takes two arguments: the timeout value (10) and the time unit (TimeUnit.SECONDS). If the operation completes successfully within the timeout, it returns a SendResult object. If the timeout is exceeded or an exception occurs during the send operation, the method will throw an exception.*/
        SendResult<String, Message> result = future.get(10, java.util.concurrent.TimeUnit.SECONDS);
        logger.info("Message sent successfully to partition: {}", result.getRecordMetadata().partition());

        // Step 5: Wait for the message to be consumed and processed
        Thread.sleep(6000);

        // Step 6: Fetch the message from the H2 database using the repository
        Message savedMessage = messageRepository.findByContent(testMessage.getContent());

        // Step 7: Verify the message was inserted into the H2 database
        assertNotNull(savedMessage, "Message should be saved in the database");
        assertEquals(testMessage.getContent(), savedMessage.getContent(), "The saved message content should match the sent message");
    }

    private <T> T readMessageFromJson(String filePath, Class<T> clazz) throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        try (FileReader reader = new FileReader(new File(filePath))) {
            return objectMapper.readValue(reader, clazz);
        }
    }

    private List<RecordHeader> readHeadersFromJson(String filePath) throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        try (FileReader reader = new FileReader(new File(filePath))) {
            List<?> headerList = objectMapper.readValue(reader, List.class);
            return headerList.stream()
                .map(obj -> {
                    var map = (java.util.Map<?,?>) obj;
                    String key = (String) map.get("key");
                    String value = (String) map.get("value");
                    return new RecordHeader(key, value.getBytes(StandardCharsets.UTF_8));
                })
                .toList();
        }
    }

    private static List<Header> getHeaders() {
        return List.of(
                new RecordHeader("header-key-1", "header-value-1".getBytes()),
                new RecordHeader("header-key-2", "header-value-2".getBytes())
        );
    }

    private ProducerRecord<String, Message> buildProducerRecord(Message message, List<Header> headers) {
        Properties kafkaProps = getKafkaConfigsProps();
        String topic = kafkaProps.getProperty("embedded.kafka.topics");
        Integer partition = Integer.valueOf(kafkaProps.getProperty("embedded.kafka.partitions"));

        return new ProducerRecord<>(topic, partition, null, null, message, headers);
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