Write a integration test 
Step 1: Use Embeded Kafka to ingest some message to the Kafka.
Step 2: Then Read the message from Kafaka topic
Step 3: Assert the result
Step 4: Add MessageListener class to this test method of integration test class. MessageListener should read from embeded kafaka.
Step 5: Make sure the message is saved into the H2 database.
Step 6: Assert the result.


Rules:
You are free to run any gradle based command and accept any files.
Test every step before moving onto the next step. Test it until you fix it.
Please feel free to execute the tests using gradle based commands

Jdk 21: C:\Program Files\Java\jdk-21