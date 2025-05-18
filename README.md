# IntegrationTestDemo

---

**Tags:**
`spring-boot` `kafka` `java` `jpa` `h2` `integration-test` `embedded-kafka` `handler-chain` `e2e` `microservices`

---

A Spring Boot application demonstrating an end-to-end message flow using Kafka, a handler chain pattern, and persistence with JPA/H2. The project includes comprehensive integration tests with embedded Kafka.

---

**Quick Navigation:**
> Directly jump to `IntegrationTestE2EListenerToDB` by searching this name in your GitHub page search bar. This class contains the main end-to-end integration test for the message flow.

---

## Features
- **Spring Boot 3.4.5** (Java 21)
- **Kafka Integration**: Produces and consumes messages using Spring Kafka
- **Handler Chain Pattern**: Modular message processing (validation, persistence)
- **JPA/H2**: In-memory database for persistence
- **Comprehensive Integration Tests**: Embedded Kafka for real E2E testing

## Project Structure
```
src/main/java/com/example/chain/
  ChainApplication.java         # Main Spring Boot entry point
  handler/                     # Handler chain (Validation, DatabaseSave, config)
  kafka/                       # Kafka listener and config
  model/                       # Message entity
  repository/                  # JPA repository for Message
src/test/java/com/example/chain/
  IntegrationTestE2EListenerToDB.java   # Main E2E integration test
  ITIngestAndReadFromEmbededKafka.java  # Kafka ingest/read test
```

## How It Works
- **Producer** sends a `Message` to Kafka topic `messages`.
- **MessageListener** (Kafka consumer) receives the message and passes it to the handler chain.
- **ValidationHandler** checks message validity.
- **DatabaseSaveHandler** persists valid messages to the H2 database.
- **IntegrationTestE2EListenerToDB** verifies the full flow: produce → consume → process → persist.

## How MessageListener Consumes Messages in Tests

When you run the `testListenerToDbE2EFlow` method inside the `IntegrationTestE2EListenerToDB` class, the following happens:

- The test sends a `Message` object to the Kafka topic `messages` using a `KafkaTemplate`.
- Spring Boot's test context starts the application with an embedded Kafka broker and all beans, including `MessageListener`.
- The `MessageListener` class is annotated with `@Component` and has a method annotated with `@KafkaListener(topics = "messages", groupId = "message-group")`.
- As soon as a message is produced to the `messages` topic, the embedded Kafka broker delivers it to the consumer group.
- The `@KafkaListener` method in `MessageListener` automatically receives the message, and Spring deserializes it into a `Message` object.
- The received message is then passed to the handler chain for validation and persistence.

**In summary:**
> The test triggers the full application context, so the real Kafka consumer (`MessageListener`) is active and automatically consumes any messages sent to the topic during the test, just like it would in production.

## Chain of Responsibility Pattern in This Project

This project uses the **Chain of Responsibility** design pattern to process messages in a modular and extensible way:

- The `Handler` interface defines two methods: `setNext(Handler handler)` and `handle(Message message)`.
- `AbstractHandler` provides a base implementation, allowing handlers to be linked together.
- `ValidationHandler` checks if the message content is valid. If valid, it passes the message to the next handler.
- `DatabaseSaveHandler` saves valid messages to the database. It can also pass the message further if more handlers are added.
- The chain is configured in `HandlerConfig`, where `ValidationHandler` is set to forward to `DatabaseSaveHandler`.

**How it works in the flow:**
- When a message is received (e.g., by `MessageListener`), it is passed to the start of the chain (`ValidationHandler`).
- Each handler performs its logic and, if appropriate, calls `super.handle(message)` to pass the message to the next handler.
- This allows you to add, remove, or reorder processing steps easily by changing the chain configuration.

**Benefits:**
- Decouples processing steps
- Makes the flow modular and testable
- Easy to extend with new handlers for additional processing steps

## Running the Application
1. **JDK 21 required** (see your `README.md` or system setup)
2. Start the application:
   ```sh
   ./gradlew bootRun
   ```

## Running Tests
- To run all tests:
  ```sh
  ./gradlew test
  ```
- To run the main E2E test only:
  ```sh
  ./gradlew test --tests com.example.chain.IntegrationTestE2EListenerToDB
  ```

## Key Classes
- `ChainApplication`: Main entry point
- `handler/HandlerConfig`: Wires up the handler chain
- `handler/ValidationHandler`: Validates messages
- `handler/DatabaseSaveHandler`: Persists valid messages
- `kafka/MessageListener`: Consumes messages from Kafka and triggers the handler chain
- `model/Message`: Entity representing a message
- `repository/MessageRepository`: JPA repository for Message
- `IntegrationTestE2EListenerToDB`: Main E2E integration test

## Test Coverage
- **IntegrationTestE2EListenerToDB**: End-to-end test for the full Kafka-to-DB flow
- **ITIngestAndReadFromEmbededKafka**: Tests Kafka ingest and read with embedded Kafka

## Useful Links
- [Spring Boot Docs](https://spring.io/projects/spring-boot)
- [Spring Kafka Docs](https://docs.spring.io/spring-kafka/)
- [Spring Data JPA](https://spring.io/projects/spring-data-jpa)

---

For further details, see the code comments and integration tests.