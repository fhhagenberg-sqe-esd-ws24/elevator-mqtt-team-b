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
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.MountableFile;

@Testcontainers
public class ElevatorManagerTest
{
    @Container
    private static final GenericContainer<?> mosquitto =
            new GenericContainer<>("eclipse-mosquitto:latest")
                    .withExposedPorts(1883)
                    .withClasspathResourceMapping("mosquitto.conf", "/mosquitto/config/mosquitto.conf", BindMode.READ_ONLY); // Default Mosquitto port for MQTT


    private IElevator mockPLC;
    private Properties properties;
    private ElevatorManager elevatorManager;
    private MqttClient mqttClient;
    private String clientId = "TestElevator";

    @BeforeEach
    public void setUp() throws RemoteException, IOException, MqttException, InterruptedException {
        mockPLC = mock(IElevator.class);

        // Sicherstellen, dass der Broker läuft
        assertTrue(mosquitto.isRunning(), "Mosquitto container is not running");
        System.out.println("Mosquitto Host: " + mosquitto.getHost());
        System.out.println("Mosquitto Port: " + mosquitto.getMappedPort(1883));
        System.out.println("Mosquitto is running: " + mosquitto.isRunning());

        // URL des Brokers abrufen
        String brokerUrl = "tcp://" + mosquitto.getHost() + ":" + mosquitto.getMappedPort(1883);
        properties = new Properties();
        properties.setProperty("mqtt.url", brokerUrl);
        properties.setProperty("timer.period", "250");

        elevatorManager = new ElevatorManager(mockPLC, properties);

        // MQTT-Client verbinden
        System.out.println("Mosquitto brokerurl: " + brokerUrl);
        mqttClient = new MqttClient(brokerUrl, clientId, new MemoryPersistence());

        System.out.println("Connecting to MQTT Broker at: " + brokerUrl);
        try {
            mqttClient.connect();
            System.out.println("Connected to MQTT Broker successfully.");
        } catch (MqttException e) {
            e.printStackTrace();
            System.err.println("Failed to connect to MQTT Broker at " + brokerUrl);
            throw e;
        }
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



    }
}