
package at.fhhagenberg.sqelevator;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.rmi.RemoteException;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

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
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
public class ElevatorManagerIntegrationTest {

    private static final DockerImageName HIVE_MQ_IMAGE = DockerImageName.parse("hivemq/hivemq-ce:latest");

    private GenericContainer<?> hiveMQContainer;
    private IElevator mockPLC;
    private ElevatorManager elevatorManager;
    private Properties properties;
    private MqttClient mqttClient;
    private String brokerUrl;

    @BeforeEach
    public void setUp() throws Exception {
        // Start the HiveMQ container
        hiveMQContainer = new GenericContainer<>(HIVE_MQ_IMAGE).withExposedPorts(1883);
        hiveMQContainer.start();

        // Prepare broker URL
        brokerUrl = "tcp://" + hiveMQContainer.getHost() + ":" + hiveMQContainer.getMappedPort(1883);

        // Initialize mock PLC
        mockPLC = mock(IElevator.class);
        // Elevator System
        when(mockPLC.getElevatorNum()).thenReturn(1);
        when(mockPLC.getFloorNum()).thenReturn(2);
        when(mockPLC.getFloorHeight()).thenReturn(3);
        
        when(mockPLC.getFloorButtonUp(0)).thenReturn(true);
        when(mockPLC.getFloorButtonUp(1)).thenReturn(false);
        when(mockPLC.getFloorButtonDown(0)).thenReturn(false);
        when(mockPLC.getFloorButtonDown(1)).thenReturn(true);
        
        // ELevator
        when(mockPLC.getCommittedDirection(0)).thenReturn(1);
        when(mockPLC.getElevatorAccel(0)).thenReturn(2);
        when(mockPLC.getElevatorDoorStatus(0)).thenReturn(3);
        when(mockPLC.getElevatorFloor(0)).thenReturn(4);
        when(mockPLC.getElevatorPosition(0)).thenReturn(5);
        when(mockPLC.getElevatorSpeed(0)).thenReturn(6);
        when(mockPLC.getElevatorWeight(0)).thenReturn(7);
        when(mockPLC.getTarget(0)).thenReturn(8);
        when(mockPLC.getElevatorCapacity(0)).thenReturn(9);
        when(mockPLC.getElevatorButton(0,0)).thenReturn(false);
        when(mockPLC.getElevatorButton(0,1)).thenReturn(true);
        when(mockPLC.getServicesFloors(0,0)).thenReturn(true);
        when(mockPLC.getServicesFloors(0,1)).thenReturn(false);

        // Set up properties
        properties = new Properties();
        properties.setProperty("mqtt.url", brokerUrl);
        properties.setProperty("timer.period", "250");

        // Initialize ElevatorManager
        elevatorManager = new ElevatorManager(mockPLC, properties);

        // Prepare MQTT client
        mqttClient = new MqttClient(brokerUrl, "TestClient", new MemoryPersistence());
        mqttClient.connect();
    }

    @AfterEach
    public void tearDown() throws MqttException {
    	
    	if (mqttClient.isConnected()) {
            mqttClient.disconnect();
            mqttClient.close();
        }
        if (hiveMQContainer.isRunning()) {
            hiveMQContainer.stop();
            hiveMQContainer.close();
        }
    }

    @Test
    public void testPublishInitialSystemTopics() throws Exception {
    	String[] topics = {"system/numElevator", "system/numFloors", "system/floorHeight", "system/elevator/0/capacity"};
        String[] expectedMessages = {"1", "2", "3", "9"};      

        // Prepare a latch to wait for messages
        CountDownLatch latch = new CountDownLatch(topics.length);

        mqttClient.setCallback(new MqttCallback() {
            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                for (int i = 0; i < topics.length; i++) {
                    if (topic.equals(topics[i]) && new String(message.getPayload()).equals(expectedMessages[i])) {
                        latch.countDown();
                        
                    }
                }
            }

            @Override
            public void disconnected(MqttDisconnectResponse disconnectResponse) {}

            @Override
            public void mqttErrorOccurred(MqttException exception) {}

            @Override
            public void deliveryComplete(org.eclipse.paho.mqttv5.client.IMqttToken token) {}

            @Override
            public void connectComplete(boolean reconnect, String serverURI) {}

            @Override
            public void authPacketArrived(int reasonCode, MqttProperties properties) {}
        });

        // Subscribe to the expected topics
        for (String topic : topics) {
            mqttClient.subscribe(topic, 2);
        }

        // Start polling
        elevatorManager.startPolling();

