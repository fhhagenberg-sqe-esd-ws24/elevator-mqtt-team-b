package at.fhhagenberg.sqelevator;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.util.Properties;
import java.rmi.RemoteException;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.eclipse.paho.mqttv5.client.IMqttToken;
import org.eclipse.paho.mqttv5.client.MqttCallback;
import org.eclipse.paho.mqttv5.client.MqttClient;
import org.eclipse.paho.mqttv5.client.MqttDisconnectResponse;
import org.eclipse.paho.mqttv5.client.persist.MemoryPersistence;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.eclipse.paho.mqttv5.common.packet.MqttProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
public class ElevatorManagerTest 
{
    @Container
    private static final GenericContainer<?> mosquitto = 
        new GenericContainer<>("eclipse-mosquitto:latest")
            .withExposedPorts(1883); // Default Mosquitto port for MQTT


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
    
        // Set up properties with dynamic MQTT broker URL
        String brokerUrl = "tcp://" + mosquitto.getHost() + ":" + mosquitto.getMappedPort(1883);
        properties = new Properties();
        properties.setProperty("mqtt.url", brokerUrl);
        properties.setProperty("timer.period", "250");

        // Initialize ElevatorManager with mocked IElevator and properties
        elevatorManager = new ElevatorManager(mockPLC, properties);

        // Connect MQTT client for verification
        mqttClient = new MqttClient(brokerUrl, clientId, new MemoryPersistence());
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

        // Start polling
        elevatorManager.startPolling();

        // Wait for the MQTT client to receive the messages
        CountDownLatch latch = new CountDownLatch(1);
        mqttClient.setCallback(new MqttCallback() {
            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                if (topic.equals("elevator/0/floorNum")) {
                    latch.countDown();
                }
            }

            @Override
            public void disconnected(MqttDisconnectResponse disconnectResponse) {
                // Not used for subscriptions
            }

            @Override
            public void mqttErrorOccurred(MqttException exception) {
                exception.printStackTrace();
            }

            @Override
            public void deliveryComplete(IMqttToken token) {
                // Not used for subscriptions
            }

            @Override
            public void connectComplete(boolean reconnect, String serverURI) {
                // Handle connection complete
            }

            @Override
            public void authPacketArrived(int reasonCode, MqttProperties properties) {
                // Handle auth packet arrived
            }
        });

        // Wait for the latch to count down
        assertTrue(latch.await(1, TimeUnit.SECONDS));

    }
}