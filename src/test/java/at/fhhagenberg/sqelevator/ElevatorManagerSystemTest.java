/*! **MH-Moduleheader*****************************************************
 *  Project:    Uebung 01

 *  Department: FH Hagenberg
 ** **********************************************************************
 *
 *  \file      ElevatorTest.java
 *  \details
 *  \author    Bauernfeind, Goldberger, Stellnberger
 *  \version   0.1
 *  \date      2024
 *  \remarks
 *
** *********************************************************************/


package at.fhhagenberg.sqelevator;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import static org.mockito.Mockito.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;


// This test uses a real MQTT broker

public class ElevatorManagerSystemTest {

    private ElevatorManager elevatorManager;
    private IElevator mockPLC;
    //private MqttClient mqttClient;
    
    @BeforeEach
    public void setUp() throws Exception {
        // Create a mock PLC
        mockPLC = mock(IElevator.class);
              
        // Load properties from app.properties file
        Properties properties = new Properties();
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("app.properties")) {
            if (input == null) {
                throw new IOException("Unable to find app.properties");
            }
            properties.load(input);
        }
        
        // Initialize ElevatorManager with the mocked PLC
        elevatorManager = new ElevatorManager(mockPLC, properties);    
        elevatorManager.startPolling();
    }

    @AfterEach
    public void tearDown() {
        elevatorManager.stopPolling();
    }
    
   /* @Test
    public void testDummyMqttConnection() throws Exception {
    	
    }
    */
/*
    @Test
    public void testInitialPublish() throws Exception {
        // Set up the mock PLC to return expected values
        //when(mockPLC.getElevatorCount()).thenReturn(2);
        //when(mockPLC.getFloorCount()).thenReturn(5);
        when(mockPLC.getFloorHeight()).thenReturn(3);

        
        // Here we would normally check the MQTT broker, but for demonstration
        // we will assume you have a method to capture the published messages.
        String numElevatorMessage = mqttClient.getTopic("system/numElevator").getQos();
        String numFloorsMessage = mqttClient.getTopic("system/numFloors").getQos();
        String floorHeightMessage = mqttClient.getTopic("system/floorHeight").getQos();
        
        // Verify that the published messages match the expected values
        assertEquals("2", numElevatorMessage);
        assertEquals("5", numFloorsMessage);
        assertEquals("3", floorHeightMessage);
    }

    @Test
    public void testPublishChanges() throws Exception {
        // Set up the mock PLC to simulate elevator state changes
        Elevator mockElevator = mock(Elevator.class);
        when(mockElevator.getElevatorNumber()).thenReturn(1);
        when(mockElevator.hasCurrentFloorChanged()).thenReturn(true);
        when(mockElevator.getCurrentFloor()).thenReturn(3);
        
        elevatorManager.getElevatorSystem().addElevator(mockElevator);
        
        // Call publishChanges to send updated state to MQTT
        elevatorManager.publishChanges();
        
        // Similar to before, we would check the published message
        String currentFloorMessage = mqttClient.getTopic("system/elevator/1/currentFloor").getQos();
        
        // Verify that the published message matches the expected value
        assertEquals("3", currentFloorMessage);
    }
    */
}