        // Verify messages were received
        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }
    
    @Test
    public void testPublishUpdateSystemTopics() throws Exception {
        String[] topics = {
            "system/floor/buttonUp",
            "system/floor/buttonDown",
        };

        String[] expectedMessages = {"[true, false]", "[false, true]"};

        // Prepare MQTT callback
        CountDownLatch latch = new CountDownLatch(topics.length);
        mqttClient.setCallback(new MqttCallback() {
            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                for (int i = 0; i < topics.length; i++) {
                    if (topic.equals(topics[i]) && new String(message.getPayload()).equals(expectedMessages[i])) {
                        latch.countDown();
                    }
                }
            }

            @Override
            public void disconnected(MqttDisconnectResponse disconnectResponse) {}

            @Override
            public void mqttErrorOccurred(MqttException exception) {}

            @Override
            public void deliveryComplete(org.eclipse.paho.mqttv5.client.IMqttToken token) {}

            @Override
            public void connectComplete(boolean reconnect, String serverURI) {}

            @Override
            public void authPacketArrived(int reasonCode, MqttProperties properties) {}
        });

        // Subscribe to topics
        for (String topic : topics) {
            mqttClient.subscribe(topic, 2);
        }

        // Start polling
        elevatorManager.startPolling();

        // Verify messages
        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }
    
    @Test
    public void testPublishUpdateElevatorTopics() throws Exception {
        String[] topics = {
			"system/elevator/0/committedDirection",
			"system/elevator/0/acceleration",
			"system/elevator/0/doorStatus",
			"system/elevator/0/currentFloor",
			"system/elevator/0/position",
			"system/elevator/0/speed",
			"system/elevator/0/weight",
			"system/elevator/0/targetFloor",
			"system/elevator/0/floorButtons",
			"system/elevator/0/serviceFloors"
        };

        String[] expectedMessages = {"1", "2", "3", "4", "5", "6", "7", "8", "[false, true]", "[true, false]"};

        // Prepare MQTT callback
        CountDownLatch latch = new CountDownLatch(topics.length);
        mqttClient.setCallback(new MqttCallback() {
            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                for (int i = 0; i < topics.length; i++) {
                	System.out.println("Received message: " + new String(message.getPayload()) + " on topic: " + topic);
                    if (topic.equals(topics[i]) && new String(message.getPayload()).equals(expectedMessages[i])) {
                        latch.countDown();
                    }
                }
            }

            @Override
            public void disconnected(MqttDisconnectResponse disconnectResponse) {}

            @Override
            public void mqttErrorOccurred(MqttException exception) {}

            @Override
            public void deliveryComplete(org.eclipse.paho.mqttv5.client.IMqttToken token) {}

            @Override
            public void connectComplete(boolean reconnect, String serverURI) {}

            @Override
            public void authPacketArrived(int reasonCode, MqttProperties properties) {}
        });

        // Subscribe to topics
        for (String topic : topics) {
            mqttClient.subscribe(topic, 2);
        }

        // Start polling
        elevatorManager.startPolling();

        // Verify messages
        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }
    
    @Test
    public void testSubscribeCommitedDirection() throws Exception {
        // Connect MQTT
        elevatorManager.startPolling();

        // Publish test message
        String topic = "system/elevator/set/0/committedDirection";
        String payload = "5";        
        MqttMessage message = new MqttMessage(payload.getBytes());
        mqttClient.publish(topic, message);

        Thread.sleep(1000);

        verify(mockPLC, times(1)).setCommittedDirection(0, 5);
    }
    
    @Test
    public void testSubscribeServiceFloors() throws Exception {
        // Connect MQTT
        elevatorManager.startPolling();

        // Publish test message
        String topic = "system/elevator/set/0/serviceFloor/1";
        String payload = "true";        
        MqttMessage message = new MqttMessage(payload.getBytes());
        mqttClient.publish(topic, message);

        Thread.sleep(1000);

        verify(mockPLC, times(1)).setServicesFloors(0, 1, true);
    }
        
    @Test
    public void testSubscribeTargetFloors() throws Exception {
        // Connect MQTT
        elevatorManager.startPolling();

        // Publish test message
        String topic = "system/elevator/set/0/target";
        String payload = "5";        
        MqttMessage message = new MqttMessage(payload.getBytes());
        mqttClient.publish(topic, message);

        Thread.sleep(1000);

        verify(mockPLC, times(1)).setTarget(0, 5);
    }
    
    
    @Test
    public void testRMIExceptionHandling() throws Exception {
        // Simulate RMI exception
        doThrow(new RemoteException("Simulated RMI failure"))
            .when(mockPLC)
            .setCommittedDirection(anyInt(), anyInt());

        // Connect MQTT
        elevatorManager.startPolling();
        
        // Attempt to set committed direction
        try {
            // Publish test message
            String topic = "system/elevator/set/0/committedDirection";
            String payload = "5";        
            MqttMessage message = new MqttMessage(payload.getBytes());
            mqttClient.publish(topic, message);

        } catch (Exception e) {
            fail("Exception should have been handled internally, but was propagated.");
        }

        Thread.sleep(1000);

        // Verify exception was logged and no retries were made
        verify(mockPLC, times(1)).setCommittedDirection(0, 5);
    }
          
}
