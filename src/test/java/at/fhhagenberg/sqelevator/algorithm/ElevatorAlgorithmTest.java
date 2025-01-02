/*! **MH-Moduleheader*****************************************************
 *  Project:    Elevator Control System
 *  Department: FH Hagenberg
 ** **********************************************************************
 *
 *  \file      ElevatorAlgorithmTest.java
 *  \details   Unit tests for the ElevatorAlgorithm class.
 *  \author    Bauernfeind, Goldberger, Stellnberger
 *  \version   0.1
 *  \date      2024
 *  \remarks
 *
 ** *********************************************************************/

package at.fhhagenberg.sqelevator.algorithm;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ElevatorAlgorithmTest {

    private ElevatorAlgorithm elevatorAlgorithm;
    private ElevatorState elevatorState[];
    private boolean[] floorButtons;
    private boolean[] serviceFloors;
    private boolean[] buttonUp;
    private boolean[] buttonDown;

    @BeforeEach
    public void setUp() {
        elevatorAlgorithm = new ElevatorAlgorithm();
        floorButtons = new boolean[5]; // 5 floors
        serviceFloors = new boolean[]{true, true, true, true, true};
        elevatorState = new ElevatorState[1];
        elevatorState[0] = new ElevatorState(serviceFloors.length);
        buttonUp = new boolean[5];
        buttonDown = new boolean[5];
    }

    @Test
    public void testInitialIdleState() {
        elevatorState[0].currentFloor = 0;
        elevatorState[0].direction = ElevatorState.eDirection.IDLE;

        floorButtons[2] = true; // Request from inside to floor 2
        elevatorAlgorithm.processRequests(elevatorState[0], elevatorState, floorButtons, serviceFloors, buttonUp, buttonDown);

        assertEquals(2, elevatorState[0].targetFloor);
        assertEquals(ElevatorState.eDirection.UP, elevatorState[0].direction);
    }

    @Test
    public void testMoveUpAndSwitchDirection() {
        elevatorState[0].currentFloor = 3;
        elevatorState[0].direction = ElevatorState.eDirection.UP;

        buttonUp[4] = true; // Request to go up to floor 4
        elevatorAlgorithm.processRequests(elevatorState[0], elevatorState, floorButtons, serviceFloors, buttonUp, buttonDown);

        assertEquals(4, elevatorState[0].targetFloor);

        // Simulate reaching the top and switching direction
        buttonDown[2] = true; // Request to go down to floor 2
        elevatorState[0].currentFloor = 4;
        elevatorState[0].doorStatus = ElevatorState.eDoorStatus.OPEN;
        elevatorAlgorithm.processRequests(elevatorState[0], elevatorState, floorButtons, serviceFloors, buttonUp, buttonDown);
        
        assertEquals(2, elevatorState[0].targetFloor);
        assertEquals(ElevatorState.eDirection.DOWN, elevatorState[0].direction);
    }

    @Test
    public void testMoveDownAndSwitchDirection() {
        elevatorState[0].currentFloor = 3;
        elevatorState[0].direction = ElevatorState.eDirection.DOWN;

        buttonDown[1] = true; // Request to go down to floor 1
        elevatorAlgorithm.processRequests(elevatorState[0], elevatorState, floorButtons, serviceFloors, buttonUp, buttonDown);

        assertEquals(1, elevatorState[0].targetFloor);

        // Simulate reaching the bottom and switching direction
        buttonUp[4] = true; // Request to go up to floor 4
        elevatorState[0].currentFloor = 1;
        elevatorState[0].doorStatus = ElevatorState.eDoorStatus.OPEN;
        elevatorAlgorithm.processRequests(elevatorState[0], elevatorState, floorButtons, serviceFloors, buttonUp, buttonDown);

        assertEquals(4, elevatorState[0].targetFloor);
        assertEquals(ElevatorState.eDirection.UP, elevatorState[0].direction);
    }

    @Test
    public void testNoRequestsRemainIdle() {
        elevatorState[0].currentFloor = 2;
        elevatorState[0].direction = ElevatorState.eDirection.IDLE;

        elevatorAlgorithm.processRequests(elevatorState[0], elevatorState, floorButtons, serviceFloors, buttonUp, buttonDown);

        assertEquals(2, elevatorState[0].currentFloor);
        assertEquals(ElevatorState.eDirection.IDLE, elevatorState[0].direction);
    }

    @Test
    public void testShouldServiceFloor() {
        floorButtons[2] = true; // Request from inside
        assertTrue(elevatorAlgorithm.shouldServiceFloor(2, floorButtons, buttonUp, buttonDown, serviceFloors));

        buttonUp[3] = true; // Request from outside
        assertTrue(elevatorAlgorithm.shouldServiceFloor(3, floorButtons, buttonUp, buttonDown, serviceFloors));

        serviceFloors[4] = false; // Floor not serviceable
        buttonDown[4] = true; // Request from outside
        assertFalse(elevatorAlgorithm.shouldServiceFloor(4, floorButtons, buttonUp, buttonDown, serviceFloors));
    }

    @Test
    public void testIgnoreUnserviceableFloors() {
        elevatorState[0].currentFloor = 0;
        elevatorState[0].direction = ElevatorState.eDirection.UP;

        serviceFloors[2] = false; // Floor 2 is not serviceable
        floorButtons[2] = true; // Request for floor 2

        elevatorAlgorithm.processRequests(elevatorState[0], elevatorState, floorButtons, serviceFloors, buttonUp, buttonDown);

        // No valid target as floor 2 is unserviceable
        assertEquals(ElevatorState.eDirection.IDLE, elevatorState[0].direction);
    }

    @Test
    public void testMultipleRequestsPrioritizeDirection() {
        elevatorState[0].currentFloor = 1;
        elevatorState[0].direction = ElevatorState.eDirection.UP;

        buttonUp[3] = true; // Request to go up to floor 3
        buttonDown[0] = true; // Request to go down to floor 0

        elevatorAlgorithm.processRequests(elevatorState[0], elevatorState, floorButtons, serviceFloors, buttonUp, buttonDown);

        // Prioritize UP
        assertEquals(3, elevatorState[0].targetFloor);
        assertEquals(ElevatorState.eDirection.UP, elevatorState[0].direction);
    }
}
