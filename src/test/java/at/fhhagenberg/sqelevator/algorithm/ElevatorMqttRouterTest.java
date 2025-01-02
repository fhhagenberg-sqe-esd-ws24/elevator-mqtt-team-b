package at.fhhagenberg.sqelevator.algorithm;

import static org.mockito.Mockito.*;

import org.eclipse.paho.mqttv5.client.MqttClient;
import org.eclipse.paho.mqttv5.client.MqttCallback;
import org.eclipse.paho.mqttv5.client.MqttClient;
import org.eclipse.paho.mqttv5.client.MqttDisconnectResponse;
import org.eclipse.paho.mqttv5.client.persist.MemoryPersistence;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.eclipse.paho.mqttv5.common.packet.MqttProperties;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ElevatorMqttRouterTest {

    private MqttClient mockMqttClient;
    private IElevatorAlgorithm mockAlgorithm;
    private ElevatorMqttRouter router;

    @BeforeEach
    void setup() throws MqttException {
        // Mock dependencies
        mockMqttClient = mock(MqttClient.class);
        mockAlgorithm = mock(IElevatorAlgorithm.class);

        // Initialize the router with mocks
        router = new ElevatorMqttRouter(mockMqttClient, mockAlgorithm);
    }

    @Test
    void testConnect_ShouldSubscribeToSetupTopics() throws MqttException {
        // Act
        router.connect();

        // Assert
        verify(mockMqttClient).connect(any());
        verify(mockMqttClient).setCallback(any());
//        verify(mockMqttClient).subscribe("system/numElevator", 2);
//        verify(mockMqttClient).subscribe("system/numFloors", 2);
//        verify(mockMqttClient).subscribe("system/floorHeight", 2);
    }

    @Test
    void testHandleIncomingMessage_SetupPhase() throws MqttException {
        // Setup
        String topic = "system/numElevator";
        MqttMessage message = new MqttMessage("3".getBytes());

        // Act
        router.connect();
        router.handleIncomingMessage(topic, message);

        // Assert
        verify(mockMqttClient, never()).subscribe(anyString(), anyInt()); // Continuous topics not yet subscribed
    }

    @Test
    void testHandleIncomingMessage_SetupPhaseInvalid() throws MqttException {
        // Setup
        String topic = "system/numElevator/invalidTooLong";
        MqttMessage message = new MqttMessage("3".getBytes());
        router.handleIncomingMessage("system/numFloors", new MqttMessage("5".getBytes()));
        router.handleIncomingMessage("system/floorHeight", new MqttMessage("3".getBytes()));
        assertTrue(router.isSetupPhase());

        // Act
        router.connect();
        router.handleIncomingMessage(topic, message);

        // Assert
        // correct massage would chang it to false
        assertTrue(router.isSetupPhase());
    }

    @Test
    void testHandleIncomingMessage_ContinuousPhase() throws MqttException {

        doAnswer(invocation -> {
            // Extract the arguments passed to processRequests
            ElevatorState elevator = invocation.getArgument(0);
            // set direction so hasChanged method returns true
            if(elevator.direction == ElevatorState.eDirection.DOWN) {
                elevator.direction = ElevatorState.eDirection.UP;
            } else if (elevator.direction == ElevatorState.eDirection.UP) {
                elevator.direction = ElevatorState.eDirection.DOWN;
            }
            else {
                elevator.direction = ElevatorState.eDirection.UP;
            }

            return null; // Void method
        }).when(mockAlgorithm).processRequests(any(), any(), any(), any(), any(), any());

        doAnswer(invocation -> {
            return true;
        }).when(mockMqttClient).isConnected();

        // Setup the router as if setup is complete
        String topic = "system/elevator/0/currentFloor";
        MqttMessage message = new MqttMessage("2".getBytes());

        router.connect();
        router.handleIncomingMessage("system/numElevator", new MqttMessage("2".getBytes()));
        router.handleIncomingMessage("system/numFloors", new MqttMessage("5".getBytes()));
        router.handleIncomingMessage("system/floorHeight", new MqttMessage("3".getBytes()));

        //Act
        router.handleIncomingMessage("system/floor/buttonUp", new MqttMessage(topic.getBytes()));
        router.handleIncomingMessage("system/floor/buttonDown", new MqttMessage(topic.getBytes()));
        router.handleIncomingMessage("system/elevator/0/doorStatus", new MqttMessage("1".getBytes()));
        router.handleIncomingMessage("system/elevator/0/currentFloor", message);
        topic = "[[true, true, true, true, true]]"; // because numFloors = 5
        router.handleIncomingMessage("system/elevator/0/floorButtons",  new MqttMessage(topic.getBytes()));
        router.handleIncomingMessage("system/elevator/0/serviceFloor", new MqttMessage(topic.getBytes()));

        // Assert
        verify(mockMqttClient, times(6)).publish(anyString(), any(MqttMessage.class));
    }

    @Test
    void testPublishToMQTT_SendsMessageWhenConnected() throws MqttException {
        // Setup
        when(mockMqttClient.isConnected()).thenReturn(true);

        // Act
        router.publishToMQTT("test/topic", "Hello World");

        // Assert
        verify(mockMqttClient).publish(eq("test/topic"), any(MqttMessage.class));
    }

    @Test
    void testPublishToMQTT_DoesNotSendMessageWhenDisconnected() throws MqttException {
        // Setup
        when(mockMqttClient.isConnected()).thenReturn(false);

        // Act
        router.publishToMQTT("test/topic", "Hello World");

        // Assert
        verify(mockMqttClient, never()).publish(anyString(), any(MqttMessage.class));
    }

    @Test
    void testDisconnect_ShouldDisconnectIfConnected() throws MqttException {
        // Setup
        when(mockMqttClient.isConnected()).thenReturn(true);

        // Act
        router.disconnect();

        // Assert
        verify(mockMqttClient).disconnect();
    }

    @Test
    void testDisconnect_ShouldNotDisconnectIfNotConnected() throws MqttException {
        // Setup
        when(mockMqttClient.isConnected()).thenReturn(false);

        // Act
        router.disconnect();

        // Assert
        verify(mockMqttClient, never()).disconnect();
    }

    @Test
    void testDoRestart_ReturnsTrueOnError() throws MqttException {
        // Simulate an error causing a restart
        router.connect();
        router.handleIncomingMessage("system/numElevator", new MqttMessage("2".getBytes()));
        router.handleIncomingMessage("system/numFloors", new MqttMessage("5".getBytes()));
        router.handleIncomingMessage("system/floorHeight", new MqttMessage("3".getBytes()));

        router.handleIncomingMessage("system/rmi/interface", new MqttMessage("0".getBytes()));

        // Act
        boolean shouldRestart = router.doRestart();

        // Assert
        assertTrue(shouldRestart);
    }

    @Test
    void testDoRestart_InvalidRmiMessage() throws MqttException {
        // Simulate an error causing a restart
        router.connect();
        router.handleIncomingMessage("system/numElevator", new MqttMessage("2".getBytes()));
        router.handleIncomingMessage("system/numFloors", new MqttMessage("5".getBytes()));
        router.handleIncomingMessage("system/floorHeight", new MqttMessage("3".getBytes()));
        assertFalse(router.isSetupPhase());

        // Act
        router.handleIncomingMessage("system/rmi/invalid", new MqttMessage("0".getBytes()));

        // Assert
        assertTrue(router.isSetupPhase());
    }

    @Test
    void testConnect_ThrowsMqttException() throws MqttException {
        // Setup
        doThrow(new MqttException(MqttException.REASON_CODE_CLIENT_EXCEPTION)).when(mockMqttClient).connect(any());

        // Act and Assert
        assertThrows(MqttException.class, () -> router.connect());

    }

    @Test
    void testMessageArrived_InvalidTopic() throws Exception {
        router.handleIncomingMessage("system/numElevator", new MqttMessage("2".getBytes()));
        router.handleIncomingMessage("system/numFloors", new MqttMessage("5".getBytes()));
        router.handleIncomingMessage("system/floorHeight", new MqttMessage("3".getBytes()));

        // Act
        String invalidTopic = "invalid/topic";
        MqttMessage message = new MqttMessage("test".getBytes());

        // Act and Assert
        assertDoesNotThrow(() -> router.handleIncomingMessage(invalidTopic, message), "Invalid topic should not throw an exception");
    }

    @Test
    void testMessageArrived_InvalidPayload() throws Exception {
        // Setup
        router.handleIncomingMessage("system/numElevator", new MqttMessage("2".getBytes()));
        router.handleIncomingMessage("system/numFloors", new MqttMessage("5".getBytes()));
        router.handleIncomingMessage("system/floorHeight", new MqttMessage("3".getBytes()));
        String topic = "system/numElevator";
        MqttMessage invalidMessage = new MqttMessage("invalid".getBytes());

        // Act and Assert
        assertDoesNotThrow(() -> router.handleIncomingMessage(topic, invalidMessage), "Invalid payload should not throw an exception");
    }

    @Test
    void testPublishToMQTT_HandlesException() throws MqttException {
        // Setup
        router.handleIncomingMessage("system/numElevator", new MqttMessage("2".getBytes()));
        router.handleIncomingMessage("system/numFloors", new MqttMessage("5".getBytes()));
        router.handleIncomingMessage("system/floorHeight", new MqttMessage("3".getBytes()));
        when(mockMqttClient.isConnected()).thenReturn(true);
        doThrow(new MqttException(MqttException.REASON_CODE_CLIENT_EXCEPTION)).when(mockMqttClient).publish(anyString(), any(MqttMessage.class));

        // Act
        router.publishToMQTT("test/topic", "Hello World");

        // Assert
        // No exception should be propagated, and doRestart should be set to true
        assertTrue(router.doRestart(), "Router should set doRestart to true on publish exception");
    }

//    @Test
//    void testSubscribeToSetupTopics_HandlesException() throws MqttException {
//        // Setup
//        router.handleIncomingMessage("system/numElevator", new MqttMessage("2".getBytes()));
//        router.handleIncomingMessage("system/numFloors", new MqttMessage("5".getBytes()));
//        router.handleIncomingMessage("system/floorHeight", new MqttMessage("3".getBytes()));
//        doThrow(new MqttException(MqttException.REASON_CODE_CLIENT_EXCEPTION)).when(mockMqttClient).subscribe(anyString(), anyInt());
//
//        // Act
//        router.connect();
//        mqttCallback.connectComplete(false, "test/server");
//
//        // Assert
//        assertTrue(router.doRestart(), "Router should set doRestart to true on subscribe exception");
//    }
//
//    @Test
//    void testDisconnectedCallback_ShouldHandleGracefully() throws Exception {
//        // Setup
//        MqttDisconnectResponse mockDisconnectResponse = mock(MqttDisconnectResponse.class);
//
//        // Act
//        mqttCallback.disconnected(mockDisconnectResponse);
//
//        // Assert
//        assertTrue(router.doRestart(), "Router should set doRestart to true on disconnection");
//    }
//
//    @Test
//    void testMqttErrorOccurred_ShouldHandleGracefully() throws Exception {
//        // Setup
//        MqttException mockException = new MqttException(MqttException.REASON_CODE_CLIENT_EXCEPTION);
//
//        // Act
//        mqttCallback.mqttErrorOccurred(mockException);
//
//        // Assert
//        assertTrue(router.doRestart(), "Router should set doRestart to true on MQTT error");
//    }

    @Test
    void testHandleIncomingMessage_HandlesNumberFormatException() throws Exception {
        // Setup
        String topic = "system/numElevator";
        MqttMessage invalidMessage = new MqttMessage("invalid_number".getBytes());

        // Act and Assert
        assertDoesNotThrow(() -> router.handleIncomingMessage(topic, invalidMessage), "Invalid number format should not throw an exception");
    }

    @Test
    void testParseSetupMessage_InvalidTopic() throws Exception {
        // Setup
        MqttMessage message = new MqttMessage("test".getBytes());

        // Act and Assert
        assertDoesNotThrow(() -> router.handleIncomingMessage("system/invalid", message), "Invalid topic should not throw an exception");
        assertDoesNotThrow(() -> router.handleIncomingMessage("system", message), "Invalid topic should not throw an exception");
    }

    @Test
    void testParseContinuousMessage_InvalidPayload() throws Exception {
        // Setup
        String[] topic = {"system", "elevator", "0", "currentFloor"};
        String invalidPayload = "invalid";
        MqttMessage message = new MqttMessage("test".getBytes());
        // Act and Assert
        assertDoesNotThrow(() -> {
            router.handleIncomingMessage("system/floorHeight", message);
        });
        assertDoesNotThrow(() -> {
            router.handleIncomingMessage("system/numFloors", message);
        });
        assertDoesNotThrow(() -> {
            router.handleIncomingMessage("system/numElevator", message);
        });
        assertDoesNotThrow(() -> {
            router.handleIncomingMessage("system/invalid", message);
        });

    }

}
