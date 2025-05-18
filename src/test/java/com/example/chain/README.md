### ConsumerRecords - key and value:

In the ConsumerRecords<String, String> object, the key and value represent the data structure of the messages consumed from a Kafka topic:


Key (String):


The key is used to determine the partition where the message will be stored in Kafka.
It can be null if no specific partitioning is required.
Example: If you are sending user data, the key could be a user ID to ensure all messages for the same user go to the same partition.
Value (String):


The value is the actual payload or content of the message.
It contains the data you want to send or consume from the Kafka topic.
Example: If you are sending user data, the value could be a JSON string representing the user's details.
Example:
If a Kafka topic contains the following messages:


Key: "user1", Value: "{"name":"Alice","age":30}"
Key: "user2", Value: "{"name":"Bob","age":25}"
The ConsumerRecords<String, String> object will allow you to iterate over these records and access the key-value pairs.
---
### About @DynamicPropertySource:

@DynamicPropertySource is an annotation that can be applied to static methods in integration test classes in order to add properties with dynamic values to the Environment's set of PropertySources.
Alternatively, dynamic properties can be added to the Environment by special beans in the test's ApplicationContext. See DynamicPropertyRegistrar for details.
This annotation and its supporting infrastructure were originally designed to allow properties from Testcontainers   based tests to be exposed easily to Spring integration tests. However, this feature may be used with any form of external resource whose lifecycle is managed outside the test's ApplicationContext.
@DynamicPropertySource methods use a DynamicPropertyRegistry to add name-value pairs to the Environment's set of PropertySources. Values are dynamic and provided via a java.util.function.Supplier which is only invoked when the property is resolved. Typically, method references are used to supply values, as in the example below.
Methods in integration test classes that are annotated with @DynamicPropertySource must be static and must accept a single DynamicPropertyRegistry argument.
Dynamic properties from methods annotated with @DynamicPropertySource will be inherited from enclosing test classes, analogous to inheritance from superclasses and interfaces. See @NestedTestConfiguration for details.
NOTE: if you use @DynamicPropertySource in a base class and discover that tests in subclasses fail because the dynamic properties change between subclasses, you may need to annotate your base class with @DirtiesContext to ensure that each subclass gets its own ApplicationContext with the correct dynamic properties.
Precedence
Dynamic properties have higher precedence than those loaded from @TestPropertySource, the operating system's environment, Java system properties, or property sources added by the application declaratively by using @PropertySource or programmatically. Thus, dynamic properties can be used to selectively override properties loaded via @TestPropertySource, system property sources, and application property sources.
Examples
The following example demonstrates how to use @DynamicPropertySource in an integration test class. Beans in the ApplicationContext can access the redis.host and redis.port properties which are dynamically retrieved from the Redis container.
@SpringJUnitConfig(...)
@Testcontainers
class ExampleIntegrationTests {

      @Container
      static GenericContainer redis =
          new GenericContainer("redis:5.0.3-alpine").withExposedPorts(6379);
 
      // ...
 
      @DynamicPropertySource
      static void redisProperties(DynamicPropertyRegistry registry) {
          registry.add("redis.host", redis::getHost);
          registry.add("redis.port", redis::getFirstMappedPort);
      }
}

---