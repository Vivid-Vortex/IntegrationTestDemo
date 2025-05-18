# IntegrationTestDemo

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