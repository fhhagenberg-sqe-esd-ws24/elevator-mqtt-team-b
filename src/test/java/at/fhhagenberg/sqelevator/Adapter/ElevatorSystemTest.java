/*! **MH-Moduleheader*****************************************************
 *  Project:    Uebung 01

 *  Department: FH Hagenberg
 ** **********************************************************************
 *
 *  \file      ElevatorSystemTest.java
 *  \details
 *  \author    Bauernfeind, Goldberger, Stellnberger
 *  \version   0.1
 *  \date      2024
 *  \remarks
 *
** *********************************************************************/

package at.fhhagenberg.sqelevator.Adapter;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.*;

public class ElevatorSystemTest {

    private IElevator mockPlc;
    private ElevatorSystem elevatorSystem;

    @BeforeEach
    public void setUp() throws Exception {
        // Create a mock PLC interface
        mockPlc = mock(IElevator.class);

        // Mock PLC returns 2 elevators and 5 floors
        when(mockPlc.getElevatorNum()).thenReturn(2);
        when(mockPlc.getFloorNum()).thenReturn(5);
        when(mockPlc.getFloorHeight()).thenReturn(14);

        // Create an ElevatorSystem with 2 elevators and 5 floors
        elevatorSystem = new ElevatorSystem(mockPlc);
    }

    @Test
    public void testElevatorSystemInitialization() {
        // Check that the system initializes with the correct number of elevators
        assertEquals(2, elevatorSystem.getElevators().size());
        assertEquals(2, elevatorSystem.getNumElevator());
        assertEquals(5, elevatorSystem.getNumFloors());
        assertEquals(14, elevatorSystem.getFloorHeight());

        // Check that each elevator has the correct elevator number
        assertEquals(0, elevatorSystem.getElevators().get(0).getElevatorNumber());
        assertEquals(1, elevatorSystem.getElevators().get(1).getElevatorNumber());
    }

    @Test
    public void testUpdateElevators() throws Exception {
        // Mock the PLC data for both elevators
        when(mockPlc.getCommittedDirection(0)).thenReturn(IElevator.ELEVATOR_DIRECTION_UP);
        when(mockPlc.getCommittedDirection(1)).thenReturn(IElevator.ELEVATOR_DIRECTION_DOWN);

        // Update the elevator system
        elevatorSystem.updateElevators();

        // Verify that each elevator has been updated with the correct values
        assertEquals(IElevator.ELEVATOR_DIRECTION_UP, elevatorSystem.getElevators().get(0).getCommittedDirection());
        assertEquals(IElevator.ELEVATOR_DIRECTION_DOWN, elevatorSystem.getElevators().get(1).getCommittedDirection());
    }

    @Test
    public void testElevatorStateTracking() throws Exception {
        Elevator elevator = elevatorSystem.getElevators().get(0);

        // Mock PLC data for the first elevator
        when(mockPlc.getCommittedDirection(0)).thenReturn(IElevator.ELEVATOR_DIRECTION_DOWN);

        // First update
        elevatorSystem.updateElevators();

        // Check initial state
        assertEquals(IElevator.ELEVATOR_DIRECTION_DOWN, elevator.getCommittedDirection());

        // Change in state
        when(mockPlc.getCommittedDirection(0)).thenReturn(IElevator.ELEVATOR_DIRECTION_UP);
        elevatorSystem.updateElevators();

        // Assert that the elevator state has changed
        assertTrue(elevator.hasCommittedDirectionChanged());
        assertEquals(IElevator.ELEVATOR_DIRECTION_UP, elevatorSystem.getElevators().get(0).getCommittedDirection());
    }
    
    @Test
    public void testSetCommittedDirection() throws Exception {
        // Mock PLC data
        when(mockPlc.getCommittedDirection(0)).thenReturn(IElevator.ELEVATOR_DIRECTION_DOWN);

        // Call setCommittedDirection
        elevatorSystem.setCommittedDirection(0, IElevator.ELEVATOR_DIRECTION_UP);

        // Verify the interaction with the PLC
        verify(mockPlc, times(1)).setCommittedDirection(0, IElevator.ELEVATOR_DIRECTION_UP);
    }

    @Test
    public void testSetServicesFloors() throws Exception {
        // Mock PLC data
        when(mockPlc.getServicesFloors(0, 3)).thenReturn(false);

        // Call setServicesFloors
        elevatorSystem.setServicesFloors(0, 3, true);

        // Verify the interaction with the PLC
        verify(mockPlc, times(1)).setServicesFloors(0, 3, true);
    }

    @Test
    public void testSetTarget() throws Exception {
        // Mock PLC data
        when(mockPlc.getTarget(0)).thenReturn(1);

        // Call setTarget
        elevatorSystem.setTarget(0, 4);

        // Verify the interaction with the PLC
        verify(mockPlc, times(1)).setTarget(0, 4);
    }
}
