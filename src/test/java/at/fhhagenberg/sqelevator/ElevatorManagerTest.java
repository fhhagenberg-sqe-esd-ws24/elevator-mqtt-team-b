package at.fhhagenberg.sqelevator;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.util.Properties;
import java.rmi.RemoteException;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.eclipse.paho.mqttv5.client.MqttClient;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.eclipse.paho.mqttv5.client.persist.MemoryPersistence;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.mockito.Mockito;

@Testcontainers
public class ElevatorManagerTest 
{

    @Container
    private GenericContainer<?> mosquitto;
    private IElevator mockPLC;
    private Properties properties;
    private ElevatorManager elevatorManager;
    private MqttClient mqttClient;
    private String clientId = "TestElevator";

    @BeforeEach
    public void setUp() throws RemoteException, IOException, MqttException 
    {
    	// Create a mock PLC
        mockPLC = mock(IElevator.class);
    
        // Create a Mosquitto container
        mosquitto = new GenericContainer<>("eclipse-mosquitto:latest").withExposedPorts(1883); 

        // Set up properties with dynamic MQTT broker URL
        properties = new Properties();
        String brokerUrl = "tcp://" + mosquitto.getHost() + ":" + mosquitto.getMappedPort(1883);
        properties.setProperty("mqtt.url", brokerUrl);
        properties.setProperty("timer.period", "250");

        // Initialize ElevatorManager with mocked IElevator and properties
        elevatorManager = new ElevatorManager(mockPLC, properties);

        // Connect MQTT client for verification
        mqttClient = new MqttClient(brokerUrl, clientId);
        mqttClient.connect();
    }

    @AfterEach
    public void tearDown() {
        // Stop and remove the container
        if (mosquitto.isRunning()) {
            mosquitto.stop();
        }
        if (mosquitto != null) {
            mosquitto.close();
        }
    }



    @Test
    public void testPublishSystemTopics() throws Exception {
        // Trigger state update in ElevatorManager
        elevatorManager.startPolling();

        // Verify system topics are published
        MqttMessage numElevatorMessage = mqttClient.getTopic("system/numElevator").receive();
        assertNotNull(numElevatorMessage);
        assertEquals("2", new String(numElevatorMessage.getPayload()));

        MqttMessage numFloorsMessage = mqttClient.getTopic("system/numFloors").receive();
        assertNotNull(numFloorsMessage);
        assertEquals("5", new String(numFloorsMessage.getPayload()));


        
        // Use a CountDownLatch to wait for MQTT messages asynchronously
        CountDownLatch latch = new CountDownLatch(2);
        String[] receivedMessages = new String[2];

        mqttClient.subscribe("system/numElevator", (topic, message) -> 
        {
            receivedMessages[0] = new String(message.getPayload());
            latch.countDown();
        });

        mqttClient.subscribe("system/numFloors", (topic, message) -> 
        {
            receivedMessages[1] = new String(message.getPayload());
            latch.countDown();
        });

        // Trigger state update in ElevatorManager
        elevatorManager.startPolling();

        // Wait for the messages to be received
        boolean allMessagesReceived = latch.await(5, TimeUnit.SECONDS);
        assertTrue(allMessagesReceived, "Messages were not received in time");

        // Verify the content of the messages
        assertEquals("2", receivedMessages[0], "Incorrect number of elevators published");
        assertEquals("5", receivedMessages[1], "Incorrect number of floors published");
    }


    @Test
    public void testStartPolling() throws MqttException
    {
        // Start polling
        elevatorManager.startPolling();
        
    }

    // Add more tests to cover other scenarios
}